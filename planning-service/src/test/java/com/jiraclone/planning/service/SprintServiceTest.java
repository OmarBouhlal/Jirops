// planning-service/src/test/java/com/jiraclone/planning/service/SprintServiceTest.java
package com.jiraclone.planning.service;

import com.jiraclone.planning.domain.Sprint;
import com.jiraclone.planning.domain.SprintStatus;
import com.jiraclone.planning.domain.SprintTask;
import com.jiraclone.planning.dto.CreateSprintRequest;
import com.jiraclone.planning.dto.SprintResponse;
import com.jiraclone.planning.event.SprintCompletedEvent;
import com.jiraclone.planning.event.SprintStartedEvent;
import com.jiraclone.planning.exception.InvalidSprintOperationException;
import com.jiraclone.planning.kafka.SprintEventPublisher;
import com.jiraclone.planning.repository.SprintRepository;
import com.jiraclone.planning.repository.SprintTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SprintService")
class SprintServiceTest {

    private SprintRepository sprintRepository;
    private SprintTaskRepository sprintTaskRepository;
    private SprintEventPublisher sprintEventPublisher;
    private SprintService sprintService;

    @BeforeEach
    void setUp() {
        sprintRepository = mock(SprintRepository.class);
        sprintTaskRepository = mock(SprintTaskRepository.class);
        sprintEventPublisher = mock(SprintEventPublisher.class);
        sprintService = new SprintService(sprintRepository, sprintTaskRepository, sprintEventPublisher);
    }

    @Test
    @DisplayName("createSprint_success")
    void createSprint_success() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(sprintRepository.saveAndFlush(any(Sprint.class))).thenAnswer(invocation -> {
            Sprint sprint = invocation.getArgument(0);
            sprint.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
            return sprint;
        });
        when(sprintTaskRepository.findByIdSprintId(any())).thenReturn(List.of());

