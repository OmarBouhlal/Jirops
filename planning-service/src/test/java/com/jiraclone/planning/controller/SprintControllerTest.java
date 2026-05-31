// planning-service/src/test/java/com/jiraclone/planning/controller/SprintControllerTest.java
package com.jiraclone.planning.controller;

import com.jiraclone.planning.domain.Sprint;
import com.jiraclone.planning.domain.SprintStatus;
import com.jiraclone.planning.dto.AddTaskToSprintRequest;
import com.jiraclone.planning.dto.CreateSprintRequest;
import com.jiraclone.planning.dto.SprintResponse;
import com.jiraclone.planning.exception.GlobalExceptionHandler;
import com.jiraclone.planning.exception.InvalidSprintOperationException;
import com.jiraclone.planning.exception.SprintNotFoundException;
import com.jiraclone.planning.repository.SprintRepository;
import com.jiraclone.planning.service.SprintService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PlanningController.class, PlanningSecurityController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("SprintController")
class SprintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SprintService sprintService;

    @MockBean
    private SprintRepository sprintRepository;

    @MockBean
    private Tracer tracer;

    @Test
    @DisplayName("createSprint returns 201")
    void createSprintReturns201() throws Exception {
        UUID sprintId = UUID.randomUUID();
        when(sprintService.createSprint(any(CreateSprintRequest.class)))
                .thenReturn(sprintResponse(sprintId));

        mockMvc.perform(post("/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"11111111-1111-1111-1111-111111111111\",\"name\":\"Sprint\",\"goal\":\"Goal\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(sprintId.toString()));
    }

    @Test
    @DisplayName("getSprints returns 200")
    void getSprintsReturns200() throws Exception {
        when(sprintService.getSprints(any(UUID.class)))
                .thenReturn(List.of(sprintResponse(UUID.randomUUID())));

        mockMvc.perform(get("/sprints").param("projectId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("startSprint returns 200")
    void startSprintReturns200() throws Exception {
        UUID sprintId = UUID.randomUUID();
        Sprint sprint = sprint(UUID.fromString("11111111-1111-1111-1111-111111111111"), SprintStatus.PLANNING);
        when(sprintRepository.findById(sprintId)).thenReturn(java.util.Optional.of(sprint));
        when(sprintService.startSprint(eq(sprintId), eq(sprint.getProjectId()))).thenReturn(sprintResponse(sprintId));

        mockMvc.perform(post("/sprints/{id}/start", sprintId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sprintId.toString()));
    }

    @Test
    @DisplayName("startSprint conflict returns 409")
    void startSprintConflictReturns409() throws Exception {
        UUID sprintId = UUID.randomUUID();
        Sprint sprint = sprint(UUID.fromString("11111111-1111-1111-1111-111111111111"), SprintStatus.PLANNING);
        when(sprintRepository.findById(sprintId)).thenReturn(java.util.Optional.of(sprint));
        when(sprintService.startSprint(eq(sprintId), eq(sprint.getProjectId())))
                .thenThrow(new InvalidSprintOperationException("Another sprint is already active for this project"));

        mockMvc.perform(post("/sprints/{id}/start", sprintId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("completeSprint returns 200")
    void completeSprintReturns200() throws Exception {
        UUID sprintId = UUID.randomUUID();
        Sprint sprint = sprint(UUID.fromString("11111111-1111-1111-1111-111111111111"), SprintStatus.ACTIVE);
        when(sprintRepository.findById(sprintId)).thenReturn(java.util.Optional.of(sprint));
        when(sprintService.completeSprint(eq(sprintId), eq(sprint.getProjectId()))).thenReturn(sprintResponse(sprintId));

        mockMvc.perform(post("/sprints/{id}/complete", sprintId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("addTaskToSprint returns 200")
    void addTaskToSprintReturns200() throws Exception {
        UUID sprintId = UUID.randomUUID();
        when(sprintService.addTaskToSprint(eq(sprintId), any(UUID.class)))
                .thenReturn(sprintResponse(sprintId));

        mockMvc.perform(post("/sprints/{id}/tasks", sprintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"33333333-3333-3333-3333-333333333333\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sprintId.toString()));
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "USER")
    @DisplayName("security currentUser returns forwarded and authenticated user")
    void securityCurrentUserReturnsAuthenticatedDetails() throws Exception {
        mockMvc.perform(get("/planning/security/me")
                        .header("X-User-Id", "11111111-1111-1111-1111-111111111111")
                        .header("X-Roles", "ROLE_USER,ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forwardedUserId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.forwardedRoles").value("ROLE_USER,ROLE_ADMIN"))
                .andExpect(jsonPath("$.authenticatedUserId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.authenticatedRoles[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("missing sprint maps to 404")
    void missingSprintMapsTo404() throws Exception {
        UUID sprintId = UUID.randomUUID();
        when(sprintRepository.findById(sprintId)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(post("/sprints/{id}/start", sprintId))
                .andExpect(status().isNotFound());
    }

    private SprintResponse sprintResponse(UUID id) {
        return new SprintResponse(id,
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Sprint",
                "Goal",
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                SprintStatus.PLANNING,
                List.of(),
                Instant.now());
    }

    private Sprint sprint(UUID projectId, SprintStatus status) {
        Sprint sprint = new Sprint();
        sprint.setId(UUID.randomUUID());
        sprint.setProjectId(projectId);
        sprint.setName("Sprint");
        sprint.setGoal("Goal");
        sprint.setStatus(status);
        sprint.setCreatedAt(Instant.now());
        return sprint;
    }
}
