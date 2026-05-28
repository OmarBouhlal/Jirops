package com.jiraclone.task.controller;

import com.jiraclone.task.domain.TaskComment;
import com.jiraclone.task.dto.AddCommentRequest;
import com.jiraclone.task.security.GatewayHeaderAuthenticationFilter;
import com.jiraclone.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tasks/{taskId}/comments")
public class TaskCommentController {

    private final TaskService taskService;

    public TaskCommentController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TaskComment addComment(@PathVariable String taskId,
                                  @Valid @RequestBody AddCommentRequest request,
                                  @RequestHeader(GatewayHeaderAuthenticationFilter.USER_ID_HEADER) String userId) {
        return taskService.addComment(taskId, request, userId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<TaskComment> getComments(@PathVariable String taskId) {
        return taskService.getComments(taskId);
    }
}
