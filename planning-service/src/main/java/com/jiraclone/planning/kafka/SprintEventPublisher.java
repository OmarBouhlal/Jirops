// planning-service/src/main/java/com/jiraclone/planning/kafka/SprintEventPublisher.java
package com.jiraclone.planning.kafka;

import com.jiraclone.planning.event.SprintCompletedEvent;
import com.jiraclone.planning.event.SprintStartedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SprintEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String sprintStartedTopic;
    private final String sprintCompletedTopic;

    public SprintEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                @Value("${app.kafka.topics.sprint-started:sprint.started}") String sprintStartedTopic,
                                @Value("${app.kafka.topics.sprint-completed:sprint.completed}") String sprintCompletedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.sprintStartedTopic = sprintStartedTopic;
        this.sprintCompletedTopic = sprintCompletedTopic;
    }

    public void publishSprintStarted(SprintStartedEvent event) {
        kafkaTemplate.send(sprintStartedTopic, event.sprintId().toString(), event);
    }

    public void publishSprintCompleted(SprintCompletedEvent event) {
        kafkaTemplate.send(sprintCompletedTopic, event.sprintId().toString(), event);
    }
}
