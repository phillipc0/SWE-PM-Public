package de.telekom.swepm;

import de.telekom.swepm.controller.TaskController;
import de.telekom.swepm.domain.Project;
import de.telekom.swepm.domain.Task;
import de.telekom.swepm.domain.User;
import de.telekom.swepm.dto.request.CreateTask;
import de.telekom.swepm.dto.request.UpdateTask;
import de.telekom.swepm.repos.ProjectRepository;
import de.telekom.swepm.repos.TaskRepository;
import de.telekom.swepm.repos.UserRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static de.telekom.swepm.domain.Status.ASSIGNED;
import static de.telekom.swepm.domain.Status.DONE;
import static de.telekom.swepm.domain.Status.IN_PROGRESS;
import static de.telekom.swepm.domain.Status.TODO;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskController taskController;

    @Autowired
    private TestSetup testSetup;

    @Autowired
    private TestUtils testUtils;

    private List<Project> projects;
    private List<Task> tasks;
    private List<User> users;

    @BeforeEach
    void setup() {
        this.reset();
        testSetup.setup();
        this.projects = testSetup.getProjects();
        this.tasks = testSetup.getTasks();
        this.users = testSetup.getUsers();
    }

    @AfterEach
    void reset() {
        log.info("Deleting test db");
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void getTasksTest() {
        val response = taskController.getTasks(projects.get(0).getId());
        val responseBody = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode(), is(OK));
        assertThat(
            responseBody.get(0).getAssignedUsers().toString(),
            not(blankOrNullString())
        );
        assertThat(
            responseBody.get(0).getProject(),
            is(projects.get(0).getId())
        );
        assertThat(
            responseBody.get(0).getDescription(),
            is("Description 1")
        );

        assertThat(
            responseBody.get(0).getTitle(),
            is("Task 1")
        );

        assertThat(
            responseBody.get(0).getStatus(),
            is(TODO)
        );

        assertThat(
            responseBody.get(3).getParentTask(),
            is(tasks.get(0).getId())
        );

        assertThat(
            responseBody.get(2).getParentTask(),
            is(tasks.get(0).getId())
        );

        assertThat(
            responseBody.get(0).getSubTasks(),
            containsInAnyOrder(tasks.get(2).getId(), tasks.get(3).getId())
        );

        assertThat(
            responseBody.get(0).getBlocks().get(0),
            is(responseBody.get(1).getId())
        );

        assertThat(
            responseBody.get(1).getBlockedBy().get(0),
            is(responseBody.get(0).getId())
        );
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void getTasksErrorTest() throws Exception {
        val invalidProjectId = Integer.MAX_VALUE;
        testUtils.getRequest("/api/v1/projects/" + invalidProjectId + "/tasks")
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "max.mustermann@mail.de", authorities = "EMPLOYEE")
    void getTaskNotFoundTest() throws Exception {
        val projectIdWhereUserIsNotAssigned = projects.get(1).getId();
        val validTaskIdInAnotherProject = tasks.get(0).getId();
        testUtils.getRequest(
                "/api/v1/projects/" + projectIdWhereUserIsNotAssigned + "/tasks/" + validTaskIdInAnotherProject)
            .andExpect(status().isNotFound());

        val invalidProjectId = Integer.MAX_VALUE;
        val validTaskId = tasks.get(0).getId();

        testUtils.getRequest("/api/v1/projects/" + invalidProjectId + "/tasks/" + validTaskId)
            .andExpect(status().isNotFound());

        val validProjectIdWithoutThisTask = projects.get(1).getId();

        testUtils.getRequest("/api/v1/projects/" + validProjectIdWithoutThisTask + "/tasks/" + validTaskId)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void getTaskTest() {
        val response = taskController.getTask(projects.get(0).getId(), tasks.get(0).getId());
        val responseBody = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode(), is(OK));
        assertThat(
            responseBody.getAssignedUsers().toString(),
            not(blankOrNullString())
        );
        assertThat(
            responseBody.getProject(),
            is(projects.get(0).getId())
        );
        assertThat(
            responseBody.getDescription(),
            is("Description 1")
        );
        assertThat(
            responseBody.getTitle(),
            is("Task 1")
        );
        assertThat(
            responseBody.getStatus(),
            is(TODO)
        );

        assertThat(
            responseBody.getParentTask(),
            is(nullValue())
        );

        assertThat(
            responseBody.getSubTasks(),
            containsInAnyOrder(tasks.get(2).getId(), tasks.get(3).getId())
        );
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void getTaskErrorTest() throws Exception {
        val invalidProjectId = Integer.MAX_VALUE;
        val invalidTaskId = Integer.MAX_VALUE;
        testUtils.getRequest("/api/v1/projects/" + invalidProjectId + "/tasks/" + invalidTaskId)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void createTaskTest() {
        val createTaskResponse = taskController.createTask(
            projects.get(0).getId(),
            CreateTask.builder()
                .title("New Task")
                .description("Task Description")
                .dueDate(LocalDate.now().plusDays(7))
                .assignedUsers(Set.of(getUser(0).getId(), getUser(1).getId()))
                .build()
        );

        assertThat(createTaskResponse.getStatusCode(), is(CREATED));
        assertThat(createTaskResponse.getBody(), notNullValue());
        assertThat(createTaskResponse.getBody().getTitle(), is("New Task"));
        assertThat(
            taskRepository.findById(Objects.requireNonNull(createTaskResponse.getBody()).getId()).isPresent(),
            is(true)
        );
        assertThat(createTaskResponse.getBody().getAssignedUsers(), hasSize(2));
        assertThat(
            createTaskResponse.getBody().getAssignedUsers(),
            containsInAnyOrder(getUser(0).getId(), getUser(1).getId())
        );
    }

    @Test
    @Transactional
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void updateTaskTest() {
        val taskId = createTask();

        updateTitleAndMoveInProgress(taskId);
        moveTaskToDone(taskId);
        moveTaskBackToInProgress(taskId);
        updateDueDate(taskId);
        updateAssignedUsers(taskId);
        updateParentTask(taskId);
        updateBlockedBy(taskId);
        moveTaskToTodo(taskId);
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void deleteTaskTest() {
        val createTaskResponse = taskController.createTask(
            projects.get(0).getId(),
            CreateTask.builder()
                .title("Task to Delete")
                .description("Task Description")
                .dueDate(LocalDate.now().plusDays(7))
                .assignedUsers(Set.of(getUser(0).getId(), getUser(1).getId()))
                .build()
        );

        val taskId = Objects.requireNonNull(createTaskResponse.getBody()).getId();
        assertThat(taskRepository.findById(taskId).isPresent(), is(true));

        val deleteTaskResponse = taskController.deleteTask(projects.get(0).getId(), taskId);

        assertThat(deleteTaskResponse.getStatusCode(), is(NO_CONTENT));
        assertThat(taskRepository.findById(taskId).isPresent(), is(false));
    }

    private int createTask() {
        val createTaskResponse = taskController.createTask(
            projects.get(0).getId(),
            CreateTask.builder()
                .title("New Task")
                .description("Task Description")
                .dueDate(LocalDate.now().plusDays(7))
                .assignedUsers(Set.of(getUser(0).getId(), getUser(1).getId()))
                .build()
        );

        val taskId = Objects.requireNonNull(createTaskResponse.getBody()).getId();

        assertThat(taskRepository.findById(taskId).isPresent(), is(true));
        assertThat(taskRepository.findById(taskId).get().getTitle(), is("New Task"));
        assertThat(taskRepository.findById(taskId).get().getStatus(), is(ASSIGNED));
        assertThat(taskRepository.findById(taskId).get().getStartDateTime(), nullValue());
        assertThat(taskRepository.findById(taskId).get().getCompletionDateTime(), nullValue());
        return taskId;
    }

    private LocalDateTime updateTitleAndMoveInProgress(int taskId) {
        val updateTask = new UpdateTask();
        updateTask.setTitle("Updated Task Title");
        updateTask.setStatus(IN_PROGRESS);
        val updateTaskResponse = taskController.updateTask(
            projects.get(0).getId(),
            taskId,
            updateTask
        );

        val startDateTime = taskRepository.findById(taskId).get().getStartDateTime();
        assertThat(updateTaskResponse.getStatusCode(), is(OK));
        assertThat(updateTaskResponse.getBody(), notNullValue());
        assertThat(updateTaskResponse.getBody().getTitle(), is("Updated Task Title"));
        assertThat(taskRepository.findById(taskId).get().getTitle(), is("Updated Task Title"));
        assertThat(updateTaskResponse.getBody().getStatus(), is(IN_PROGRESS));
        assertThat(taskRepository.findById(taskId).get().getStatus(), is(IN_PROGRESS));
        assertEquals(
            updateTaskResponse.getBody().getStartDateTime().truncatedTo(MILLIS),
            startDateTime.truncatedTo(MILLIS)
        );
        assertThat(startDateTime, notNullValue());
        assertThat(updateTaskResponse.getBody().getCompletionDateTime(), nullValue());
        assertThat(taskRepository.findById(taskId).get().getCompletionDateTime(), nullValue());
        return startDateTime;
    }

    private void moveTaskToDone(int taskId) {
        val updateTaskDone = new UpdateTask();
        updateTaskDone.setStatus(DONE);
        val updateTaskDoneResponse = taskController.updateTask(
            projects.get(0).getId(),
            taskId,
            updateTaskDone
        );

        assertThat(updateTaskDoneResponse.getStatusCode(), is(OK));
        assertThat(updateTaskDoneResponse.getBody(), notNullValue());
        assertThat(updateTaskDoneResponse.getBody().getStatus(), is(DONE));
        assertThat(taskRepository.findById(taskId).get().getStatus(), is(DONE));
        assertThat(updateTaskDoneResponse.getBody().getCompletionDateTime(), notNullValue());
        assertThat(taskRepository.findById(taskId).get().getCompletionDateTime(), notNullValue());
    }

    private void moveTaskBackToInProgress(int taskId) {
        val updateTaskInProgress = new UpdateTask();
        updateTaskInProgress.setStatus(IN_PROGRESS);
        val updateTaskInProgressResponse = taskController.updateTask(
            projects.get(0).getId(),
            taskId,
            updateTaskInProgress
        );

        assertThat(updateTaskInProgressResponse.getStatusCode(), is(OK));
        assertThat(updateTaskInProgressResponse.getBody(), notNullValue());
        assertThat(updateTaskInProgressResponse.getBody().getStatus(), is(IN_PROGRESS));
        assertThat(taskRepository.findById(taskId).get().getStatus(), is(IN_PROGRESS));
        assertThat(updateTaskInProgressResponse.getBody().getCompletionDateTime(), nullValue());
        assertThat(taskRepository.findById(taskId).get().getCompletionDateTime(), nullValue());
        assertThat(updateTaskInProgressResponse.getBody().getStartDateTime(), notNullValue());
        assertThat(taskRepository.findById(taskId).get().getStartDateTime(), notNullValue());
    }

    private void updateDueDate(int taskId) {
        val newDueDate = LocalDate.now().plusDays(10);
        val updateTaskDueDate = new UpdateTask();
        updateTaskDueDate.setDueDate(newDueDate);
        val updateTaskDueDateResponse = taskController.updateTask(
            projects.get(0).getId(),
            taskId,
            updateTaskDueDate
        );

        assertThat(updateTaskDueDateResponse.getStatusCode(), is(OK));
        assertThat(updateTaskDueDateResponse.getBody(), notNullValue());
        assertThat(updateTaskDueDateResponse.getBody().getDueDate(), is(newDueDate));
        assertThat(
            taskRepository.findById(taskId).get().getDueDate(),
            is(newDueDate)
        );
    }

    private void updateAssignedUsers(int taskId) {
        val newAssignedUsers = Set.of(getUser(2).getId());
        val updateTaskAssignedUsers = new UpdateTask();
        updateTaskAssignedUsers.setAssignedUsers(newAssignedUsers);
        val updateTaskAssignedUsersResponse = taskController.updateTask(
            projects.get(0).getId(),
            taskId,
            updateTaskAssignedUsers
        );

        assertThat(updateTaskAssignedUsersResponse.getStatusCode(), is(OK));
        assertThat(updateTaskAssignedUsersResponse.getBody(), notNullValue());
        assertThat(
            updateTaskAssignedUsersResponse.getBody().getAssignedUsers(),
            containsInAnyOrder(newAssignedUsers.toArray())
        );

        assertThat(taskRepository.findById(taskId)
            .get()
            .getAssignedUsers()
            .stream()
            .map(User::getId)
            .collect(Collectors.toSet()), containsInAnyOrder(newAssignedUsers.toArray()));
    }

    private void updateParentTask(int taskId) {
        val newParentTaskId = taskRepository.findAll()
            .stream()
            .filter(t -> t.getId() != taskId)
            .findFirst()
            .get()
            .getId();
        val updateTaskParentTask = new UpdateTask();
        updateTaskParentTask.setParentTask(newParentTaskId);
        val updateTaskParentTaskResponse = taskController.updateTask(
            projects.get(0).getId(),
            taskId,
            updateTaskParentTask
        );

        assertThat(updateTaskParentTaskResponse.getStatusCode(), is(OK));
        assertThat(updateTaskParentTaskResponse.getBody(), notNullValue());
        assertThat(updateTaskParentTaskResponse.getBody().getParentTask(), is(newParentTaskId));
        assertThat(taskRepository.findById(taskId).get().getParentTask().getId(), is(newParentTaskId));
    }

    private void updateBlockedBy(int taskId) {
        val newBlockedByTaskIds = Set.of(tasks.get(3).getId());
        val updateTaskBlockedBy = new UpdateTask();
        updateTaskBlockedBy.setBlockedBy(newBlockedByTaskIds);
        val updateTaskBlockedByResponse = taskController.updateTask(
            projects.get(0).getId(),
            taskId,
            updateTaskBlockedBy
        );

        assertThat(updateTaskBlockedByResponse.getStatusCode(), is(OK));
        assertThat(updateTaskBlockedByResponse.getBody(), notNullValue());
        assertThat(
            updateTaskBlockedByResponse.getBody().getBlockedBy(),
            containsInAnyOrder(newBlockedByTaskIds.toArray())
        );
    }

    private void moveTaskToTodo(int taskId) {
        val updateTaskDone = new UpdateTask();
        updateTaskDone.setStatus(TODO);
        val updateTaskDoneResponse = taskController.updateTask(
            projects.get(0).getId(),
            taskId,
            updateTaskDone
        );

        assertThat(updateTaskDoneResponse.getStatusCode(), is(OK));
        assertThat(updateTaskDoneResponse.getBody(), notNullValue());
        assertThat(updateTaskDoneResponse.getBody().getStatus(), is(TODO));
        assertThat(taskRepository.findById(taskId).get().getStatus(), is(TODO));
        assertThat(updateTaskDoneResponse.getBody().getCompletionDateTime(), nullValue());
        assertThat(taskRepository.findById(taskId).get().getCompletionDateTime(), nullValue());
        assertThat(updateTaskDoneResponse.getBody().getStartDateTime(), nullValue());
        assertThat(taskRepository.findById(taskId).get().getStartDateTime(), nullValue());
    }

    private User getUser(int num) {
        return userRepository.findById(users.get(num).getId()).orElse(null);
    }
}