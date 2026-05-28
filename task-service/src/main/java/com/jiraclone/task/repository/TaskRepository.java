package com.jiraclone.task.repository;

import com.jiraclone.task.domain.TaskDocument;
import com.jiraclone.task.domain.TaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<TaskDocument, String> {

    List<TaskDocument> findByProjectId(String projectId);

    List<TaskDocument> findBySprintIdAndStatusNot(String sprintId, TaskStatus status);

    long deleteByProjectId(String projectId);
}
