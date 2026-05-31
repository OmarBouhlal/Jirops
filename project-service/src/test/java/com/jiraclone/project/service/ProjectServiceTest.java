// project-service/src/test/java/com/jiraclone/project/service/ProjectServiceTest.java
package com.jiraclone.project.service;

import com.jiraclone.project.domain.Project;
import com.jiraclone.project.dto.CreateProjectRequest;
import com.jiraclone.project.dto.ProjectResponse;
import com.jiraclone.project.dto.UpdateProjectRequest;
import com.jiraclone.project.event.ProjectCreatedEvent;
import com.jiraclone.project.event.ProjectDeletedEvent;
import com.jiraclone.project.exception.DuplicateKeyException;
import com.jiraclone.project.exception.UnauthorizedException;
import com.jiraclone.project.kafka.ProjectEventPublisher;
import com.jiraclone.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService")
class ProjectServiceTest {

    private ProjectRepository projectRepository;
    private ProjectEventPublisher projectEventPublisher;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        projectEventPublisher = mock(ProjectEventPublisher.class);
        projectService = new ProjectService(projectRepository, projectEventPublisher);
    }

    @Test
    @DisplayName("createProject_success")
    void createProject_success() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(projectRepository.existsByKey("ABC1")).thenReturn(false);
        when(projectRepository.saveAndFlush(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
            return project;
        });

        ProjectResponse response = projectService.createProject(
                new CreateProjectRequest("New Project", "abc1", "  important  "),
                ownerId
        );

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getKey()).isEqualTo("ABC1");
        assertThat(captor.getValue().getDescription()).isEqualTo("important");
        assertThat(response.ownerId()).isEqualTo(ownerId);
        verify(projectEventPublisher).publishProjectCreated(any(ProjectCreatedEvent.class));
    }

    @Test
    @DisplayName("createProject_duplicateKey")
    void createProject_duplicateKey() {
        when(projectRepository.existsByKey("ABC1")).thenReturn(false);
        when(projectRepository.saveAndFlush(any(Project.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> projectService.createProject(new CreateProjectRequest("New Project", "abc1", null),
                UUID.randomUUID()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("createProject_existingKey")
    void createProject_existingKey() {
        when(projectRepository.existsByKey("ABC1")).thenReturn(true);

        assertThatThrownBy(() -> projectService.createProject(new CreateProjectRequest("New Project", "abc1", null),
                UUID.randomUUID()))
                .isInstanceOf(DuplicateKeyException.class);
        verify(projectRepository, never()).saveAndFlush(any(Project.class));
    }

    @Test
    @DisplayName("getProjects_returnsOnlyUserProjects")
    void getProjects_returnsOnlyUserProjects() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Project ownerProject = project("owner", userId, List.of());
        Project memberProject = project("member", UUID.randomUUID(), List.of(userId));
        when(projectRepository.findByOwnerIdOrMembersContainingAndDeletedFalse(userId, userId))
                .thenReturn(List.of(ownerProject, memberProject));

        List<ProjectResponse> result = projectService.getProjects(userId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProjectResponse::id).containsExactlyInAnyOrder(ownerProject.getId(), memberProject.getId());
        verify(projectRepository).findByOwnerIdOrMembersContainingAndDeletedFalse(userId, userId);
    }

    @Test
    @DisplayName("getProjectById_userIsOwner")
    void getProjectById_userIsOwner() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Project project = project("owner", userId, List.of());
        when(projectRepository.findByIdAndDeletedFalse(project.getId())).thenReturn(Optional.of(project));

        ProjectResponse result = projectService.getProjectById(project.getId(), userId);

        assertThat(result.id()).isEqualTo(project.getId());
    }

    @Test
    @DisplayName("getProjectById_userNotMember")
    void getProjectById_userNotMember() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Project project = project("owner", ownerId, List.of());
        when(projectRepository.findByIdAndDeletedFalse(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.getProjectById(project.getId(), userId))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("updateProject_ownerCanUpdate")
    void updateProject_ownerCanUpdate() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Project project = project("owner", ownerId, List.of());
        when(projectRepository.findByIdAndDeletedFalse(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.updateProject(project.getId(), ownerId,
                new UpdateProjectRequest("Updated Name", "  new description  "));

        assertThat(response.name()).isEqualTo("Updated Name");
        assertThat(response.description()).isEqualTo("new description");
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("updateProject_blankNameKeepsOriginal")
    void updateProject_blankNameKeepsOriginal() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Project project = project("owner", ownerId, List.of());
        when(projectRepository.findByIdAndDeletedFalse(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.updateProject(project.getId(), ownerId,
                new UpdateProjectRequest("   ", null));

        assertThat(response.name()).isEqualTo("owner");
        assertThat(response.description()).isEqualTo("description");
    }

    @Test
    @DisplayName("updateProject_nullDescriptionKeepsExistingValue")
    void updateProject_nullDescriptionKeepsExistingValue() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Project project = project("owner", ownerId, List.of());
        when(projectRepository.findByIdAndDeletedFalse(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.updateProject(project.getId(), ownerId,
                new UpdateProjectRequest("Updated Name", null));

        assertThat(response.name()).isEqualTo("Updated Name");
        assertThat(response.description()).isEqualTo("description");
    }

    @Test
    @DisplayName("updateProject_nonOwner")
    void updateProject_nonOwner() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Project project = project("owner", ownerId, List.of());
        when(projectRepository.findByIdAndDeletedFalse(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.updateProject(project.getId(), userId,
                new UpdateProjectRequest("Updated Name", "new description")))
                .isInstanceOf(UnauthorizedException.class);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("deleteProject_ownerCanDelete")
    void deleteProject_ownerCanDelete() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Project project = project("owner", ownerId, List.of());
        when(projectRepository.findByIdAndDeletedFalse(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenAnswer(invocation -> invocation.getArgument(0));

        projectService.deleteProject(project.getId(), ownerId);

        assertThat(project.isDeleted()).isTrue();
        verify(projectRepository).save(project);
        verify(projectEventPublisher).publishProjectDeleted(any(ProjectDeletedEvent.class));
    }

    @Test
    @DisplayName("addMember_success")
    void addMember_success() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID memberId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Project project = project("owner", ownerId, new ArrayList<>());
        when(projectRepository.findByIdAndDeletedFalse(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.addMember(project.getId(), ownerId, memberId);

        assertThat(response.members()).contains(memberId);
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("addMember_existingMemberDoesNotDuplicate")
    void addMember_existingMemberDoesNotDuplicate() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID memberId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Project project = project("owner", ownerId, new ArrayList<>(List.of(memberId)));
        when(projectRepository.findByIdAndDeletedFalse(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.addMember(project.getId(), ownerId, memberId);

        assertThat(response.members()).containsExactly(memberId);
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("addMember_initializesNullMemberList")
    void addMember_initializesNullMemberList() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID memberId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Project project = project("owner", ownerId, List.of());
        project.setMembers(null);
        when(projectRepository.findByIdAndDeletedFalse(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.addMember(project.getId(), ownerId, memberId);

        assertThat(response.members()).containsExactly(memberId);
        assertThat(project.getMembers()).containsExactly(memberId);
    }

    private Project project(String name, UUID ownerId, List<UUID> members) {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setName(name);
        project.setKey(name.substring(0, Math.min(name.length(), 4)).toUpperCase());
        project.setDescription("description");
        project.setOwnerId(ownerId);
        project.setMembers(new ArrayList<>(members));
        project.setCreatedAt(Instant.now());
        project.setDeleted(false);
        return project;
    }
}
