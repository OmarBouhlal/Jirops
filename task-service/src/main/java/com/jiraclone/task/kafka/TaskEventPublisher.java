package com.jiraclone.task.kafka;

import com.jiraclone.task.event.TaskCreatedEvent;
import com.jiraclone.task.event.TaskStatusChangedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TaskEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String taskCreatedTopic;
    private final String taskStatusChangedTopic;

    public TaskEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                              @Value("${app.kafka.topics.task-created:task.created}") String taskCreatedTopic,
                              @Value("${app.kafka.topics.task-status-changed:task.status_changed}") String taskStatusChangedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.taskCreatedTopic = taskCreatedTopic;
        this.taskStatusChangedTopic = taskStatusChangedTopic;
    }

    public void publishTaskCreated(TaskCreatedEvent event) {
        kafkaTemplate.send(taskCreatedTopic, event.taskId(), event);
    }

    public void publishTaskStatusChanged(TaskStatusChangedEvent event) {
        kafkaTemplate.send(taskStatusChangedTopic, event.taskId(), event);
    }
}
