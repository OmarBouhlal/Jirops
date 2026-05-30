// planning-service/src/main/java/com/jiraclone/planning/domain/SprintTask.java
package com.jiraclone.planning.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "sprint_tasks")
public class SprintTask {

    @EmbeddedId
    private SprintTaskId id;

    public SprintTask() {
    }

    public SprintTask(UUID sprintId, UUID taskId) {
        this.id = new SprintTaskId(sprintId, taskId);
    }

    public SprintTaskId getId() {
        return id;
    }

    public void setId(SprintTaskId id) {
        this.id = id;
    }
}
