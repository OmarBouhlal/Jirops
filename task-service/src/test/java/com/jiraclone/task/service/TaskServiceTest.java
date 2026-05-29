package com.jiraclone.task.service;

import com.jiraclone.task.domain.TaskDocument;
import com.jiraclone.task.domain.TaskPriority;
import com.jiraclone.task.domain.TaskStatus;
import com.jiraclone.task.dto.AddCommentRequest;
import com.jiraclone.task.dto.CreateTaskRequest;
import com.jiraclone.task.event.TaskCreatedEvent;
import com.jiraclone.task.event.TaskStatusChangedEvent;
import com.jiraclone.task.kafka.TaskEventPublisher;
import com.jiraclone.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private MongoTemplate mongoTemplate;
    private TaskEventPublisher taskEventPublisher;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        mongoTemplate = mock(MongoTemplate.class);
        taskEventPublisher = mock(TaskEventPublisher.class);
        taskService = new TaskService(taskRepository, mongoTemplate, taskEventPublisher);
    }

    @Test
    void createTaskDefaultsStatusAndPublishesEvent() {
        CreateTaskRequest request = new CreateTaskRequest(
                "project-1",
                "sprint-1",
                "Build board",
                "Implement board view",
                null,
                "user-2",
                List.of("frontend")
        );
        when(taskRepository.save(any(TaskDocument.class))).thenAnswer(invocation -> {
            TaskDocument task = invocation.getArgument(0);
            task.setId("task-1");
            return task;
        });

        TaskDocument result = taskService.createTask(request, "user-1");

        assertEquals("task-1", result.getId());
        assertEquals(TaskStatus.TODO, result.getStatus());
        assertEquals(TaskPriority.MEDIUM, result.getPriority());
        assertEquals("user-1", result.getReporter());

        ArgumentCaptor<TaskCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCreatedEvent.class);
        verify(taskEventPublisher).publishTaskCreated(eventCaptor.capture());
        assertEquals("task-1", eventCaptor.getValue().taskId());
        assertEquals("project-1", eventCaptor.getValue().projectId());
    }

    @Test
    void searchTasksUsesMongoTemplateForDynamicFilters() {
        TaskDocument task = task("task-1", TaskStatus.IN_PROGRESS);
        when(mongoTemplate.find(any(Query.class), eq(TaskDocument.class))).thenReturn(List.of(task));

        List<TaskDocument> result = taskService.searchTasks(
                "project-1",
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                "user-2",
                "backend"
        );

        assertEquals(List.of(task), result);
        verify(mongoTemplate).find(any(Query.class), eq(TaskDocument.class));
    }

    @Test
    void updateStatusPersistsAndPublishesWhenStatusChanges() {
        TaskDocument task = task("task-1", TaskStatus.TODO);
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskDocument result = taskService.updateStatus("task-1", TaskStatus.DONE, "user-1");

        assertSame(task, result);
        assertEquals(TaskStatus.DONE, result.getStatus());

        ArgumentCaptor<TaskStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(TaskStatusChangedEvent.class);
        verify(taskEventPublisher).publishTaskStatusChanged(eventCaptor.capture());
        assertEquals(TaskStatus.TODO, eventCaptor.getValue().previousStatus());
        assertEquals(TaskStatus.DONE, eventCaptor.getValue().newStatus());
        assertEquals("user-1", eventCaptor.getValue().changedBy());
    }

    @Test
    void updateStatusSkipsPublishWhenStatusDoesNotChange() {
        TaskDocument task = task("task-1", TaskStatus.DONE);
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(task));

        TaskDocument result = taskService.updateStatus("task-1", TaskStatus.DONE, "user-1");

        assertSame(task, result);
        verify(taskRepository, never()).save(any(TaskDocument.class));
        verify(taskEventPublisher, never()).publishTaskStatusChanged(any(TaskStatusChangedEvent.class));
    }

    @Test
    void addCommentAppendsCommentWithAuthor() {
        TaskDocument task = task("task-1", TaskStatus.TODO);
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        var comment = taskService.addComment("task-1", new AddCommentRequest("Looks good"), "user-1");

        assertEquals("user-1", comment.authorId());
        assertEquals("Looks good", comment.body());
        assertEquals(1, task.getComments().size());
        verify(taskRepository).save(task);
    }

    @Test
    void deleteTasksByProjectIdDelegatesToRepository() {
        when(taskRepository.deleteByProjectId("project-1")).thenReturn(3L);

        long deleted = taskService.deleteTasksByProjectId("project-1");

        assertEquals(3L, deleted);
        verify(taskRepository).deleteByProjectId("project-1");
    }

    @Test
    void markSprintUnfinishedTasksDoneUpdatesOnlyUnfinishedTasks() {
        TaskDocument first = task("task-1", TaskStatus.TODO);
        TaskDocument second = task("task-2", TaskStatus.IN_PROGRESS);
        when(taskRepository.findBySprintIdAndStatusNot("sprint-1", TaskStatus.DONE))
                .thenReturn(List.of(first, second));

        int changed = taskService.markSprintUnfinishedTasksDone("sprint-1");

        assertEquals(2, changed);
        assertTrue(List.of(first, second).stream().allMatch(task -> task.getStatus() == TaskStatus.DONE));
        verify(taskRepository).saveAll(List.of(first, second));
    }

    @Test
    void deleteTaskFailsWhenTaskDoesNotExist() {
        when(taskRepository.existsById("missing")).thenReturn(false);

        try {
            taskService.deleteTask("missing");
        } catch (RuntimeException exception) {
            assertTrue(exception.getMessage().contains("missing"));
            verify(taskRepository, never()).deleteById("missing");
            return;
        }

        assertFalse(true, "Expected deleteTask to throw when task does not exist");
    }

    private TaskDocument task(String id, TaskStatus status) {
        TaskDocument task = new TaskDocument();
        task.setId(id);
        task.setProjectId("project-1");
        task.setSprintId("sprint-1");
        task.setTitle("Task " + id);
        task.setStatus(status);
        task.setPriority(TaskPriority.HIGH);
        task.setAssignee("user-2");
        task.setReporter("user-1");
        task.setLabels(List.of("backend"));
        return task;
    }
}
