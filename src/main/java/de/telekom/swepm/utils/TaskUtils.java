package de.telekom.swepm.utils;

import de.telekom.swepm.domain.Task;
import de.telekom.swepm.repos.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskUtils {
    @Autowired
    private TaskRepository taskRepository;

    public Task saveIfNotExists(Task task) {
        return taskRepository.findByProjectAndTitle(task.getProject(), task.getTitle())
            .orElseGet(() -> taskRepository.save(task));
    }
}
