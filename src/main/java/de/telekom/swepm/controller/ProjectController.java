package de.telekom.swepm.controller;

import de.telekom.swepm.domain.Project;
import de.telekom.swepm.dto.request.CreateProject;
import de.telekom.swepm.dto.request.UpdateProject;
import de.telekom.swepm.dto.response.ReadProject;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static de.telekom.swepm.domain.Role.PROJECT_MANAGER;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/projects", produces = APPLICATION_JSON_VALUE)
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HtmlEscaper htmlEscaper;
    @Autowired
    private TaskRepository taskRepository;

    // Get all projects basic information
    @Transactional(readOnly = true)
    @GetMapping("")
    public ResponseEntity<List<ReadProject>> getProjects() {
        val sessionUser = userUtils.getSessionUser();

        List<Project> projects = sessionUser.getRole() == PROJECT_MANAGER
            ? projectRepository.findAll() //PROJECT_MANAGER can get all projects
            : projectRepository.findAllByUsersContains(sessionUser); //EMPLOYEE can get only the projects they are in

        return ResponseEntity.ok(
            projects.stream()
                .map(objectMapper::toReadProject)
                .toList()
        );
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public ResponseEntity<ReadProject> getProject(@PathVariable Integer id) {
        val sessionUser = userUtils.getSessionUser();

        val project = sessionUser.getRole() == PROJECT_MANAGER
            ? projectRepository.findById(id) //PROJECT_MANAGER can get any project
            : projectRepository
            .findByIdAndUsersContains(id, sessionUser); //EMPLOYEE can only get a project if they are assigned to it
        val readProject = objectMapper.toReadProject(project
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));

        return ResponseEntity.ok(readProject);
    }

    @PostMapping("")
    @Transactional
    @PreAuthorize("hasAuthority('PROJECT_MANAGER')")
    public synchronized ResponseEntity<ReadProject> createProject(@RequestBody @Valid CreateProject createProject) {
        val sanitizedName = htmlEscaper.escapeHTML(createProject.getName());
        val sanitizedDescription = htmlEscaper.escapeHTML(createProject.getDescription());
        projectRepository.findByName(sanitizedName)
            .ifPresent(ignored -> {
                throw new ResponseStatusException(CONFLICT);
            });

        val manager = userRepository.findByIdAndRole(createProject.getManager(), PROJECT_MANAGER)
            .orElseThrow(() -> new ResponseStatusException(UNPROCESSABLE_ENTITY));

        val users = new ArrayList<>(userRepository.findAllById(createProject.getUsers()));
        if (users.size() != createProject.getUsers().size())
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY);

        if (!users.contains(manager))
            users.add(manager);

        val newProject = Project.builder()
            .name(sanitizedName)
            .description(sanitizedDescription)
            .createdOn(LocalDateTime.now())
            .projectManager(manager)
            .users(new HashSet<>(users))
            .build();

        projectRepository.save(newProject);

        return ResponseEntity.status(CREATED).body(objectMapper.toReadProject(newProject));
    }

    @PatchMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('PROJECT_MANAGER')")
    public ResponseEntity<ReadProject> updateProject(
        @PathVariable Integer id,
        @RequestBody @Valid UpdateProject updateProject
    ) {
        val sanitizedName = htmlEscaper.escapeHTML(updateProject.getName());
        val sanitizedDescription = htmlEscaper.escapeHTML(updateProject.getDescription());

        val project = projectRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        if (updateProject.getManager() != null) {
            val newManager = userRepository.findByIdAndRole(updateProject.getManager(), PROJECT_MANAGER)
                .orElseThrow(() -> new ResponseStatusException(UNPROCESSABLE_ENTITY));
            project.setProjectManager(newManager);
        }

        if (updateProject.getUsers() != null) {
            val newUsers = userRepository.findAllById(updateProject.getUsers());
            if (newUsers.size() != updateProject.getUsers().size()) {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY);
            }

            // Identify users being removed
            val currentUsers = project.getUsers();
            val usersToRemove = new HashSet<>(currentUsers);
            newUsers.forEach(usersToRemove::remove);

            // Remove users from tasks if they are removed from the project
            if (!usersToRemove.isEmpty()) {
                val projectTasks = taskRepository.findAllByProject(project);
                for (val task : projectTasks) {
                    task.getAssignedUsers().removeAll(usersToRemove);
                    taskRepository.save(task); // Save the updated task
                }
            }

            project.setUsers(new HashSet<>(newUsers));
        }

        if (updateProject.getName() != null) {
            if (sanitizedName != null && !sanitizedName.isBlank()) {
                project.setName(sanitizedName);
            } else {
                throw new ResponseStatusException(BAD_REQUEST);
            }
        }

        if (updateProject.getDescription() != null)
            project.setDescription(sanitizedDescription);

        projectRepository.save(project);

        return ResponseEntity.ok(objectMapper.toReadProject(project));
    }

    @Transactional
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER')")
    public ResponseEntity<Void> deleteProject(@PathVariable Integer id) {
        val project = projectRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        projectRepository.delete(project);

        return ResponseEntity.noContent().build();
    }
}
