package com.jiraclone.task.service;

import com.jiraclone.task.domain.TaskAttachment;
import com.jiraclone.task.domain.TaskComment;
import com.jiraclone.task.domain.TaskDocument;
import com.jiraclone.task.domain.TaskPriority;
import com.jiraclone.task.domain.TaskStatus;
import com.jiraclone.task.dto.AddAttachmentRequest;
import com.jiraclone.task.dto.AddCommentRequest;
import com.jiraclone.task.dto.CreateTaskRequest;
import com.jiraclone.task.dto.UpdateTaskRequest;
import com.jiraclone.task.event.TaskCreatedEvent;
import com.jiraclone.task.event.TaskStatusChangedEvent;
import com.jiraclone.task.exception.TaskNotFoundException;
import com.jiraclone.task.kafka.TaskEventPublisher;
import com.jiraclone.task.repository.TaskRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final MongoTemplate mongoTemplate;
    private final TaskEventPublisher taskEventPublisher;

    public TaskService(TaskRepository taskRepository,
                       MongoTemplate mongoTemplate,
                       TaskEventPublisher taskEventPublisher) {
        this.taskRepository = taskRepository;
        this.mongoTemplate = mongoTemplate;
        this.taskEventPublisher = taskEventPublisher;
    }

    public TaskDocument createTask(CreateTaskRequest request, String reporterId) {
        TaskDocument task = new TaskDocument();
        task.setProjectId(request.projectId());
        task.setSprintId(request.sprintId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(TaskStatus.TODO);
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setAssignee(request.assignee());
        task.setReporter(reporterId);
        task.setLabels(request.labels());

        TaskDocument saved = taskRepository.save(task);
        taskEventPublisher.publishTaskCreated(new TaskCreatedEvent(
                saved.getId(),
                saved.getProjectId(),
                saved.getSprintId(),
                saved.getTitle(),
                saved.getStatus(),
                saved.getPriority(),
                saved.getAssignee(),
                saved.getReporter(),
                saved.getCreatedAt()
        ));
        return saved;
    }

    public List<TaskDocument> searchTasks(String projectId,
                                          TaskStatus status,
                                          TaskPriority priority,
                                          String assignee,
                                          String label) {
        List<Criteria> criteria = new ArrayList<>();
        addTextCriteria(criteria, "projectId", projectId);
        addExactCriteria(criteria, "status", status);
        addExactCriteria(criteria, "priority", priority);
        addTextCriteria(criteria, "assignee", assignee);
        addTextCriteria(criteria, "labels", label);

        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }
        return mongoTemplate.find(query, TaskDocument.class);
    }

    public TaskDocument getTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    public TaskDocument updateTask(String taskId, UpdateTaskRequest request) {
        TaskDocument task = getTask(taskId);
        if (request.projectId() != null) {
            task.setProjectId(request.projectId());
        }
        if (request.sprintId() != null) {
            task.setSprintId(request.sprintId());
        }
        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.assignee() != null) {
            task.setAssignee(request.assignee());
        }
        if (request.labels() != null) {
            task.setLabels(request.labels());
        }
        return taskRepository.save(task);
    }

    public TaskDocument updateStatus(String taskId, TaskStatus newStatus, String changedBy) {
        TaskDocument task = getTask(taskId);
        TaskStatus previousStatus = task.getStatus();
        if (previousStatus == newStatus) {
            return task;
        }

        task.setStatus(newStatus);
        TaskDocument saved = taskRepository.save(task);
        taskEventPublisher.publishTaskStatusChanged(new TaskStatusChangedEvent(
                saved.getId(),
                saved.getProjectId(),
                saved.getSprintId(),
                previousStatus,
                saved.getStatus(),
                changedBy,
                Instant.now()
        ));
        return saved;
    }

    public void deleteTask(String taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(taskId);
        }
        taskRepository.deleteById(taskId);
    }

    public TaskComment addComment(String taskId, AddCommentRequest request, String authorId) {
        TaskDocument task = getTask(taskId);
        TaskComment comment = new TaskComment(UUID.randomUUID().toString(), authorId, request.body(), Instant.now());
        task.getComments().add(comment);
        taskRepository.save(task);
        return comment;
    }

    public List<TaskComment> getComments(String taskId) {
        return List.copyOf(getTask(taskId).getComments());
    }

    public TaskAttachment addAttachment(String taskId, AddAttachmentRequest request, String uploadedBy) {
        TaskDocument task = getTask(taskId);
        TaskAttachment attachment = new TaskAttachment(
                UUID.randomUUID().toString(),
                request.fileName(),
                request.contentType(),
                request.size(),
                request.url(),
                uploadedBy,
                Instant.now()
        );
        task.getAttachments().add(attachment);
        taskRepository.save(task);
        return attachment;
    }

    public List<TaskAttachment> getAttachments(String taskId) {
        return List.copyOf(getTask(taskId).getAttachments());
    }

    public long deleteTasksByProjectId(String projectId) {
        return taskRepository.deleteByProjectId(projectId);
    }

    public int markSprintUnfinishedTasksDone(String sprintId) {
        List<TaskDocument> unfinishedTasks = taskRepository.findBySprintIdAndStatusNot(sprintId, TaskStatus.DONE);
        unfinishedTasks.forEach(task -> task.setStatus(TaskStatus.DONE));
        taskRepository.saveAll(unfinishedTasks);
        return unfinishedTasks.size();
    }

    private void addTextCriteria(List<Criteria> criteria, String field, String value) {
        if (value != null && !value.isBlank()) {
            criteria.add(Criteria.where(field).is(value));
        }
    }

    private void addExactCriteria(List<Criteria> criteria, String field, Object value) {
        if (value != null) {
            criteria.add(Criteria.where(field).is(value));
        }
    }
}
