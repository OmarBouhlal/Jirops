package com.jiraclone.task.controller;

import com.jiraclone.task.domain.TaskPriority;
import com.jiraclone.task.domain.TaskStatus;
import com.jiraclone.task.dto.CreateTaskRequest;
import com.jiraclone.task.dto.TaskResponse;
import com.jiraclone.task.dto.UpdateTaskRequest;
import com.jiraclone.task.dto.UpdateTaskStatusRequest;
import com.jiraclone.task.security.GatewayHeaderAuthenticationFilter;
import com.jiraclone.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TaskResponse createTask(@Valid @RequestBody CreateTaskRequest request,
                                   @RequestHeader(GatewayHeaderAuthenticationFilter.USER_ID_HEADER) String userId) {
        return TaskResponse.from(taskService.createTask(request, userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<TaskResponse> searchTasks(@RequestParam(required = false) String projectId,
                                          @RequestParam(required = false) TaskStatus status,
                                          @RequestParam(required = false) TaskPriority priority,
                                          @RequestParam(required = false) String assignee,
                                          @RequestParam(required = false) String label) {
        return taskService.searchTasks(projectId, status, priority, assignee, label).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TaskResponse getTask(@PathVariable String id) {
        return TaskResponse.from(taskService.getTask(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TaskResponse updateTask(@PathVariable String id,
                                   @RequestBody UpdateTaskRequest request) {
        return TaskResponse.from(taskService.updateTask(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TaskResponse updateStatus(@PathVariable String id,
                                     @Valid @RequestBody UpdateTaskStatusRequest request,
                                     @RequestHeader(GatewayHeaderAuthenticationFilter.USER_ID_HEADER) String userId) {
        return TaskResponse.from(taskService.updateStatus(id, request.status(), userId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public void deleteTask(@PathVariable String id) {
        taskService.deleteTask(id);
    }
}
