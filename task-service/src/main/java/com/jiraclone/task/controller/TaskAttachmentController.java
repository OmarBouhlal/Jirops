package com.jiraclone.task.controller;

import com.jiraclone.task.domain.TaskAttachment;
import com.jiraclone.task.dto.AddAttachmentRequest;
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
@RequestMapping("/tasks/{taskId}/attachments")
public class TaskAttachmentController {

    private final TaskService taskService;

    public TaskAttachmentController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TaskAttachment addAttachment(@PathVariable String taskId,
                                        @Valid @RequestBody AddAttachmentRequest request,
                                        @RequestHeader(GatewayHeaderAuthenticationFilter.USER_ID_HEADER) String userId) {
        return taskService.addAttachment(taskId, request, userId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<TaskAttachment> getAttachments(@PathVariable String taskId) {
        return taskService.getAttachments(taskId);
    }
}
