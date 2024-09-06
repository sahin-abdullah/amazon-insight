package com.amazoninsight.llmreview.repository;

import com.amazoninsight.llmreview.model.UserTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTaskBlockingRepository extends JpaRepository<UserTask, Long> {
    Optional<UserTask> findByUsernameAndTaskId(String username, String taskId);
    Optional<UserTask> findByTaskId(String taskId);
    Optional<UserTask> findAsinByTaskId(String taskId);
}