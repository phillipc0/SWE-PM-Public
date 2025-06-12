package de.telekom.swepm.controller;

import de.telekom.swepm.domain.Project;
import de.telekom.swepm.domain.Role;
import de.telekom.swepm.domain.Task;
import de.telekom.swepm.domain.User;
import de.telekom.swepm.dto.request.CreateTask;
import de.telekom.swepm.dto.request.UpdateTask;
import de.telekom.swepm.dto.response.ReadTask;
import de.telekom.swepm.mapper.ObjectMapper;
import de.telekom.swepm.repos.ProjectRepository;
import de.telekom.swepm.repos.TaskRepository;
import de.telekom.swepm.repos.UserRepository;
import de.telekom.swepm.utils.HtmlEscaper;
import de.telekom.swepm.utils.UserUtils;
import jakarta.validation.Valid;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static de.telekom.swepm.domain.Status.ASSIGNED;
import static de.telekom.swepm.domain.Status.DONE;
import static de.telekom.swepm.domain.Status.TODO;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/projects/{projectId}/tasks", produces = APPLICATION_JSON_VALUE)
public class TaskController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private HtmlEscaper htmlEscaper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReadTask>> getTasks(@PathVariable Integer projectId) {
        val currentProject = getProjectByIdForCurrentUser(projectId);

        List<Task> tasks = taskRepository.findAllByProject(currentProject);

