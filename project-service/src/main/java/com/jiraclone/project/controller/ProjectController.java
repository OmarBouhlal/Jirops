// project-service/src/main/java/com/jiraclone/project/controller/ProjectController.java
package com.jiraclone.project.controller;

import com.jiraclone.project.dto.AddMemberRequest;
import com.jiraclone.project.dto.CreateProjectRequest;
import com.jiraclone.project.dto.ProjectResponse;
import com.jiraclone.project.dto.UpdateProjectRequest;
import com.jiraclone.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request,
                                                         @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(request, parseUserId(userId)));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(projectService.getProjects(parseUserId(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable UUID id,
                                                          @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(projectService.getProjectById(id, parseUserId(userId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable UUID id,
                                                         @Valid @RequestBody UpdateProjectRequest request,
                                                         @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(projectService.updateProject(id, parseUserId(userId), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id,
                                              @RequestHeader("X-User-Id") String userId) {
        projectService.deleteProject(id, parseUserId(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ProjectResponse> addMember(@PathVariable UUID id,
                                                     @Valid @RequestBody AddMemberRequest request,
                                                     @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(projectService.addMember(id, parseUserId(userId), request.userId()));
    }

    private UUID parseUserId(String userId) {
        return UUID.fromString(userId.trim());
    }
}
