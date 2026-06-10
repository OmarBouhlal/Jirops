// auth-service/src/test/java/com/jiraclone/auth/controller/AuthControllerIT.java
package com.jiraclone.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiraclone.auth.dto.LoginRequest;
import com.jiraclone.auth.dto.RefreshRequest;
import com.jiraclone.auth.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@Transactional
@DisplayName("AuthController integration")
class AuthControllerIT {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("integration-test-secret-key-integration-test-secret-key".getBytes(StandardCharsets.UTF_8));

    @Container
    static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_it")
            .withUsername("auth")
            .withPassword("auth");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("jwt.secret", () -> SECRET);
        registry.add("jwt.access-token-expiry-ms", () -> "600000");
        registry.add("jwt.refresh-token-expiry-ms", () -> "1200000");
    }

    @Test
    @DisplayName("POST /auth/register returns 201")
    void registerSuccess() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("user@example.com", "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/register duplicate email returns 409")
    void registerDuplicateEmail() throws Exception {
        register("user@example.com", "secret123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("user@example.com", "secret123"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /auth/login valid returns 200 and tokens")
    void loginSuccess() throws Exception {
        register("user@example.com", "secret123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("user@example.com", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/login wrong password returns 401")
    void loginWrongPassword() throws Exception {
        register("user@example.com", "secret123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("user@example.com", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/refresh valid returns 200")
    void refreshSuccess() throws Exception {
        register("user@example.com", "secret123");
        String refreshToken = loginAndReadRefreshToken("user@example.com", "secret123");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/logout returns 204")
    void logoutSuccess() throws Exception {
        register("user@example.com", "secret123");
        String refreshToken = loginAndReadRefreshToken("user@example.com", "secret123");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest(refreshToken))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /auth/refresh after logout returns 401")
    void refreshAfterLogout() throws Exception {
        register("user@example.com", "secret123");
        String refreshToken = loginAndReadRefreshToken("user@example.com", "secret123");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest(refreshToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("OPTIONS /auth/login returns 200 for CORS preflight")
    void loginPreflightAllowed() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /auth/users valid returns 200")
    void usersSuccess() throws Exception {
        register("user@example.com", "secret123");
        String accessToken = loginAndReadAccessToken("user@example.com", "secret123");

        mockMvc.perform(get("/auth/users")
                        .header(AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("user@example.com"));
    }

    @Test
    @DisplayName("GET /auth/users without token returns 401")
    void usersWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/auth/users"))
                .andExpect(status().isUnauthorized());
    }

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(email, password))))
                .andExpect(status().isCreated());
    }

    private String loginAndReadRefreshToken(String email, String password) throws Exception {
        JsonNode node = loginAndReadTokenPayload(email, password);
        assertThat(node.get("refreshToken").asText()).isNotBlank();
        return node.get("refreshToken").asText();
    }

    private String loginAndReadAccessToken(String email, String password) throws Exception {
        JsonNode node = loginAndReadTokenPayload(email, password);
        assertThat(node.get("accessToken").asText()).isNotBlank();
        return node.get("accessToken").asText();
    }

    private JsonNode loginAndReadTokenPayload(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
