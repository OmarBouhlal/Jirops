// project-service/src/main/java/com/jiraclone/project/repository/ProjectRepository.java
package com.jiraclone.project.repository;

import com.jiraclone.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdAndDeletedFalse(UUID id);

    boolean existsByKey(String key);

    @Query("""
            select distinct p
            from Project p
            left join p.members m
            where p.deleted = false
              and (p.ownerId = :ownerId or m = :memberId)
            """)
    List<Project> findByOwnerIdOrMembersContainingAndDeletedFalse(@Param("ownerId") UUID ownerId,
                                                                  @Param("memberId") UUID memberId);
}
