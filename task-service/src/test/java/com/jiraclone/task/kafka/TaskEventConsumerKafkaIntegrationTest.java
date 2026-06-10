package com.jiraclone.task.kafka;

import com.jiraclone.task.service.TaskService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(classes = {
        TaskEventConsumerKafkaIntegrationTest.KafkaTestConfig.class,
        TaskEventConsumer.class
})

@EmbeddedKafka(
        partitions = 1,
        topics = {"project.deleted", "sprint.completed"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"}
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "app.kafka.topics.project-deleted=project.deleted",
        "app.kafka.topics.sprint-completed=sprint.completed"
})
class TaskEventConsumerKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        reset(taskService);
    }

    @Test
    void consumesProjectDeletedEventsAndDeletesProjectTasks() throws Exception {
        kafkaTemplate.send("project.deleted", Map.of("projectId", "project-1")).get();

        verify(taskService, timeout(5_000)).deleteTasksByProjectId("project-1");
    }

    @Test
    void consumesSprintCompletedEventsAndMarksUnfinishedTasksDone() throws Exception {
        kafkaTemplate.send("sprint.completed", Map.of("sprintId", "sprint-1")).get();

        verify(taskService, timeout(5_000)).markSprintUnfinishedTasksDone("sprint-1");
    }

    @Test
    void ignoresConsumerEventsWithoutRequiredIds() throws Exception {
        kafkaTemplate.send("project.deleted", Map.of("projectId", " ")).get();
        kafkaTemplate.send("sprint.completed", Map.of("otherField", "sprint-1")).get();

        verify(taskService, after(750).never()).deleteTasksByProjectId(anyString());
        verify(taskService, after(750).never()).markSprintUnfinishedTasksDone(anyString());
    }

    @Configuration
    @EnableKafka
    static class KafkaTestConfig {

        @Bean
        TaskService taskService() {
            return mock(TaskService.class);
        }

        @Bean
        ConsumerFactory<String, Map<String, Object>> consumerFactory(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, "task-consumer-integration-test");
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
            properties.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
            properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
            properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");
            return new DefaultKafkaConsumerFactory<>(
                    properties,
                    new StringDeserializer(),
                    new JsonDeserializer<>(Map.class, false)
            );
        }

        @Bean
        ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> kafkaListenerContainerFactory(
                ConsumerFactory<String, Map<String, Object>> consumerFactory) {
            ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory);
            return factory;
        }

        @Bean
        ProducerFactory<String, Object> producerFactory(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            return new DefaultKafkaProducerFactory<>(properties);
        }

        @Bean
        KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }
    }
}
