// planning-service/src/main/java/com/jiraclone/planning/repository/SprintRepository.java
package com.jiraclone.planning.repository;

import com.jiraclone.planning.domain.Sprint;
import com.jiraclone.planning.domain.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {

    List<Sprint> findByProjectId(UUID projectId);

    Optional<Sprint> findByProjectIdAndStatus(UUID projectId, SprintStatus status);

    Optional<Sprint> findByIdAndProjectId(UUID id, UUID projectId);
}
