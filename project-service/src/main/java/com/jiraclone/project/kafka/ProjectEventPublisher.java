// project-service/src/main/java/com/jiraclone/project/kafka/ProjectEventPublisher.java
package com.jiraclone.project.kafka;

import com.jiraclone.project.event.ProjectCreatedEvent;
import com.jiraclone.project.event.ProjectDeletedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProjectEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String projectCreatedTopic;
    private final String projectDeletedTopic;

    public ProjectEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                 @Value("${app.kafka.topics.project-created:project.created}") String projectCreatedTopic,
                                 @Value("${app.kafka.topics.project-deleted:project.deleted}") String projectDeletedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.projectCreatedTopic = projectCreatedTopic;
        this.projectDeletedTopic = projectDeletedTopic;
    }

    public void publishProjectCreated(ProjectCreatedEvent event) {
        kafkaTemplate.send(projectCreatedTopic, event.projectId().toString(), event);
    }

    public void publishProjectDeleted(ProjectDeletedEvent event) {
        kafkaTemplate.send(projectDeletedTopic, event.projectId().toString(), event);
    }
}
