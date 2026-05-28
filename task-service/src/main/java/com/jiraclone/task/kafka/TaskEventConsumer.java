package com.jiraclone.task.kafka;

import com.jiraclone.task.service.TaskService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TaskEventConsumer {

    private final TaskService taskService;

    public TaskEventConsumer(TaskService taskService) {
        this.taskService = taskService;
    }

    @KafkaListener(topics = "${app.kafka.topics.project-deleted:project.deleted}")
    public void handleProjectDeleted(Map<String, Object> event) {
        String projectId = stringValue(event, "projectId");
        if (projectId != null) {
            taskService.deleteTasksByProjectId(projectId);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.sprint-completed:sprint.completed}")
    public void handleSprintCompleted(Map<String, Object> event) {
        String sprintId = stringValue(event, "sprintId");
        if (sprintId != null) {
            taskService.markSprintUnfinishedTasksDone(sprintId);
        }
    }

    private String stringValue(Map<String, Object> event, String field) {
        Object value = event.get(field);
        if (value == null) {
            return null;
        }
        String stringValue = value.toString();
        return stringValue.isBlank() ? null : stringValue;
    }
}
