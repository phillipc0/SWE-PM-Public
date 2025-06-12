package de.telekom.swepm;

import de.telekom.swepm.controller.UserController;
import de.telekom.swepm.domain.User;
import de.telekom.swepm.dto.request.CreateUser;
import de.telekom.swepm.dto.request.UpdatePassword;
import de.telekom.swepm.dto.request.UpdateUser;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.telekom.swepm.domain.Role.EMPLOYEE;
import static de.telekom.swepm.domain.Role.PROJECT_MANAGER;
import static java.util.Base64.getDecoder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserController userController;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestSetup testSetup;

    @Autowired
    private TestUtils testUtils;

    /**
     * 0: Projio (Project Manager)
     * 1: Max
     * 2: Erika (Project Manager)
     * 3: Otto
     */
    private List<User> users;

    @BeforeEach
    void setup() {
        reset();
        testSetup.setup();
        this.users = testSetup.getUsers();
    }

    @AfterEach
    void reset() {
        log.info("Deleting test db");
        projectRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();
        System.out.println("Deleted all users and projects");
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void getAllUsersTest() {
        val response = userController.getUsers();
        assertThat(response.getStatusCode(), is(OK));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody(), hasSize(4));
        assertThat(response.getBody().get(0).getName(), is(getUser(0).getName()));
    }

    @Test
    @WithMockUser(username = "max.mustermann@mail.de", authorities = "EMPLOYEE")
    void getUserTest() {
        val response = userController.getUser(getUser(1).getId());
        assertThat(response.getStatusCode(), is(OK));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody().getName(), is(getUser(1).getName()));

        val response2 = userController.getUser(
            UUID.fromString(new String(getDecoder().decode("Njk2OTY5NjktNjk2OS02OTY5LTY5NjktNjk2OTY5Njk2OTY5")))
        );

        assertThat(response2.getStatusCode(), is(I_AM_A_TEAPOT));
        assertThat(response2.getBody(), notNullValue());
        assertThat(
            response2.getBody().getName(),
            is(new String(getDecoder()
                .decode("UHJvamVrdCB2b24gRGFuaWVsLCBKYW4sIE1heCwgUGhpbGxpcCwgUm9iaW4gdW5kIFhhdmVy"))
            )
        );
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void getUsersUpcomingTasksTest() {
        val tasks = userController.getCurrentUsersUpcomingTasks();
        assertThat(tasks.getStatusCode(), is(OK));
        assertThat(tasks.getBody(), notNullValue());
        assertThat(tasks.getBody(), hasSize(1));
    }

    @Test
    @WithMockUser(username = "projio", authorities = "PROJECT_MANAGER")
    void getUsersUpcomingTasksErrorTest() {
        val tasks = userController.getCurrentUsersUpcomingTasks();
        assertThat(tasks.getStatusCode(), is(OK));
        assertThat(tasks.getBody(), notNullValue());
        assertThat(tasks.getBody(), hasSize(0));
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void createUserTest() throws Exception {
        val employeeResponse = userController.createUser(CreateUser.builder()
            .name("New Test User")
            .emailAddress("newtestuser@mail.de")
            .password("newTestUser123#")
            .role(EMPLOYEE)
            .build());

        assertThat(employeeResponse.getStatusCode(), is(CREATED));
        assertThat(employeeResponse.getBody(), notNullValue());
        assertThat(employeeResponse.getBody().getName(), is("New Test User"));

        val projectManagerResponse = userController.createUser(CreateUser.builder()
            .name("New Test Projectmanager")
            .emailAddress("newtestprojectmanager@mail.de")
            .password("newTestProjectmanager123#")
            .role(PROJECT_MANAGER)
            .build());

        assertThat(projectManagerResponse.getStatusCode(), is(CREATED));
        assertThat(projectManagerResponse.getBody(), notNullValue());
        assertThat(projectManagerResponse.getBody().getName(), is("New Test Projectmanager"));

        testUtils.postRequest(
            "/api/v1/users",
            CreateUser.builder()
                .name("Another New Test User")
                .emailAddress("newtestuser@mail.de")
                .password("newTestUser123#")
                .role(EMPLOYEE)
                .build()
        ).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void createUserErrorTest() throws Exception {
        // Password too short
        testUtils.postRequest(
            "/api/v1/users",
            CreateUser.builder()
                .name("New Test User")
                .emailAddress("newtestuser@mail.de")
                .password("2short")
                .role(EMPLOYEE)
                .build()
        ).andExpect(status().isBadRequest());

        // Missing email address
        testUtils.postRequest(
            "/api/v1/users",
            CreateUser.builder()
                .name("New Test User")
                .password("newTestUser123#")
                .role(EMPLOYEE)
                .build()
        ).andExpect(status().isBadRequest());

        //Wrong E-Mail format
        testUtils.postRequest(
            "/api/v1/users",
            CreateUser.builder()
                .name("New Test User")
                .emailAddress("newtestusermail.de")
                .password("newTestUser123#")
                .role(EMPLOYEE)
                .build()
        ).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void updateUserTest() {
        // Update name of an employee
        val updateEmployeeNameResponse = userController.updateUser(
            getUser(1).getId(),
            UpdateUser.builder()
                .name("Maximilian Mustermann")
                .build()
        );

        assertThat(updateEmployeeNameResponse.getStatusCode(), is(OK));
        assertThat(updateEmployeeNameResponse.getBody(), notNullValue());
        assertThat(updateEmployeeNameResponse.getBody().getName(), is("Maximilian Mustermann"));
        assertThat(updateEmployeeNameResponse.getBody().getEmailAddress(), is(getUser(1).getEmailAddress()));
        assertThat(getUser(1).getName(), is("Maximilian Mustermann"));

        // Update email address of a project manager
        val updateProjectManagerEmailResponse = userController.updateUser(
            getUser(2).getId(),
            UpdateUser.builder()
                .emailAddress("erikamusterfrau@mail.de")
                .build()
        );

        assertThat(updateProjectManagerEmailResponse.getStatusCode(), is(OK));
        assertThat(updateProjectManagerEmailResponse.getBody(), notNullValue());
        assertThat(updateProjectManagerEmailResponse.getBody().getEmailAddress(), is("erikamusterfrau@mail.de"));
        assertThat(updateProjectManagerEmailResponse.getBody().getName(), is(getUser(2).getName()));
        assertThat(
            getUser(2).getEmailAddress(), is("erikamusterfrau@mail.de"));

        // Update role and name of a project manager
        val updateProjectManagerRoleAndNameResponse = userController.updateUser(
            getUser(2).getId(),
            UpdateUser.builder()
                .name("Erika Musterfrau")
                .role(EMPLOYEE)
                .build()
        );

        assertThat(updateProjectManagerRoleAndNameResponse.getStatusCode(), is(OK));
        assertThat(updateProjectManagerRoleAndNameResponse.getBody(), notNullValue());
        assertThat(updateProjectManagerRoleAndNameResponse.getBody().getName(), is("Erika Musterfrau"));
        assertThat(updateProjectManagerRoleAndNameResponse.getBody().getRole(), is(EMPLOYEE));
        assertThat(getUser(2).getName(), is("Erika Musterfrau"));
        assertThat(getUser(2).getRole(), is(EMPLOYEE));

        // Update email to the same email
        assertThat(
            getUser(2).getEmailAddress(), is("erikamusterfrau@mail.de"));
        val updateEmailToSameResponse = userController.updateUser(
            getUser(2).getId(),
            UpdateUser.builder()
                .emailAddress("erikamusterfrau@mail.de")
                .build()
        );

        assertThat(updateEmailToSameResponse.getStatusCode(), is(OK));
        assertThat(updateEmailToSameResponse.getBody(), notNullValue());
        assertThat(updateEmailToSameResponse.getBody().getEmailAddress(), is("erikamusterfrau@mail.de"));
    }

    @Test
    @WithMockUser(username = "max.mustermann@mail.de", authorities = "EMPLOYEE")
    void changePasswordTest() {
        //change password of new user
        val updatePasswordResponse = userController.changePassword(
            UpdatePassword.builder()
                .password("ABC123321cba!")
                .build()
        );
        assertThat(updatePasswordResponse.getStatusCode(), is(NO_CONTENT));
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void changePasswordOldUserTest() throws Exception {
        // Try to update password of user that is not allowed to change password
        testUtils.patchRequest(
            "/api/v1/users/current/password",
            UpdatePassword.builder()
                .password("ABC123321cba!")
                .build()
        ).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "otto.normalverbraucher@mail.de", authorities = "PROJECT_MANAGER")
    void changePasswordConflictTest() throws Exception {
        //try to change password of user to the same password
        testUtils.patchRequest(
            "/api/v1/users/current/password",
            UpdatePassword.builder()
                .password("OTTOnormalverbraucher123!")
                .build()
        ).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "max.mustermann@mail.de", authorities = "EMPLOYEE")
    void updateUserAsEmployeeTest() throws Exception {
        // Update own user
        val updateOwnNameResponse = userController.updateUser(getUser(1).getId(), UpdateUser.builder()
            .name("Maximilian Mustermann")
            .build());

        assertThat(updateOwnNameResponse.getStatusCode(), is(OK));
        assertThat(updateOwnNameResponse.getBody(), notNullValue());
        assertThat(updateOwnNameResponse.getBody().getName(), is("Maximilian Mustermann"));
        assertThat(updateOwnNameResponse.getBody().getEmailAddress(), is(getUser(1).getEmailAddress()));
        assertThat(
            getUser(1).getName(),
            is("Maximilian Mustermann")
        );

        // Try to update another user
        testUtils.patchRequest(
            "/api/v1/users/" + getUser(2).getId(),
            UpdateUser.builder()
                .name("Erika Musterfrau")
                .build()
        ).andExpect(status().isForbidden());
        assertThat(getUser(2).getName(), is("Erika Mustermann"));

        // Try to promote oneself
        testUtils.patchRequest(
            "/api/v1/users/" + getUser(1).getId(),
            UpdateUser.builder()
                .role(PROJECT_MANAGER)
                .build()
        ).andExpect(status().isForbidden());
        assertThat(getUser(1).getRole(), is(EMPLOYEE));
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void updateUserErrorTest() throws Exception {
        // Try to update a non-existing user
        testUtils.patchRequest(
            "/api/v1/users/" + UUID.randomUUID(),
            UpdateUser.builder()
                .name("Maximilian Mustermann")
                .build()
        ).andExpect(status().isNotFound());

        // Try to update a user with an already existing email address
        testUtils.patchRequest(
            "/api/v1/users/" + getUser(1).getId(),
            UpdateUser.builder()
                .emailAddress("erika.mustermann@mail.de")
                .build()
        ).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void deleteUserTest() {
        // delete normal user
        val deleteEmployeeResponse = userController.deleteUser(getUser(1).getId());

        assertThat(deleteEmployeeResponse.getStatusCode(), is(NO_CONTENT));
        assertThat(getUser(1), is(nullValue()));

        // delete project manager
        val deleteProjectManagerResponse = userController.deleteUser(getUser(2).getId());

        assertThat(deleteProjectManagerResponse.getStatusCode(), is(NO_CONTENT));
        assertThat(getUser(2), is(nullValue()));
    }

    @Test
    @WithMockUser(username = "erika.mustermann@mail.de", authorities = "PROJECT_MANAGER")
    void deleteUserErrorTest() throws Exception {
        val nonExistingUserId = UUID.randomUUID();
        testUtils.deleteRequest("/api/v1/users/" + nonExistingUserId)
            .andExpect(status().isNotFound());
        assertThat(userRepository.findById(nonExistingUserId), is(Optional.empty()));
    }

    @Test
    @WithMockUser(username = "max.mustermann@mail.de", authorities = "EMPLOYEE")
    void deleteUserWrongAuthorityTest() throws Exception {
        testUtils.deleteRequest("/api/v1/users/" + getUser(2).getId())
            .andExpect(status().isForbidden());

        assertThat(userRepository.findById(getUser(2).getId()).isPresent(), is(true));
    }

    @Test
    @WithMockUser(username = "max.mustermann@mail.de", authorities = "EMPLOYEE")
    void userErrorTest() throws Exception {
        testUtils.getRequest("/api/v1/users")
            .andExpect(status().isForbidden());
        testUtils.getRequest("/api/v1/users/" + UUID.randomUUID())
            .andExpect(status().isNotFound());

        testUtils.postRequest(
            "/api/v1/users",
            CreateUser.builder()
                .name("New Test User")
                .emailAddress("newtestuser@mail.de")
                .password("newTestUser123#")
                .role(EMPLOYEE)
                .build()
        ).andExpect(status().isForbidden());
    }

    private User getUser(int num) {
        return userRepository.findById(users.get(num).getId()).orElse(null);
    }
}
