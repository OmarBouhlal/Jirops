// project-service/src/main/java/com/jiraclone/project/kafka/ProjectEventPublisher.java
package com.jiraclone.project.kafka;

import com.jiraclone.project.event.ProjectCreatedEvent;
import com.jiraclone.project.event.ProjectDeletedEvent;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ProjectEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProjectEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String projectCreatedTopic;
    private final String projectDeletedTopic;
    private final Tracer tracer;

    public ProjectEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                 Tracer tracer,
                                 @Value("${app.kafka.topics.project-created:project.created}") String projectCreatedTopic,
                                 @Value("${app.kafka.topics.project-deleted:project.deleted}") String projectDeletedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.tracer = tracer;
        this.projectCreatedTopic = projectCreatedTopic;
        this.projectDeletedTopic = projectDeletedTopic;
    }

    public void publishProjectCreated(ProjectCreatedEvent event) {
        log.info("Published {} event for id={} traceId={}",
                event.getClass().getSimpleName(),
                event.projectId(),
                traceId());
        kafkaTemplate.send(projectCreatedTopic, event.projectId().toString(), event);
    }

    public void publishProjectDeleted(ProjectDeletedEvent event) {
        log.info("Published {} event for id={} traceId={}",
                event.getClass().getSimpleName(),
                event.projectId(),
                traceId());
        kafkaTemplate.send(projectDeletedTopic, event.projectId().toString(), event);
    }

    private String traceId() {
        return tracer.currentSpan() == null ? "n/a" : tracer.currentSpan().context().traceId();
    }
}
