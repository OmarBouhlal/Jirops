// planning-service/src/main/java/com/jiraclone/planning/repository/SprintTaskRepository.java
package com.jiraclone.planning.repository;

import com.jiraclone.planning.domain.SprintTask;
import com.jiraclone.planning.domain.SprintTaskId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SprintTaskRepository extends JpaRepository<SprintTask, SprintTaskId> {

    List<SprintTask> findByIdSprintId(UUID sprintId);

    boolean existsByIdSprintIdAndIdTaskId(UUID sprintId, UUID taskId);
}
