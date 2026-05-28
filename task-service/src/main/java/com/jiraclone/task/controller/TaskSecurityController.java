package com.jiraclone.task.controller;

import com.jirops.jwt.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tasks/security")
public class TaskSecurityController {

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CurrentUserResponse currentUser(@RequestHeader("X-User-Id") String userId,
                                           @RequestHeader(value = "X-Roles", required = false) String rolesHeader) {
        return new CurrentUserResponse(
                userId,
                rolesHeader,
                AuthenticatedUser.currentUserId().orElse(null),
                AuthenticatedUser.currentRoles()
        );
    }

    public record CurrentUserResponse(String forwardedUserId,
                                      String forwardedRoles,
                                      String authenticatedUserId,
                                      List<String> authenticatedRoles) {
    }
}
