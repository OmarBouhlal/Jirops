// auth-service/src/main/java/com/jiraclone/auth/repository/UserRepository.java
package com.jiraclone.auth.repository;

import com.jiraclone.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
}
