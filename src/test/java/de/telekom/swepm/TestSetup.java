package de.telekom.swepm;

import de.telekom.swepm.domain.Project;
import de.telekom.swepm.domain.Task;
import de.telekom.swepm.domain.User;
import de.telekom.swepm.utils.ProjectUtils;
import de.telekom.swepm.utils.TaskUtils;
import de.telekom.swepm.utils.UserUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static de.telekom.swepm.domain.Role.EMPLOYEE;
import static de.telekom.swepm.domain.Role.PROJECT_MANAGER;
import static de.telekom.swepm.domain.Status.IN_PROGRESS;
import static de.telekom.swepm.domain.Status.TODO;

@Service
public class TestSetup {

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProjectUtils projectUtils;

    @Autowired
    private TaskUtils taskUtils;

    @Getter
    private List<User> users;
    @Getter
    private List<Project> projects;
    @Getter
    private List<Task> tasks;

    /**
     * Important Note: Changing anything in the InitialSetup, will change the behavior of the tests.
     **/

    @PostConstruct
    public void setup() {
        this.tasks = new ArrayList<>();
        this.users = new ArrayList<>();
        this.projects = new ArrayList<>();
        createAdmin();
        generateUsers();
        generateProjects();
        generateTasks();
    }

    private void createAdmin() {
        val user0 = userUtils.saveIfNotExists(
            User.builder()
                .name("Projio Admin")
                .emailAddress("projio")
                .role(PROJECT_MANAGER)
                .passwordHash(passwordEncoder.encode("projio"))
                .isNewUser(false)
                .build());

        this.users.add(user0);
    }

    void generateTasks() {
        Project project = projects.get(0);

        Task task1 = taskUtils.saveIfNotExists(Task.builder()
            .title("Task 1")
            .project(project)
            .description("Description 1")
            .status(TODO)
            .dueDate(LocalDate.now().plusDays(69))
            .creationDateTime(LocalDateTime.now())
            .assignedUsers(Set.of(users.get(1), users.get(2)))
            .dueDate(LocalDate.now().plusDays(10))
            .build());

        Task task2 = taskUtils.saveIfNotExists(Task.builder()
            .title("Task 2")
            .blockedBy(Set.of(task1))
            .project(project)
            .description("Description 2")
            .status(IN_PROGRESS)
            .dueDate(LocalDate.now().plusDays(69))
            .creationDateTime(LocalDateTime.now())
            .build());

        Task subTask1 = taskUtils.saveIfNotExists(Task.builder()
            .title("SubTask 1")
            .project(project)
            .parentTask(task1)
            .description("Subtask Description 1")
            .status(TODO)
            .dueDate(LocalDate.now().plusDays(42))
            .creationDateTime(LocalDateTime.now())
            .assignedUsers(Set.of(users.get(1)))
            .build());

        Task subTask2 = taskUtils.saveIfNotExists(Task.builder()
            .title("SubTask 2")
            .project(project)
            .parentTask(task1)
            .description("Subtask Description 2")
            .status(IN_PROGRESS)
            .dueDate(LocalDate.now().plusDays(42))
            .creationDateTime(LocalDateTime.now())
            .build());

        this.tasks.addAll(List.of(task1, task2, subTask1, subTask2));
    }

    private void generateUsers() {
        val user1 = userUtils.saveIfNotExists(
            User.builder()
                .name("Max Mustermann")
                .emailAddress("max.mustermann@mail.de")
                .role(EMPLOYEE)
                .passwordHash(passwordEncoder.encode("max"))
                .isNewUser(true)
                .build());

        val user2 = userUtils.saveIfNotExists(
            User.builder()
                .name("Erika Mustermann")
                .emailAddress("erika.mustermann@mail.de")
                .role(PROJECT_MANAGER)
                .passwordHash(passwordEncoder.encode("erika"))
                .isNewUser(false)
                .build());

        val user3 = userUtils.saveIfNotExists(
            User.builder()
                .name("Otto Normalverbraucher")
                .emailAddress("otto.normalverbraucher@mail.de")
                .role(PROJECT_MANAGER)
                .passwordHash(passwordEncoder.encode("OTTOnormalverbraucher123!"))
                .isNewUser(true)
                .build());

        this.users.addAll(List.of(user1, user2, user3));
    }

    private void generateProjects() {
        val project1 = projectUtils.saveIfNotExists(Project.builder()
            .id(1)
            .name("Project 1")
            .description("Description 1")
            .createdOn(LocalDateTime.now())
            .projectManager(users.get(2))
            .users(Set.of(users.get(1), users.get(2)))
            .build());

        val project2 = projectUtils.saveIfNotExists(Project.builder()
            .id(2)
            .name("Project 2")
            .description("Description 2")
            .createdOn(LocalDateTime.now())
            .projectManager(users.get(2))
            .users(Set.of(users.get(2)))
            .build());

        this.projects.addAll(List.of(project1, project2));
    }
}
