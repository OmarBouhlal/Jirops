// planning-service/src/main/java/com/jiraclone/planning/controller/PlanningController.java
package com.jiraclone.planning.controller;

import com.jiraclone.planning.dto.AddTaskToSprintRequest;
import com.jiraclone.planning.dto.CreateSprintRequest;
import com.jiraclone.planning.dto.SprintResponse;
import com.jiraclone.planning.service.SprintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sprints")
public class PlanningController {

    private final SprintService sprintService;

    public PlanningController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @PostMapping
    public ResponseEntity<SprintResponse> createSprint(@Valid @RequestBody CreateSprintRequest request,
                                                       @RequestHeader("X-User-Id") String userId) {
        requireUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sprintService.createSprint(request));
    }

    @GetMapping
    public ResponseEntity<List<SprintResponse>> getSprints(@RequestParam UUID projectId,
                                                           @RequestHeader("X-User-Id") String userId) {
        requireUserId(userId);
        return ResponseEntity.ok(sprintService.getSprints(projectId));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<SprintResponse> startSprint(@PathVariable UUID id,
                                                      @RequestParam(required = false) UUID projectId,
                                                      @RequestHeader("X-User-Id") String userId) {
        requireUserId(userId);
        return ResponseEntity.ok(sprintService.startSprint(id, projectId));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<SprintResponse> completeSprint(@PathVariable UUID id,
                                                         @RequestParam(required = false) UUID projectId,
                                                         @RequestHeader("X-User-Id") String userId) {
        requireUserId(userId);
        return ResponseEntity.ok(sprintService.completeSprint(id, projectId));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<SprintResponse> addTaskToSprint(@PathVariable UUID id,
                                                          @Valid @RequestBody AddTaskToSprintRequest request,
                                                          @RequestHeader("X-User-Id") String userId) {
        requireUserId(userId);
        return ResponseEntity.ok(sprintService.addTaskToSprint(id, request.taskId()));
    }

    private void requireUserId(String userId) {
        UUID.fromString(userId.trim());
    }
}
