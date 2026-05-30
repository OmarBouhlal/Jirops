// planning-service/src/main/java/com/jiraclone/planning/domain/SprintTaskId.java
package com.jiraclone.planning.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class SprintTaskId implements Serializable {

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    public SprintTaskId() {
    }

    public SprintTaskId(UUID sprintId, UUID taskId) {
        this.sprintId = sprintId;
        this.taskId = taskId;
    }

    public UUID getSprintId() {
        return sprintId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setSprintId(UUID sprintId) {
        this.sprintId = sprintId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SprintTaskId that = (SprintTaskId) o;
        return java.util.Objects.equals(sprintId, that.sprintId)
                && java.util.Objects.equals(taskId, that.taskId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(sprintId, taskId);
    }
}
