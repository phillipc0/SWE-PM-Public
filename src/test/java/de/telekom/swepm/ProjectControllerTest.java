package de.telekom.swepm;

import de.telekom.swepm.controller.ProjectController;
import de.telekom.swepm.domain.Project;
import de.telekom.swepm.domain.User;
import de.telekom.swepm.dto.request.CreateProject;
import de.telekom.swepm.dto.request.UpdateProject;
import de.telekom.swepm.repos.ProjectRepository;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectController projectController;

    @Autowired
    private TestSetup testSetup;

    @Autowired
    private TestUtils testUtils;

    private List<Project> projects;
    private List<User> users;

    @BeforeEach
    void setup() {
        this.reset();
        testSetup.setup();
        this.projects = testSetup.getProjects();
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
    void getAllProjectsTest() {
        val response = projectController.getProjects();
        assertThat(response.getStatusCode(), is(OK));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody().size(), is(2));
        assertThat(response.getBody().get(1).getName(), is(projects.get(1).getName()));
    }

    @Test
    @WithMockUser(username = "max.mustermann@mail.de", authorities = "EMPLOYEE")
    void getProjectTest() {
        val response = projectController.getProject(projects.get(0).getId());
        assertThat(response.getStatusCode(), is(OK));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody().getName(), is(projects.get(0).getName()));
    }

    @Test
    @WithMockUser(username = "max.mustermann@mail.de", authorities = "EMPLOYEE")
    void projectControllerNotFoundTest() throws Exception {
        val response = projectController.getProjects();
        assertThat(response.getStatusCode(), is(OK));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody(), hasSize(1));
        assertThat(response.getBody().get(0).getName(), is(projects.get(0).getName()));

        testUtils.getRequest("/api/v1/projects/" + projects.get(1).getId())
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "max.mustermann@mail.de", authorities = "EMPLOYEE")
    void projectControllerPermissionTest() throws Exception {
        // EMPLOYEE can only get projects they are assigned to others return 404
        val getProjectResponse = projectController.getProjects();
        assertThat(getProjectResponse.getStatusCode(), is(OK));
        assertThat(getProjectResponse.getBody(), notNullValue());
        assertThat(getProjectResponse.getBody(), hasSize(1));
        assertThat(getProjectResponse.getBody().get(0).getName(), is(projects.get(0).getName()));

        testUtils.getRequest("/api/v1/projects/" + projects.get(1).getId())
            .andExpect(status().isNotFound());

        testUtils.postRequest(
            "/api/v1/projects",
            CreateProject.builder()
                .name("New Test Project")
                .description("Description of the new test project")
                .manager(getUser(2).getId())
                .users(Set.of(getUser(1).getId(), getUser(2).getId()))
                .build()
        ).andExpect(status().isForbidden());
        testUtils.patchRequest("/api/v1/projects/" + projects.get(0).getId(), UpdateProject.builder().build())
            .andExpect(status().isForbidden());
        testUtils.deleteRequest("/api/v1/projects/" + projects.get(0).getId()).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void createUpdateThenDeleteProjectTest() {
        val createProjectResponse = projectController.createProject(
            CreateProject.builder()
                .name("New Test Project")
                .description("Description of the new test project")
                .manager(getUser(2).getId())
                .users(Set.of(getUser(1).getId(), getUser(2).getId()))
                .build()
        );

        assertThat(createProjectResponse.getStatusCode(), is(CREATED));
        assertThat(createProjectResponse.getBody(), notNullValue());
        assertThat(createProjectResponse.getBody().getName(), is("New Test Project"));
        assertThat(createProjectResponse.getBody().getProjectManager().getName(), is(getUser(2).getName()));
        assertThat(createProjectResponse.getBody().getUsers(), hasSize(2));
        assertThat(createProjectResponse.getBody()
            .getUsers()
            .stream()
            .anyMatch(user -> user.getId().equals(getUser(1).getId())), is(true)
        );
        assertThat(createProjectResponse.getBody()
            .getUsers()
            .stream()
            .anyMatch(user -> user.getId().equals(getUser(2).getId())), is(true)
        );

        // Exchange project manager and update title
        assertThat(getUser(0).getProjects(), hasSize(0));
        val updateProjectResponse = projectController.updateProject(
            createProjectResponse.getBody().getId(),
            UpdateProject.builder()
                .name("Updated Test Project")
                .manager(getUser(0).getId())
                .users(Set.of(getUser(0).getId(), getUser(1).getId()))
                .build()
        );

        assertThat(updateProjectResponse.getStatusCode(), is(OK));
        assertThat(updateProjectResponse.getBody(), notNullValue());
        assertThat(updateProjectResponse.getBody().getName(), is("Updated Test Project"));
        assertThat(updateProjectResponse.getBody().getProjectManager().getName(), is(getUser(0).getName()));
        assertThat(updateProjectResponse.getBody().getUsers(), hasSize(2));

        // Assert projects also set in users
        val projectId = updateProjectResponse.getBody().getId();
        assertThat(projectRepository.findById(projectId).get().getProjectManager().getId(), is(getUser(0).getId()));
        assertThat(projectRepository.findById(projectId).get().getUsers(), hasSize(2));
        assertThat(projectRepository.findById(projectId)
            .get()
            .getUsers()
            .stream()
            .anyMatch(user -> user.getId().equals(getUser(0).getId())), is(true)
        );
        assertThat(projectRepository.findById(projectId)
            .get()
            .getUsers()
            .stream()
            .anyMatch(user -> user.getId().equals(getUser(1).getId())), is(true)
        );
        assertThat(projectRepository.findById(projectId)
            .get()
            .getUsers()
            .stream()
            .anyMatch(user -> user.getId().equals(getUser(2).getId())), is(false)
        );

        val deleteProjectResponse = projectController.deleteProject(updateProjectResponse.getBody().getId());

        assertThat(deleteProjectResponse.getStatusCode(), is(NO_CONTENT));
        assertThat(projectRepository.findById(updateProjectResponse.getBody().getId()).isEmpty(), is(true));
        assertThat(getUser(0), notNullValue());
        assertThat(getUser(0).getProjects(), hasSize(0));
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void projectControllerErrorTest() throws Exception {
        // Same title project is conflict
        val conflictProject =
            CreateProject.builder()
                .name("Project 1")
                .description("Description of the new test project")
                .manager(getUser(2).getId())
                .users(Set.of(getUser(1).getId(), getUser(2).getId()))
                .build();
        testUtils.postRequest("/api/v1/projects", conflictProject)
            .andExpect(status().isConflict());

        // ID of user not found is unprocessable entity
        val invalidUserProject =
            CreateProject.builder()
                .name("New Test Project")
                .description("Description of the new test project")
                .manager(getUser(2).getId())
                .users(Set.of(getUser(2).getId(), UUID.randomUUID()))
                .build();
        testUtils.postRequest("/api/v1/projects", invalidUserProject)
            .andExpect(status().isUnprocessableEntity());

        // ID of manager not found is unprocessable entity
        val manager = UUID.randomUUID();
        val invalidManagerProject = CreateProject.builder()
            .manager(manager)
            .users(Set.of(manager))
            .build();
        testUtils.patchRequest(
                "/api/v1/projects/" + projects.get(0).getId(),
                invalidManagerProject
            )
            .andExpect(status().isUnprocessableEntity());

        // Update project that does not exist is not found
        testUtils.patchRequest(
            "/api/v1/projects/" + Integer.MAX_VALUE,
            UpdateProject.builder().build()
        ).andExpect(status().isNotFound());

        // Delete project that does not exist is not found
        testUtils.deleteRequest("/api/v1/projects/" + Integer.MAX_VALUE)
            .andExpect(status().isNotFound());
    }

    private User getUser(int num) {
        return userRepository.findById(users.get(num).getId()).orElse(null);
    }
}
