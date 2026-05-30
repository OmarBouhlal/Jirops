// planning-service/src/main/java/com/jiraclone/planning/kafka/TaskStatusChangedListener.java
package com.jiraclone.planning.kafka;

import com.jiraclone.planning.event.TaskStatusChangedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class TaskStatusChangedListener {

    private static final Logger log = Logger.getLogger(TaskStatusChangedListener.class.getName());

    @KafkaListener(topics = "${app.kafka.topics.task-status-changed:task.status_changed}")
    public void handleTaskStatusChanged(TaskStatusChangedEvent event) {
        log.info(String.format("Task status changed: taskId=%s, sprintId=%s, newStatus=%s, timestamp=%s",
                event.taskId(), event.sprintId(), event.newStatus(), event.timestamp()));
    }
}
