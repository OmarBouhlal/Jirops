// project-service/src/test/java/com/jiraclone/project/controller/ProjectControllerIT.java
package com.jiraclone.project.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiraclone.commons.JwtTokenProvider;
import com.jiraclone.project.dto.AddMemberRequest;
import com.jiraclone.project.dto.CreateProjectRequest;
import com.jiraclone.project.dto.UpdateProjectRequest;
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
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@Transactional
@DisplayName("ProjectController integration")
class ProjectControllerIT {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("integration-test-secret-key-integration-test-secret-key".getBytes(StandardCharsets.UTF_8));

    @Container
    static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("project_it")
            .withUsername("project")
            .withPassword("project");

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
    @DisplayName("POST /projects returns 201")
    void createProject() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);

        mockMvc.perform(post("/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProjectRequest("Project One", "PRJ1", "desc"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("PRJ1"));
    }

    @Test
    @DisplayName("GET /projects returns 200 and list")
    void getProjects() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);
        UUID projectId = createProjectAndReturnId(token, userId, "Project One", "PRJ1");

        mockMvc.perform(get("/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(projectId.toString()));
    }

    @Test
    @DisplayName("GET /projects/{id} returns 200")
    void getProjectById() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);
        UUID projectId = createProjectAndReturnId(token, userId, "Project One", "PRJ1");

        mockMvc.perform(get("/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()));
    }

    @Test
    @DisplayName("GET /projects/{unknown-id} returns 404")
    void getUnknownProjectById() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);

        mockMvc.perform(get("/projects/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /projects/{id} returns 200 and updates name")
    void updateProject() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);
        UUID projectId = createProjectAndReturnId(token, userId, "Project One", "PRJ1");

        mockMvc.perform(put("/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateProjectRequest("Project Renamed", "desc"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Project Renamed"));
    }

    @Test
    @DisplayName("DELETE /projects/{id} returns 204")
    void deleteProject() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);
        UUID projectId = createProjectAndReturnId(token, userId, "Project One", "PRJ1");

        mockMvc.perform(delete("/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /projects/{id} again returns 404")
    void deleteProjectAgainReturns404() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String token = token(userId);
        UUID projectId = createProjectAndReturnId(token, userId, "Project One", "PRJ1");

        mockMvc.perform(delete("/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /projects/{id}/members returns 200")
    void addMember() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID memberId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String token = token(userId);
        UUID projectId = createProjectAndReturnId(token, userId, "Project One", "PRJ1");

        mockMvc.perform(post("/projects/{id}/members", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AddMemberRequest(memberId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[0]").value(memberId.toString()));
    }

    @Test
    @DisplayName("GET /projects without token returns 401")
    void getProjectsWithoutToken() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized());
    }

    private UUID createProjectAndReturnId(String token, UUID userId, String name, String key) throws Exception {
        String response = mockMvc.perform(post("/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProjectRequest(name, key, "desc"))))
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
