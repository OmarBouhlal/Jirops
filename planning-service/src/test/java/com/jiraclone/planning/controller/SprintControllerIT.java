// planning-service/src/test/java/com/jiraclone/planning/controller/SprintControllerIT.java
package com.jiraclone.planning.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiraclone.commons.JwtTokenProvider;
import com.jiraclone.planning.domain.SprintStatus;
import com.jiraclone.planning.dto.AddTaskToSprintRequest;
import com.jiraclone.planning.dto.CreateSprintRequest;
import com.jiraclone.planning.dto.SprintResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@Transactional
@DisplayName("SprintController integration")
class SprintControllerIT {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("integration-test-secret-key-integration-test-secret-key".getBytes(StandardCharsets.UTF_8));

    @Container
    static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("planning_it")
            .withUsername("planning")
            .withPassword("planning");

    @Container
    static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("jwt.secret", () -> SECRET);
        registry.add("jwt.access-token-expiry-ms", () -> "600000");
        registry.add("jwt.refresh-token-expiry-ms", () -> "1200000");
    }

    @Test
    @DisplayName("POST /sprints returns 201")
    void createSprint() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);

        mockMvc.perform(post("/sprints")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateSprintRequest(
                                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                                "Sprint One",
                                "Goal",
                                LocalDate.of(2026, 6, 1),
                                LocalDate.of(2026, 6, 15)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(SprintStatus.PLANNING.name()));
    }

    @Test
    @DisplayName("GET /sprints?projectId= returns 200")
    void getSprints() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);
        UUID projectId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID sprintId = createSprintAndReturnId(token, userId, projectId, "Sprint One");

        mockMvc.perform(get("/sprints")
                        .param("projectId", projectId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sprintId.toString()));
    }

    @Test
    @DisplayName("POST /sprints/{id}/start returns 200 and ACTIVE status")
    void startSprint() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);
        UUID projectId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID sprintId = createSprintAndReturnId(token, userId, projectId, "Sprint One");

        mockMvc.perform(post("/sprints/{id}/start", sprintId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(SprintStatus.ACTIVE.name()));
    }

    @Test
    @DisplayName("POST /sprints/{id}/start again on second sprint returns 409")
    void startSecondSprintReturns409() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);
        UUID projectId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID firstSprintId = createSprintAndReturnId(token, userId, projectId, "Sprint One");
        UUID secondSprintId = createSprintAndReturnId(token, userId, projectId, "Sprint Two");

        mockMvc.perform(post("/sprints/{id}/start", firstSprintId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/sprints/{id}/start", secondSprintId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /sprints/{id}/complete returns 200 and CLOSED status")
    void completeSprint() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);
        UUID projectId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID sprintId = createSprintAndReturnId(token, userId, projectId, "Sprint One");

        mockMvc.perform(post("/sprints/{id}/start", sprintId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/sprints/{id}/complete", sprintId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(SprintStatus.CLOSED.name()));
    }

    @Test
    @DisplayName("POST /sprints/{id}/tasks returns 200 and taskId in response")
    void addTaskToSprint() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);
        UUID projectId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID sprintId = createSprintAndReturnId(token, userId, projectId, "Sprint One");
        UUID taskId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        mockMvc.perform(post("/sprints/{id}/tasks", sprintId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AddTaskToSprintRequest(taskId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskIds[0]").value(taskId.toString()));
    }

    private UUID createSprintAndReturnId(String token, UUID userId, UUID projectId, String name) throws Exception {
        String response = mockMvc.perform(post("/sprints")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateSprintRequest(projectId, name, "Goal", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15)))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return UUID.fromString(node.get("id").asText());
    }

    private String token(UUID userId) {
        return jwtTokenProvider.generateAccessToken(userId.toString(), List.of("ROLE_USER"));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
