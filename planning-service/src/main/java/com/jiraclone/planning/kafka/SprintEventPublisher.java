// planning-service/src/main/java/com/jiraclone/planning/kafka/SprintEventPublisher.java
package com.jiraclone.planning.kafka;

import com.jiraclone.planning.event.SprintCompletedEvent;
import com.jiraclone.planning.event.SprintStartedEvent;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SprintEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SprintEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String sprintStartedTopic;
    private final String sprintCompletedTopic;
    private final Tracer tracer;

    public SprintEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                Tracer tracer,
                                @Value("${app.kafka.topics.sprint-started:sprint.started}") String sprintStartedTopic,
                                @Value("${app.kafka.topics.sprint-completed:sprint.completed}") String sprintCompletedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.tracer = tracer;
        this.sprintStartedTopic = sprintStartedTopic;
        this.sprintCompletedTopic = sprintCompletedTopic;
    }

    public void publishSprintStarted(SprintStartedEvent event) {
        log.info("Published {} event for id={} traceId={}",
                event.getClass().getSimpleName(),
                event.sprintId(),
                traceId());
        kafkaTemplate.send(sprintStartedTopic, event.sprintId().toString(), event);
    }

    public void publishSprintCompleted(SprintCompletedEvent event) {
        log.info("Published {} event for id={} traceId={}",
                event.getClass().getSimpleName(),
                event.sprintId(),
                traceId());
        kafkaTemplate.send(sprintCompletedTopic, event.sprintId().toString(), event);
    }

    private String traceId() {
        return tracer.currentSpan() == null ? "n/a" : tracer.currentSpan().context().traceId();
    }
}
