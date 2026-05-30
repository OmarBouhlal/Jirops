// planning-service/src/main/java/com/jiraclone/planning/service/SprintService.java
package com.jiraclone.planning.service;

import com.jiraclone.planning.domain.Sprint;
import com.jiraclone.planning.domain.SprintStatus;
import com.jiraclone.planning.domain.SprintTask;
import com.jiraclone.planning.dto.CreateSprintRequest;
import com.jiraclone.planning.dto.SprintResponse;
import com.jiraclone.planning.event.SprintCompletedEvent;
import com.jiraclone.planning.event.SprintStartedEvent;
import com.jiraclone.planning.exception.InvalidSprintOperationException;
import com.jiraclone.planning.exception.SprintNotFoundException;
import com.jiraclone.planning.kafka.SprintEventPublisher;
import com.jiraclone.planning.repository.SprintRepository;
import com.jiraclone.planning.repository.SprintTaskRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final SprintTaskRepository sprintTaskRepository;
    private final SprintEventPublisher sprintEventPublisher;

    public SprintService(SprintRepository sprintRepository,
                         SprintTaskRepository sprintTaskRepository,
                         SprintEventPublisher sprintEventPublisher) {
        this.sprintRepository = sprintRepository;
        this.sprintTaskRepository = sprintTaskRepository;
        this.sprintEventPublisher = sprintEventPublisher;
    }

    @Transactional
    public SprintResponse createSprint(CreateSprintRequest request) {
        Sprint sprint = new Sprint();
        sprint.setProjectId(request.projectId());
        sprint.setName(request.name().trim());
        sprint.setGoal(normalizeText(request.goal()));
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());
        sprint.setStatus(SprintStatus.PLANNING);
        sprint.setCreatedAt(Instant.now());

        Sprint saved = sprintRepository.saveAndFlush(sprint);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprints(UUID projectId) {
        return sprintRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SprintResponse startSprint(UUID sprintId, UUID projectId) {
        Sprint sprint = getSprintOrThrow(sprintId, projectId);
        if (sprint.getStatus() == SprintStatus.CLOSED) {
            throw new InvalidSprintOperationException("Closed sprint cannot be started");
        }

        sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)
                .filter(active -> !active.getId().equals(sprintId))
                .ifPresent(active -> {
                    throw new InvalidSprintOperationException("Another sprint is already active for this project");
                });

        if (sprint.getStatus() == SprintStatus.ACTIVE) {
            return toResponse(sprint);
        }

        if (sprint.getStartDate() == null) {
            sprint.setStartDate(LocalDate.now());
        }
        sprint.setStatus(SprintStatus.ACTIVE);
        Sprint saved = sprintRepository.save(sprint);

        publishAfterCommit(() -> sprintEventPublisher.publishSprintStarted(
                new SprintStartedEvent(saved.getId(), saved.getProjectId(), saved.getStartDate(), Instant.now())
        ));
        return toResponse(saved);
    }

    @Transactional
    public SprintResponse completeSprint(UUID sprintId, UUID projectId) {
        Sprint sprint = getSprintOrThrow(sprintId, projectId);
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new InvalidSprintOperationException("Only an active sprint can be completed");
        }

        sprint.setStatus(SprintStatus.CLOSED);
        Sprint saved = sprintRepository.save(sprint);
        Instant now = Instant.now();
        publishAfterCommit(() -> sprintEventPublisher.publishSprintCompleted(
                new SprintCompletedEvent(saved.getId(), saved.getProjectId(), now, now)
        ));
        return toResponse(saved);
    }

    @Transactional
    public SprintResponse addTaskToSprint(UUID sprintId, UUID taskId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException("Sprint not found"));

        if (sprint.getStatus() != SprintStatus.PLANNING && sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new InvalidSprintOperationException("Tasks can only be added to planning or active sprints");
        }

        if (!sprintTaskRepository.existsByIdSprintIdAndIdTaskId(sprintId, taskId)) {
            try {
                sprintTaskRepository.saveAndFlush(new SprintTask(sprintId, taskId));
            } catch (DataIntegrityViolationException ex) {
                // Another request inserted the same mapping concurrently; return the latest state.
            }
        }

        return toResponse(sprint);
    }

    private Sprint getSprintOrThrow(UUID sprintId, UUID projectId) {
        if (projectId == null) {
            return sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new SprintNotFoundException("Sprint not found"));
        }
        return sprintRepository.findByIdAndProjectId(sprintId, projectId)
                .orElseThrow(() -> new SprintNotFoundException("Sprint not found"));
    }

    private SprintResponse toResponse(Sprint sprint) {
        List<UUID> taskIds = sprintTaskRepository.findByIdSprintId(sprint.getId()).stream()
                .map(SprintTask::getId)
                .map(id -> id.getTaskId())
                .toList();

        return new SprintResponse(
                sprint.getId(),
                sprint.getProjectId(),
                sprint.getName(),
                sprint.getGoal(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getStatus(),
                List.copyOf(taskIds),
                sprint.getCreatedAt()
        );
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
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
