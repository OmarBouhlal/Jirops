// project-service/src/test/java/com/jiraclone/project/controller/ProjectControllerTest.java
package com.jiraclone.project.controller;

import com.jiraclone.project.dto.AddMemberRequest;
import com.jiraclone.project.dto.CreateProjectRequest;
import com.jiraclone.project.dto.ProjectResponse;
import com.jiraclone.project.dto.UpdateProjectRequest;
import com.jiraclone.project.exception.GlobalExceptionHandler;
import com.jiraclone.project.exception.ProjectNotFoundException;
import com.jiraclone.project.exception.UnauthorizedException;
import com.jiraclone.project.service.ProjectService;
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
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProjectController.class, ProjectSecurityController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("ProjectController")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private Tracer tracer;

    @Test
    @DisplayName("createProject returns 201")
    void createProjectReturns201() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.createProject(any(CreateProjectRequest.class), any(UUID.class)))
                .thenReturn(projectResponse(projectId));

        mockMvc.perform(post("/projects")
                        .header("X-User-Id", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Project\",\"key\":\"PRJ1\",\"description\":\"desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(projectId.toString()));
    }

    @Test
    @DisplayName("getProjects returns 200")
    void getProjectsReturns200() throws Exception {
        when(projectService.getProjects(any(UUID.class)))
                .thenReturn(List.of(projectResponse(UUID.randomUUID())));

        mockMvc.perform(get("/projects")
                        .header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("missing X-User-Id returns 400")
    void missingUserHeaderReturns400() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getProjectById returns 200")
    void getProjectByIdReturns200() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.getProjectById(eq(projectId), any(UUID.class)))
                .thenReturn(projectResponse(projectId));

        mockMvc.perform(get("/projects/{id}", projectId)
                        .header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()));
    }

    @Test
    @DisplayName("getProjectById unknown returns 404")
    void getProjectByIdUnknownReturns404() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.getProjectById(eq(projectId), any(UUID.class)))
                .thenThrow(new ProjectNotFoundException("Project not found"));

        mockMvc.perform(get("/projects/{id}", projectId)
                        .header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("invalid X-User-Id returns 400")
    void invalidUserHeaderReturns400() throws Exception {
        mockMvc.perform(get("/projects/{id}", UUID.randomUUID())
                        .header("X-User-Id", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("invalid X-User-Id on create returns 400")
    void invalidUserHeaderOnCreateReturns400() throws Exception {
        mockMvc.perform(post("/projects")
                        .header("X-User-Id", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Project\",\"key\":\"PRJ1\",\"description\":\"desc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("missing X-User-Id on update returns 400")
    void missingUserHeaderOnUpdateReturns400() throws Exception {
        mockMvc.perform(put("/projects/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\",\"description\":\"desc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("invalid X-User-Id on delete returns 400")
    void invalidUserHeaderOnDeleteReturns400() throws Exception {
        mockMvc.perform(delete("/projects/{id}", UUID.randomUUID())
                        .header("X-User-Id", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("updateProject returns 200")
    void updateProjectReturns200() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.updateProject(eq(projectId), any(UUID.class), any(UpdateProjectRequest.class)))
                .thenReturn(projectResponse(projectId));

        mockMvc.perform(put("/projects/{id}", projectId)
                        .header("X-User-Id", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\",\"description\":\"desc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()));
    }

    @Test
    @DisplayName("deleteProject returns 204")
    void deleteProjectReturns204() throws Exception {
        doNothing().when(projectService).deleteProject(any(UUID.class), any(UUID.class));

        mockMvc.perform(delete("/projects/{id}", UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("addMember returns 200")
    void addMemberReturns200() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.addMember(eq(projectId), any(UUID.class), any(UUID.class)))
                .thenReturn(projectResponse(projectId));

        mockMvc.perform(post("/projects/{id}/members", projectId)
                        .header("X-User-Id", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"33333333-3333-3333-3333-333333333333\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "USER")
    @DisplayName("security currentUser returns forwarded and authenticated user")
    void securityCurrentUserReturnsAuthenticatedDetails() throws Exception {
        mockMvc.perform(get("/projects/security/me")
                        .header("X-User-Id", "11111111-1111-1111-1111-111111111111")
                        .header("X-Roles", "ROLE_USER,ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forwardedUserId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.forwardedRoles").value("ROLE_USER,ROLE_ADMIN"))
                .andExpect(jsonPath("$.authenticatedUserId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.authenticatedRoles[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("invalid createProject body returns 400")
    void invalidCreateProjectBodyReturns400() throws Exception {
        mockMvc.perform(post("/projects")
                        .header("X-User-Id", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"key\":\"bad key\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("nonexistent delete maps to 404")
    void unauthorizedMapsTo403() throws Exception {
        doThrow(new UnauthorizedException("Only the project owner can perform this action"))
                .when(projectService).deleteProject(any(UUID.class), any(UUID.class));

        mockMvc.perform(delete("/projects/{id}", UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    private ProjectResponse projectResponse(UUID id) {
        return new ProjectResponse(id, "Project", "PRJ1", "desc", UUID.randomUUID(), List.of(), Instant.now());
    }
}
