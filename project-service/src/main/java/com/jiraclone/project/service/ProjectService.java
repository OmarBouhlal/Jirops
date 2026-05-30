// project-service/src/main/java/com/jiraclone/project/service/ProjectService.java
package com.jiraclone.project.service;

import com.jiraclone.project.domain.Project;
import com.jiraclone.project.dto.CreateProjectRequest;
import com.jiraclone.project.dto.ProjectResponse;
import com.jiraclone.project.dto.UpdateProjectRequest;
import com.jiraclone.project.event.ProjectCreatedEvent;
import com.jiraclone.project.event.ProjectDeletedEvent;
import com.jiraclone.project.exception.DuplicateKeyException;
import com.jiraclone.project.exception.ProjectNotFoundException;
import com.jiraclone.project.exception.UnauthorizedException;
import com.jiraclone.project.kafka.ProjectEventPublisher;
import com.jiraclone.project.repository.ProjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectEventPublisher projectEventPublisher;

    public ProjectService(ProjectRepository projectRepository, ProjectEventPublisher projectEventPublisher) {
        this.projectRepository = projectRepository;
        this.projectEventPublisher = projectEventPublisher;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, UUID ownerId) {
        String normalizedKey = request.key().trim().toUpperCase(Locale.ROOT);
        if (projectRepository.existsByKey(normalizedKey)) {
            throw new DuplicateKeyException("Project key already exists");
        }

        Project project = new Project();
        project.setName(request.name().trim());
        project.setKey(normalizedKey);
        project.setDescription(normalizeText(request.description()));
        project.setOwnerId(ownerId);
        project.setDeleted(false);
        project.setCreatedAt(Instant.now());
        project.setMembers(new ArrayList<>());

        Project saved;
        try {
            saved = projectRepository.saveAndFlush(project);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyException("Project key already exists");
        }

        ProjectResponse response = toResponse(saved);
        publishAfterCommit(() -> projectEventPublisher.publishProjectCreated(
                new ProjectCreatedEvent(saved.getId(), saved.getName(), saved.getOwnerId(), Instant.now())
        ));
        return response;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects(UUID userId) {
        return projectRepository.findByOwnerIdOrMembersContainingAndDeletedFalse(userId, userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID id, UUID userId) {
        Project project = getProjectOrThrow(id);
        ensureAccess(project, userId);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(UUID id, UUID userId, UpdateProjectRequest request) {
        Project project = getProjectOrThrow(id);
        ensureOwner(project, userId);

        if (StringUtils.hasText(request.name())) {
            project.setName(request.name().trim());
        }
        if (request.description() != null) {
            project.setDescription(normalizeText(request.description()));
        }

        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(UUID id, UUID userId) {
        Project project = getProjectOrThrow(id);
        ensureOwner(project, userId);

        project.setDeleted(true);
        projectRepository.save(project);

        publishAfterCommit(() -> projectEventPublisher.publishProjectDeleted(
                new ProjectDeletedEvent(project.getId(), Instant.now())
        ));
    }

    @Transactional
    public ProjectResponse addMember(UUID projectId, UUID userId, UUID memberToAdd) {
        Project project = getProjectOrThrow(projectId);
        ensureOwner(project, userId);

        List<UUID> members = project.getMembers();
        if (members == null) {
            members = new ArrayList<>();
            project.setMembers(members);
        }
        if (!members.contains(memberToAdd)) {
            members.add(memberToAdd);
        }

        return toResponse(projectRepository.save(project));
    }

    private Project getProjectOrThrow(UUID id) {
        return projectRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));
    }

    private void ensureAccess(Project project, UUID userId) {
        if (!project.getOwnerId().equals(userId) && !project.getMembers().contains(userId)) {
            throw new UnauthorizedException("You do not have access to this project");
        }
    }

    private void ensureOwner(Project project, UUID userId) {
        if (!project.getOwnerId().equals(userId)) {
            throw new UnauthorizedException("Only the project owner can perform this action");
        }
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getKey(),
                project.getDescription(),
                project.getOwnerId(),
                project.getMembers() == null ? List.of() : List.copyOf(project.getMembers()),
                project.getCreatedAt()
        );
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void publishAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }
}
