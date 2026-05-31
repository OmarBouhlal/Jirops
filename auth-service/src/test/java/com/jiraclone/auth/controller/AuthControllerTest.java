// auth-service/src/test/java/com/jiraclone/auth/controller/AuthControllerTest.java
package com.jiraclone.auth.controller;

import com.jiraclone.auth.dto.AuthResponse;
import com.jiraclone.auth.dto.LoginRequest;
import com.jiraclone.auth.dto.RefreshRequest;
import com.jiraclone.auth.dto.RegisterRequest;
import com.jiraclone.auth.exception.EmailAlreadyExistsException;
import com.jiraclone.auth.exception.InvalidCredentialsException;
import com.jiraclone.auth.exception.InvalidTokenException;
import com.jiraclone.auth.service.AuthService;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.jiraclone.auth.exception.GlobalExceptionHandler.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private Tracer tracer;

    @Test
    @DisplayName("register returns 201")
    void registerReturns201() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("access", "refresh", "Bearer", 600));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("register duplicate returns 409")
    void registerDuplicateReturns409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email already exists"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("login returns 200")
    void loginReturns200() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("access", "refresh", "Bearer", 600));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("login invalid returns 401")
    void loginInvalidReturns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refresh returns 200")
    void refreshReturns200() throws Exception {
        when(authService.refresh(any(RefreshRequest.class)))
                .thenReturn(new AuthResponse("access", "refresh", "Bearer", 600));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("logout returns 204")
    void logoutReturns204() throws Exception {
        doNothing().when(authService).logout(any(RefreshRequest.class));

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token\"}"))
                .andExpect(status().isNoContent());
    }
}