        SprintResponse response = sprintService.createSprint(new CreateSprintRequest(
                projectId,
                "Sprint 1",
                "  planning  ",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 15)
        ));

        assertThat(response.status()).isEqualTo(SprintStatus.PLANNING);
        assertThat(response.projectId()).isEqualTo(projectId);
        ArgumentCaptor<Sprint> captor = ArgumentCaptor.forClass(Sprint.class);
        verify(sprintRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SprintStatus.PLANNING);
    }

    @Test
    @DisplayName("createSprint_blankGoalBecomesNull")
    void createSprint_blankGoalBecomesNull() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(sprintRepository.saveAndFlush(any(Sprint.class))).thenAnswer(invocation -> {
            Sprint sprint = invocation.getArgument(0);
            sprint.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
            return sprint;
        });
        when(sprintTaskRepository.findByIdSprintId(any())).thenReturn(List.of());

        SprintResponse response = sprintService.createSprint(new CreateSprintRequest(
                projectId,
                "Sprint 1",
                "   ",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 15)
        ));

        assertThat(response.goal()).isNull();
        ArgumentCaptor<Sprint> captor = ArgumentCaptor.forClass(Sprint.class);
        verify(sprintRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getGoal()).isNull();
    }

    @Test
    @DisplayName("getSprints_returnsProjectSprints")
    void getSprints_returnsProjectSprints() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Sprint first = sprint(projectId, SprintStatus.PLANNING);
        Sprint second = sprint(projectId, SprintStatus.ACTIVE);
        UUID firstTaskId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID secondTaskId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        when(sprintRepository.findByProjectId(projectId)).thenReturn(List.of(first, second));
        when(sprintTaskRepository.findByIdSprintId(first.getId()))
                .thenReturn(List.of(new SprintTask(first.getId(), firstTaskId)));
        when(sprintTaskRepository.findByIdSprintId(second.getId()))
                .thenReturn(List.of(new SprintTask(second.getId(), secondTaskId)));

        List<SprintResponse> result = sprintService.getSprints(projectId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SprintResponse::id).containsExactly(first.getId(), second.getId());
        assertThat(result.get(0).taskIds()).containsExactly(firstTaskId);
        assertThat(result.get(1).taskIds()).containsExactly(secondTaskId);
        verify(sprintRepository).findByProjectId(projectId);
    }

    @Test
    @DisplayName("startSprint_success")
    void startSprint_success() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Sprint sprint = sprint(projectId, SprintStatus.PLANNING);
        when(sprintRepository.findByIdAndProjectId(sprint.getId(), projectId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)).thenReturn(Optional.empty());
        when(sprintRepository.save(sprint)).thenAnswer(invocation -> invocation.getArgument(0));
        when(sprintTaskRepository.findByIdSprintId(sprint.getId())).thenReturn(List.of());

        SprintResponse response = sprintService.startSprint(sprint.getId(), projectId);

        assertThat(response.status()).isEqualTo(SprintStatus.ACTIVE);
        verify(sprintRepository).save(sprint);
        ArgumentCaptor<SprintStartedEvent> captor = ArgumentCaptor.forClass(SprintStartedEvent.class);
        verify(sprintEventPublisher).publishSprintStarted(captor.capture());
        assertThat(captor.getValue().projectId()).isEqualTo(projectId);
    }

    @Test
    @DisplayName("startSprint_alreadyActiveReturnsCurrentSprint")
    void startSprint_alreadyActiveReturnsCurrentSprint() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Sprint sprint = sprint(projectId, SprintStatus.ACTIVE);
        when(sprintRepository.findByIdAndProjectId(sprint.getId(), projectId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)).thenReturn(Optional.of(sprint));
        when(sprintTaskRepository.findByIdSprintId(sprint.getId())).thenReturn(List.of());

        SprintResponse response = sprintService.startSprint(sprint.getId(), projectId);

        assertThat(response.status()).isEqualTo(SprintStatus.ACTIVE);
        verify(sprintRepository, never()).save(any(Sprint.class));
        verify(sprintEventPublisher, never()).publishSprintStarted(any(SprintStartedEvent.class));
    }

    @Test
    @DisplayName("startSprint_assignsCurrentDateWhenMissing")
    void startSprint_assignsCurrentDateWhenMissing() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Sprint sprint = sprint(projectId, SprintStatus.PLANNING);
        sprint.setStartDate(null);
        when(sprintRepository.findByIdAndProjectId(sprint.getId(), projectId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)).thenReturn(Optional.empty());
        when(sprintRepository.save(sprint)).thenAnswer(invocation -> invocation.getArgument(0));
        when(sprintTaskRepository.findByIdSprintId(sprint.getId())).thenReturn(List.of());

        SprintResponse response = sprintService.startSprint(sprint.getId(), projectId);

        assertThat(response.startDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("startSprint_withoutProjectIdUsesSprintLookup")
    void startSprint_withoutProjectIdUsesSprintLookup() {
        Sprint sprint = sprint(UUID.fromString("11111111-1111-1111-1111-111111111111"), SprintStatus.PLANNING);
        when(sprintRepository.findById(sprint.getId())).thenReturn(Optional.of(sprint));
        when(sprintRepository.findByProjectIdAndStatus(null, SprintStatus.ACTIVE)).thenReturn(Optional.empty());
        when(sprintRepository.save(sprint)).thenAnswer(invocation -> invocation.getArgument(0));
        when(sprintTaskRepository.findByIdSprintId(sprint.getId())).thenReturn(List.of());

        SprintResponse response = sprintService.startSprint(sprint.getId(), null);

        assertThat(response.id()).isEqualTo(sprint.getId());
        verify(sprintRepository).findById(sprint.getId());
    }

    @Test
    @DisplayName("startSprint_alreadyActiveForProject")
    void startSprint_alreadyActiveForProject() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Sprint target = sprint(projectId, SprintStatus.PLANNING);
        Sprint active = sprint(projectId, SprintStatus.ACTIVE);
        when(sprintRepository.findByIdAndProjectId(target.getId(), projectId)).thenReturn(Optional.of(target));
        when(sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> sprintService.startSprint(target.getId(), projectId))
                .isInstanceOf(InvalidSprintOperationException.class);
        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    @Test
    @DisplayName("startSprint_closedSprint")
    void startSprint_closedSprint() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Sprint sprint = sprint(projectId, SprintStatus.CLOSED);
        when(sprintRepository.findByIdAndProjectId(sprint.getId(), projectId)).thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> sprintService.startSprint(sprint.getId(), projectId))
                .isInstanceOf(InvalidSprintOperationException.class);
        verify(sprintRepository, never()).save(any(Sprint.class));
        verify(sprintEventPublisher, never()).publishSprintStarted(any(SprintStartedEvent.class));
    }

    @Test
    @DisplayName("completeSprint_success")
    void completeSprint_success() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Sprint sprint = sprint(projectId, SprintStatus.ACTIVE);
        when(sprintRepository.findByIdAndProjectId(sprint.getId(), projectId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenAnswer(invocation -> invocation.getArgument(0));
        when(sprintTaskRepository.findByIdSprintId(sprint.getId())).thenReturn(List.of());

        SprintResponse response = sprintService.completeSprint(sprint.getId(), projectId);

        assertThat(response.status()).isEqualTo(SprintStatus.CLOSED);
        verify(sprintRepository).save(sprint);
        verify(sprintEventPublisher).publishSprintCompleted(any(SprintCompletedEvent.class));
    }

    @Test
    @DisplayName("startSprint_publishesAfterCommitWhenSynchronizationActive")
    void startSprint_publishesAfterCommitWhenSynchronizationActive() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Sprint sprint = sprint(projectId, SprintStatus.PLANNING);
        when(sprintRepository.findByIdAndProjectId(sprint.getId(), projectId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)).thenReturn(Optional.empty());
        when(sprintRepository.save(sprint)).thenAnswer(invocation -> invocation.getArgument(0));
        when(sprintTaskRepository.findByIdSprintId(sprint.getId())).thenReturn(List.of());

        TransactionSynchronizationManager.initSynchronization();
        try {
            sprintService.startSprint(sprint.getId(), projectId);
            verify(sprintEventPublisher, never()).publishSprintStarted(any(SprintStartedEvent.class));

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(sprintEventPublisher).publishSprintStarted(any(SprintStartedEvent.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("completeSprint_publishesAfterCommitWhenSynchronizationActive")
    void completeSprint_publishesAfterCommitWhenSynchronizationActive() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Sprint sprint = sprint(projectId, SprintStatus.ACTIVE);
        when(sprintRepository.findByIdAndProjectId(sprint.getId(), projectId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenAnswer(invocation -> invocation.getArgument(0));
        when(sprintTaskRepository.findByIdSprintId(sprint.getId())).thenReturn(List.of());

        TransactionSynchronizationManager.initSynchronization();
        try {
            sprintService.completeSprint(sprint.getId(), projectId);
            verify(sprintEventPublisher, never()).publishSprintCompleted(any(SprintCompletedEvent.class));

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(sprintEventPublisher).publishSprintCompleted(any(SprintCompletedEvent.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("completeSprint_notActive")
    void completeSprint_notActive() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Sprint sprint = sprint(projectId, SprintStatus.PLANNING);
        when(sprintRepository.findByIdAndProjectId(sprint.getId(), projectId)).thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> sprintService.completeSprint(sprint.getId(), projectId))
                .isInstanceOf(InvalidSprintOperationException.class);
    }

    @Test
    @DisplayName("completeSprint_withoutProjectIdUsesSprintLookup")
    void completeSprint_withoutProjectIdUsesSprintLookup() {
        Sprint sprint = sprint(UUID.fromString("11111111-1111-1111-1111-111111111111"), SprintStatus.ACTIVE);
        when(sprintRepository.findById(sprint.getId())).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenAnswer(invocation -> invocation.getArgument(0));
        when(sprintTaskRepository.findByIdSprintId(sprint.getId())).thenReturn(List.of());

        SprintResponse response = sprintService.completeSprint(sprint.getId(), null);

        assertThat(response.status()).isEqualTo(SprintStatus.CLOSED);
        verify(sprintRepository).findById(sprint.getId());
    }

    @Test
    @DisplayName("addTaskToSprint_planningStatus")
    void addTaskToSprint_planningStatus() {
        UUID taskId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Sprint sprint = sprint(UUID.fromString("11111111-1111-1111-1111-111111111111"), SprintStatus.PLANNING);
        when(sprintRepository.findById(sprint.getId())).thenReturn(Optional.of(sprint));
        when(sprintTaskRepository.existsByIdSprintIdAndIdTaskId(sprint.getId(), taskId)).thenReturn(false);
        when(sprintTaskRepository.findByIdSprintId(sprint.getId())).thenReturn(List.of(new SprintTask(sprint.getId(), taskId)));

        SprintResponse response = sprintService.addTaskToSprint(sprint.getId(), taskId);

        assertThat(response.taskIds()).containsExactly(taskId);
        verify(sprintTaskRepository).saveAndFlush(any(SprintTask.class));
    }

    @Test
    @DisplayName("addTaskToSprint_existingMappingDoesNotDuplicate")
    void addTaskToSprint_existingMappingDoesNotDuplicate() {
        UUID taskId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Sprint sprint = sprint(UUID.fromString("11111111-1111-1111-1111-111111111111"), SprintStatus.PLANNING);
        when(sprintRepository.findById(sprint.getId())).thenReturn(Optional.of(sprint));
        when(sprintTaskRepository.existsByIdSprintIdAndIdTaskId(sprint.getId(), taskId)).thenReturn(true);
        when(sprintTaskRepository.findByIdSprintId(sprint.getId())).thenReturn(List.of(new SprintTask(sprint.getId(), taskId)));

        SprintResponse response = sprintService.addTaskToSprint(sprint.getId(), taskId);

        assertThat(response.taskIds()).containsExactly(taskId);
        verify(sprintTaskRepository, never()).saveAndFlush(any(SprintTask.class));
    }

    @Test
    @DisplayName("addTaskToSprint_closedSprint")
    void addTaskToSprint_closedSprint() {
        UUID taskId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Sprint sprint = sprint(UUID.fromString("11111111-1111-1111-1111-111111111111"), SprintStatus.CLOSED);
        when(sprintRepository.findById(sprint.getId())).thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> sprintService.addTaskToSprint(sprint.getId(), taskId))
                .isInstanceOf(InvalidSprintOperationException.class);
        verify(sprintTaskRepository, never()).saveAndFlush(any(SprintTask.class));
    }

    private Sprint sprint(UUID projectId, SprintStatus status) {
        Sprint sprint = new Sprint();
        sprint.setId(UUID.randomUUID());
        sprint.setProjectId(projectId);
        sprint.setName("Sprint " + status);
        sprint.setGoal("Goal");
        sprint.setStartDate(LocalDate.of(2026, 6, 1));
        sprint.setEndDate(LocalDate.of(2026, 6, 15));
        sprint.setStatus(status);
        sprint.setCreatedAt(Instant.now());
        return sprint;
    }
}