        return ResponseEntity.ok(
            tasks.stream()
                .map(objectMapper::toReadTask)
                .toList()
        );
    }

    @GetMapping("/{taskId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ReadTask> getTask(@PathVariable Integer projectId, @PathVariable Integer taskId) {
        val currentProject = getProjectByIdForCurrentUser(projectId);

        val task = taskRepository.findByIdAndProject(taskId, currentProject)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        return ResponseEntity.ok(objectMapper.toReadTask(task));
    }

    @Transactional
    @PostMapping("")
    public synchronized ResponseEntity<ReadTask> createTask(
        @PathVariable Integer projectId,
        @RequestBody @Valid CreateTask createTask
    ) {
        val sanitizedTitle = htmlEscaper.escapeHTML(createTask.getTitle());
        val sanitizedDescription = htmlEscaper.escapeHTML(createTask.getDescription());

        val currentProject = getProjectByIdForCurrentUser(projectId);

        Set<User> assignedUsers = null;
        if (createTask.getAssignedUsers() != null) {
            assignedUsers = new HashSet<>(userRepository.findAllById(createTask.getAssignedUsers()));
            if (assignedUsers.size() != createTask.getAssignedUsers().size()) {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY);
            }
        }

        Task parentTask = null;
        if (createTask.getParentTask() != null) {
            parentTask = taskRepository.findById(createTask.getParentTask())
                .orElseThrow(() -> new ResponseStatusException(UNPROCESSABLE_ENTITY));

            // Check if the parent task itself is a subtask of another task (it should not be)
            if (parentTask.getParentTask() != null) {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "A subtask cannot have its own subtasks.");
            }
        }

        Set<Task> blockedBy = new HashSet<>();
        if (createTask.getBlockedBy() != null) {
            blockedBy = new HashSet<>(taskRepository.findAllById(createTask.getBlockedBy()));
            if (blockedBy.size() != createTask.getBlockedBy().size()) {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "One or more blocking tasks not found.");
            }
        }

        val newTask = Task.builder()
            .title(sanitizedTitle)
            .description(sanitizedDescription)
            .status(assignedUsers == null || assignedUsers.isEmpty() ? TODO : ASSIGNED)
            .creationDateTime(LocalDateTime.now())
            .dueDate(createTask.getDueDate())
            .project(currentProject)
            .assignedUsers(assignedUsers)
            .parentTask(parentTask)
            .blockedBy(blockedBy)
            .build();

        taskRepository.save(newTask);

        return ResponseEntity.status(CREATED).body(objectMapper.toReadTask(newTask));
    }

    @Transactional
    @PatchMapping("/{taskId}")
    public ResponseEntity<ReadTask> updateTask(
        @PathVariable Integer projectId,
        @PathVariable Integer taskId,
        @RequestBody @Valid UpdateTask updateTask
    ) {
        val sanitizedTitle = htmlEscaper.escapeHTML(updateTask.getTitle());
        val sanitizedDescription = htmlEscaper.escapeHTML(updateTask.getDescription());

        val currentProject = getProjectByIdForCurrentUser(projectId);

        val task = taskRepository.findByIdAndProject(taskId, currentProject)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        if (updateTask.isTitleSet()) {
            task.setTitle(sanitizedTitle);
        }

        if (updateTask.isDescriptionSet()) {
            task.setDescription(sanitizedDescription);
        }

        if (updateTask.isStatusSet()) {
            // Only allow update if a user is assigned (the most complex if statement I have ever written)
            if (updateTask.getStatus() != TODO &&
                ((!updateTask.isAssignedUsersSet() &&
                    (task.getAssignedUsers() == null || task.getAssignedUsers().isEmpty())) ||
                    (updateTask.isAssignedUsersSet() &&
                        (updateTask.getAssignedUsers() == null || updateTask.getAssignedUsers().isEmpty())))) {
                throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY,
                    "Eine Aufgabe kann nicht bearbeitet werden, wenn kein Nutzer zugewiesen ist."
                );
            }
            // Set start time
            if (updateTask.getStatus() != TODO && updateTask.getStatus() != ASSIGNED && task.getStartDateTime() == null) {
                task.setStartDateTime(LocalDateTime.now());
            } else if (updateTask.getStatus() == TODO || updateTask.getStatus() == ASSIGNED) {
                task.setStartDateTime(null);
            }
            // not blocked by check
            if (updateTask.getStatus() != TODO &&
                updateTask.getStatus() != ASSIGNED &&
                !task.getBlockedBy().isEmpty()) {
                throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY, "Eine blockierte Aufgabe kann nicht gestartet werden.");
            }
            if (updateTask.getStatus() == DONE) {
                // Subtasks completed check
                if (!task.getSubtasks().isEmpty()) {
                    task.getSubtasks().forEach(subtask -> {
                        if (subtask.getStatus() != DONE) {
                            throw new ResponseStatusException(
                                UNPROCESSABLE_ENTITY,
                                "Alle Unteraufgaben müssen erledigt sein, damit die Aufgabe abgeschlossen werden kann."
                            );
                        }
                    });
                }
                // Clear blocks list
                task.getBlocks().forEach(blocked -> blocked.getBlockedBy().remove(task));
                task.getBlocks().clear();
                // Set completion time
                task.setCompletionDateTime(LocalDateTime.now());
            } else {
                task.setCompletionDateTime(null);
            }
            // Update task status
            task.setStatus(updateTask.getStatus());
        }

        if (updateTask.isDueDateSet()) {
            task.setDueDate(updateTask.getDueDate());
        }

        if (updateTask.isAssignedUsersSet()) {
            Set<User> assignedUsers = new HashSet<>(userRepository.findAllById(updateTask.getAssignedUsers()));
            if (assignedUsers.size() != updateTask.getAssignedUsers().size()) {
                throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY,
                    "Einer oder mehrere Nutzer konnten nicht gefunden werden."
                );
            }
            if (task.getStatus() == TODO && !updateTask.isStatusSet() && task.getAssignedUsers().isEmpty()) {
                task.setStatus(ASSIGNED);
            }
            task.setAssignedUsers(assignedUsers);
        }

        if (updateTask.isParentTaskSet()) {
            if (updateTask.getParentTask() != null) {
                val parentTask = taskRepository.findById(updateTask.getParentTask())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

                // Check if the parent task is itself a subtask of another task
                if (parentTask.getParentTask() != null) {
                    throw new ResponseStatusException(
                        UNPROCESSABLE_ENTITY,
                        "Eine Unteraufgabe kann nicht ihre eigene Unteraufgabe sein."
                    );
                }

                // Check if the task has subtasks
                if (!task.getSubtasks().isEmpty()) {
                    throw new ResponseStatusException(
                        UNPROCESSABLE_ENTITY,
                        "Eine Aufgabe mit Unteraufgaben kann nicht zur Unteraufgabe gemacht werden."
                    );
                }
                if (parentTask.getStatus() == DONE && task.getStatus() != DONE) {
                    throw new ResponseStatusException(
                        UNPROCESSABLE_ENTITY,
                        "Eine Unteraufgabe die nicht fertig ist kann keiner fertigen Aufgabe zugeordnet werden."
                    );
                }

                task.setParentTask(parentTask);
            } else {
                task.setParentTask(null);
            }
        }

        if (updateTask.isBlockedBySet()) {
            if (updateTask.getBlockedBy().contains(taskId)) {
                throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY,
                    "Eine Aufgabe kann sich nicht selbst blockieren."
                );
            }

            Set<Task> newBlockedBy = new HashSet<>(taskRepository.findAllById(updateTask.getBlockedBy()));

            for (val blockingTask : newBlockedBy) {
                if (blockingTask.hasCircularDependency(task)) {
                    throw new ResponseStatusException(
                        UNPROCESSABLE_ENTITY,
                        "Aufgaben dürfen keine Kreisabhängigkeit untereinander haben."
                    );
                }
                if (blockingTask.getStatus() == DONE) {
                    throw new ResponseStatusException(
                        UNPROCESSABLE_ENTITY,
                        "Eine Aufgabe kann nicht durch eine fertige Aufgabe blockiert werden."
                    );
                }
                // If a task is already being done, it will be set back to assigned if its blocked
                if (task.getStatus() != TODO) {
                    task.setStatus(ASSIGNED);
                }
            }

            task.setBlockedBy(newBlockedBy.isEmpty() ? new HashSet<>() : newBlockedBy);
        }

        if (updateTask.isSubTasksSet()) {
            Set<Task> newSubtasks = new HashSet<>(taskRepository.findAllById(updateTask.getSubTasks()));
            if (newSubtasks.size() != updateTask.getSubTasks().size()) {
                throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY,
                    "Eine oder mehrere Unteraufgaben können nicht gefunden werden."
                );
            }
            if (newSubtasks.contains(task)) {
                throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY,
                    "Eine Unteraufgabe kann nicht ihre eigene Unteraufgabe sein."
                );
            }
            if (newSubtasks.contains(task.getParentTask())) {
                throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY,
                    "Eine Aufgabe kann nicht die Unteraufgabe ihrer Überaufgabe sein."
                );
            }
            if (newSubtasks.stream().anyMatch(subtask -> subtask.hasCircularDependency(task))) {
                throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY,
                    "Aufgaben dürfen keine Kreisabhängigkeit untereinander haben."
                );
            }
            if (!newSubtasks.isEmpty() && task.getParentTask() != null) {
                throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY,
                    "Aufgaben dürfen keine Kreisabhängigkeit untereinander haben."
                );
            }
            if (task.getStatus() == DONE && newSubtasks.stream().anyMatch(anyTask -> anyTask.getStatus() != DONE)) {
                throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY,
                    "Eine Unteraufgabe die nicht fertig ist kann keiner fertigen Aufgabe zugeordnet werden."
                );
            }

            // Identify subtasks to remove
            Set<Task> subtasksToRemove = new HashSet<>(task.getSubtasks());
            subtasksToRemove.removeAll(newSubtasks);

            // Identify subtasks to add
            Set<Task> subtasksToAdd = new HashSet<>(newSubtasks);
            subtasksToAdd.removeAll(task.getSubtasks());

            // Remove orphan subtasks
            for (Task subtask : subtasksToRemove) {
                subtask.setParentTask(null);
                task.getSubtasks().remove(subtask);
            }

            // Add new subtasks and set their parentTask
            for (Task subtask : subtasksToAdd) {
                subtask.setParentTask(task);
                task.getSubtasks().add(subtask);
            }
        }

        taskRepository.save(task);

        return ResponseEntity.ok(objectMapper.toReadTask(task));
    }

    @Transactional
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Integer projectId, @PathVariable Integer taskId) {
        val currentProject = getProjectByIdForCurrentUser(projectId);

        val task = taskRepository.findByIdAndProject(taskId, currentProject)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        taskRepository.delete(task);

        return ResponseEntity.noContent().build();
    }

    private Project getProjectByIdForCurrentUser(Integer projectId) {
        val sessionUser = userUtils.getSessionUser();

        return sessionUser.getRole() == Role.PROJECT_MANAGER ?
            projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)) :
            projectRepository.findByIdAndUsersContains(projectId, sessionUser)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }
}
