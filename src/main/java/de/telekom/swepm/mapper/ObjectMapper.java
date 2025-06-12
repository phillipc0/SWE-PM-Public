package de.telekom.swepm.mapper;

import de.telekom.swepm.domain.Project;
import de.telekom.swepm.domain.Status;
import de.telekom.swepm.domain.Task;
import de.telekom.swepm.domain.User;
import de.telekom.swepm.dto.response.ReadProject;
import de.telekom.swepm.dto.response.ReadTask;
import de.telekom.swepm.dto.response.ReadUser;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ObjectMapper {

    public ReadProject toReadProject(Project project) {
        // Initialize the map with all statuses set to zero
        Map<Status, Integer> taskStatusMap = new EnumMap<>(Status.class);
        for (Status status : Status.values()) {
            taskStatusMap.put(status, 0);
        }

        // Update the map with actual task counts
        taskStatusMap.putAll(project.getTasks().stream()
            .collect(Collectors.groupingBy(Task::getStatus, Collectors.summingInt(e -> 1))));

        return ReadProject.builder()
            .id(project.getId())
            .name(project.getName())
            .description(project.getDescription())
            .createdOn(project.getCreatedOn())
            .projectManager(Optional.ofNullable(project.getProjectManager())
                .map(this::toReadUser)
                .orElse(null))
            .users(Optional.ofNullable(project.getUsers())
                .map(users -> users.stream()
                    .map(this::toReadUser)
                    .toList())
                .orElse(null))
            .taskStatusCount(taskStatusMap)
            .build();
    }

    public ReadUser toReadUser(User user) {
        return ReadUser.builder()
            .id(user.getId())
            .name(user.getName())
            .emailAddress(user.getEmailAddress())
            .role(user.getRole())
            .isNewUser(user.isNewUser())
            .projectIds(
                Optional.ofNullable(user.getProjects())
                    .map(projects -> projects.stream()
                        .map(Project::getId)
                        .toList())
                    .orElse(null))
            .build();
    }

    public ReadTask toReadTask(Task task) {
        return ReadTask.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .status(task.getStatus())
            .creationDateTime(task.getCreationDateTime())
            .dueDate(task.getDueDate())
            .startDateTime(task.getStartDateTime())
            .completionDateTime(task.getCompletionDateTime())
            .project(task.getProject().getId())
            .assignedUsers(Optional.ofNullable(task.getAssignedUsers())
                .map(users -> users.stream().map(User::getId).toList())
                .orElse(List.of()))
            .parentTask(task.getParentTask() != null ? task.getParentTask().getId() : null)
            .subTasks(Optional.ofNullable(task.getSubtasks())
                .map(subtasks -> subtasks.stream().map(Task::getId).toList())
                .orElse(List.of()))
            .blocks(Optional.ofNullable(task.getBlocks())
                .map(blocks -> blocks.stream().map(Task::getId).toList())
                .orElse(List.of()))
            .blockedBy(Optional.ofNullable(task.getBlockedBy())
                .map(blockedBy -> blockedBy.stream().map(Task::getId).toList())
                .orElse(List.of()))
            .build();
    }
}
