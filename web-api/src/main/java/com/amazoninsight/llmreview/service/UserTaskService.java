package com.amazoninsight.llmreview.service;

import com.amazoninsight.llmreview.model.UserTask;
import com.amazoninsight.llmreview.repository.UserTaskBlockingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserTaskService {

    private final UserTaskBlockingRepository userTaskBlockingRepository;

    @Autowired
    public UserTaskService(UserTaskBlockingRepository userTaskBlockingRepository) {
        this.userTaskBlockingRepository = userTaskBlockingRepository;
    }

    public void saveUserTask(String username, String taskId, String asin) {
        UserTask userTask = new UserTask();
        userTask.setUsername(username);
        userTask.setTaskId(taskId);
        userTask.setAsin(asin);
        userTask.setStartDate(LocalDateTime.now());
        userTaskBlockingRepository.save(userTask);
    }
}