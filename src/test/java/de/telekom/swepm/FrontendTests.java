package de.telekom.swepm;

import de.telekom.swepm.controller.TaskController;
import de.telekom.swepm.domain.Project;
import de.telekom.swepm.domain.Role;
import de.telekom.swepm.domain.Status;
import de.telekom.swepm.domain.Task;
import de.telekom.swepm.dto.request.CreateTask;
import de.telekom.swepm.repos.ProjectRepository;
import de.telekom.swepm.repos.TaskRepository;
import de.telekom.swepm.repos.UserRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FrontendTests {
    @Value("${server.port}")
    private int serverPort;

    private static WebDriver driver;
    private static WebDriverWait wait;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskController taskController;
    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    public void setupAll() {
        WebDriverManager.chromedriver().setup();

        // Set up ChromeOptions
        val options = new ChromeOptions();
        options.addArguments("--headless"); // Comment this out for debugging, to see the browser
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-debugging-port=9222");

        // Initialize WebDriver and WebDriverWait
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        //Login as projio:projio PM
        loginAsUser("projio", "projio");
    }

    private void loginAsUser(String user, String password) {
        driver.get("http://localhost:" + serverPort + "/login");

        val emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        val passwordField = driver.findElement(By.id("password"));
        val loginButton = driver.findElement(By.cssSelector("button[type='submit']"));

        emailField.sendKeys(user);
        passwordField.sendKeys(password);
        loginButton.click();

        wait.until(ExpectedConditions.urlContains("projects"));
    }

    @Test
    @WithMockUser(username = "projio", roles = "PROJECT_MANAGER")
    void taskBoardPerformanceTest() {
        List<Integer> taskIds = new ArrayList<>();
        try {
            driver.get("http://localhost:" + serverPort + "/projects");
            val projectId = projectRepository.findAll().get(0).getId();
            val startTime = System.currentTimeMillis();

            for (int i = 0; i < 500; i++) {
                taskIds.add(taskController.createTask(
                    projectId, CreateTask.builder()
                        .title("Testaufgabe 1")
                        .description("task description :)")
                        .dueDate(LocalDate.now().plusDays(7))
                        .build()
                ).getBody().getId());
            }
            val timeToCreateTasks = System.currentTimeMillis() - startTime;

            assertThat(timeToCreateTasks, is(lessThan(2500L)));

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
            driver.get("http://localhost:" + serverPort + "/projects/" + projectId + "/views?view=board");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("boardList_todo")));
            val endTime = System.currentTimeMillis();
            assertThat(
                "Time to create the Tasks: " + timeToCreateTasks + "\nTime to finish: " + (endTime - startTime),
                endTime - startTime,
                is(lessThan(3000L))
            );
        } finally {
            taskRepository.deleteAllById(taskIds);
        }
    }

    @Test
    @Transactional
    void createProjectTest() {
        driver.get("http://localhost:" + serverPort + "/projects");

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));

        val createProjectButton = driver.findElement(By.id("createProjectBTN"));
        createProjectButton.sendKeys(Keys.RETURN);

        val projectNameInput = driver.findElement(By.id("createProject_name"));
        val projectDescriptionInput = driver.findElement(By.id("createProject_description"));
        val managerListElement = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='managerList']/li[3]")));

        projectNameInput.sendKeys("Testprojekt");
        projectDescriptionInput.sendKeys("projectdescription");
        managerListElement.click();

        //todo when one manager is the single user in the application no user should be visible in this list after the manager has been selected
        val userListElement = driver.findElement(By.xpath("//*[@id='userList']/li[1]"));
        userListElement.click();

        driver.findElement(By.id("confirmProjectCreateForm_btn")).click();

        wait.until(ignored -> projectRepository.findByName("Testprojekt").isPresent());

        // Database Test
        Optional<Project> testProject = projectRepository.findByName("Testprojekt");

        assertTrue(testProject.isPresent());
        assertEquals("Testprojekt", testProject.get().getName());
        assertEquals("projectdescription", testProject.get().getDescription());
        assertEquals("Projio Admin", testProject.get().getProjectManager().getName());
        assertEquals(2, testProject.get().getUsers().size());
        assertEquals(0, testProject.get().getTasks().size());

        // UI Test
        List<Project> projects = projectRepository.findAll();
        projects.forEach(project -> {
            if (!"Testprojekt".equals(project.getName())) {
                return; // Skip if the project name is not "testprojekt"
            }
            val card = driver.findElement(By.id("projectCard" + project.getId()));

            val titleElement = card.findElement(By.className("card-title"));
            assertThat(titleElement.getText(), is(project.getName()));

            val projectManagerElement = card.findElement(By.className("card-subtitle"));
            assertThat(projectManagerElement.getText(), is(project.getProjectManager().getName()));

            val descriptionElement = card.findElement(By.className("card-text"));
            assertThat(descriptionElement.getText(), is(project.getDescription()));

            val buttonElement = card.findElement(By.id("selectProjectButton" + project.getId()));
            assertThat(
                buttonElement.getAttribute("href"),
                is("http://localhost:" + serverPort + "/projects/" + project.getId() + "/views?view=board")
            );

            if (project.getTasks().isEmpty()) {
                assertFalse(card.findElements(By.id("noTasksInProject" + project.getId())).isEmpty());
            } else {
                Map<Status, Long> statusCountMap = project.getTasks().stream()
                    .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));

                statusCountMap.forEach((status, count) ->
                    assertFalse(card.findElements(By.id(status.toString() + count + project.getId())).isEmpty())
                );
            }
            val memberSection = card.findElement(By.id("user"));
            val memberCountElement = memberSection.findElement(By.tagName("h5"));
            assertThat(Integer.valueOf(memberCountElement.getText()), is(project.getUsers().size()));
        });
    }

    @Test
    void createUserTest() {
        driver.get("http://localhost:" + serverPort + "/projects");
        // Navigate to the project page
        driver.get("http://localhost:" + serverPort + "/projects");

        // Use explicit wait for modal elements that need to be visible
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // Find and click the "Create User" button
        val createUserButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("createUserBTN")));
        createUserButton.click();

        // Wait until the modal is fully visible
        WebElement createUserModal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("createUserModal")));

        // Wait until the input field in the modal is interactable
        WebElement userNameInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("createUser_name")));
        userNameInput.sendKeys("Max Ackermann");

        // Fill in email
        WebElement userEmailInput = driver.findElement(By.id("createUser_email"));
        userEmailInput.sendKeys("max.ackermann@gmail.com");

        // Test if the password input is initially empty
        WebElement passwordInput = driver.findElement(By.id("createUser_password"));
        assertThat(passwordInput.getAttribute("value").isEmpty(), is(true));

        // Generate password and verify it is generated
        String initialPassword = passwordInput.getAttribute("value");
        driver.findElement(By.id("generatePW")).click();

        // Wait until the password field is updated
        wait.until(ExpectedConditions.not(
            ExpectedConditions.attributeToBe(By.id("createUser_password"), "value", initialPassword)));
        String newPassword = passwordInput.getAttribute("value");

        // Ensure password was generated and is different from the initial
        assertFalse(newPassword.isEmpty());
        assertNotSame(initialPassword, newPassword);

        // Submit the form
        driver.findElement(By.id("confirmUserCreateForm_btn")).sendKeys(Keys.RETURN);

        // Wait for the user to be created and verify its existence in the repository
        wait.until(ignored -> userRepository.findByEmailAddressIgnoreCase("max.ackermann@gmail.com").isPresent());
        val user = userRepository.findByEmailAddressIgnoreCase("max.ackermann@gmail.com");

        assertTrue(user.isPresent());
    }

    @Test
    void createProjectManagerTest() {
        // Navigate to the project page
        driver.get("http://localhost:" + serverPort + "/projects");

        // Use explicit wait for modal elements that need to be visible
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // Find and click the "Create User" button
        val createUserButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("createUserBTN")));
        createUserButton.click();

        // Wait until the modal is fully visible
        WebElement createUserModal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("createUserModal"))
        );

        // Wait until the input field in the modal is interactable
        WebElement userNameInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("createUser_name")));
        userNameInput.sendKeys("Erika Mustermann");

        // Fill in email
        WebElement userEmailInput = driver.findElement(By.id("createUser_email"));
        userEmailInput.sendKeys("erika.mustermann@gmail.com");

        // Test if the password input is initially empty
        WebElement passwordInput = driver.findElement(By.id("createUser_password"));
        assertThat(passwordInput.getAttribute("value").isEmpty(), is(true));

        // Generate password and verify it is generated
        String initialPassword = passwordInput.getAttribute("value");
        driver.findElement(By.id("generatePW")).click();

        // Wait until the password field is updated
        wait.until(ExpectedConditions.not(
            ExpectedConditions.attributeToBe(By.id("createUser_password"), "value", initialPassword)));
        String newPassword = passwordInput.getAttribute("value");

        // Ensure password was generated and is different from the initial
        assertFalse(newPassword.isEmpty());
        assertNotSame(initialPassword, newPassword);

        // Click the label for "Project Manager" to select the radio button
        WebElement projectManagerLabel = driver.findElement(By.cssSelector("label[for='projectManager']"));
        projectManagerLabel.click();

        // Submit the form
        driver.findElement(By.id("confirmUserCreateForm_btn")).sendKeys(Keys.RETURN);

        // Wait for the user to be created and verify its existence in the repository
        wait.until(ignored -> userRepository.findByEmailAddressIgnoreCase("erika.mustermann@gmail.com").isPresent());
        val user = userRepository.findByEmailAddressIgnoreCase("erika.mustermann@gmail.com");

        // Verify that the user was created and has the role of "PROJECT_MANAGER"
        assertTrue(user.isPresent());
        assertThat(user.get().getRole(), is(Role.PROJECT_MANAGER));
    }

    @Test
    void createUserBoardTest() {
        val projectId = projectRepository.findAll().get(0).getId();

        // Navigate to the project page
        driver.get("http://localhost:" + serverPort + "/projects/" + projectId + "/views?view=board");

        // Use explicit wait for modal elements that need to be visible
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // Find and click the "Create User" button
        val createUserButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("createUserBTN")));
        createUserButton.click();

        // Wait until the modal is fully visible
        WebElement createUserModal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("createUserModal")));

        // Wait until the input field in the modal is interactable
        WebElement userNameInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("createUser_name")));
        userNameInput.sendKeys("Max Ackermann");

        // Fill in email
        WebElement userEmailInput = driver.findElement(By.id("createUser_email"));
        userEmailInput.sendKeys("max.ackermann@gmail.com");

        // Test if the password input is initially empty
        WebElement passwordInput = driver.findElement(By.id("createUser_password"));
        assertThat(passwordInput.getAttribute("value").isEmpty(), is(true));

        // Generate password and verify it is generated
        String initialPassword = passwordInput.getAttribute("value");
        driver.findElement(By.id("generatePW")).click();

        // Wait until the password field is updated
        wait.until(ExpectedConditions.not(
            ExpectedConditions.attributeToBe(By.id("createUser_password"), "value", initialPassword)));
        String newPassword = passwordInput.getAttribute("value");

        // Ensure password was generated and is different from the initial
        assertFalse(newPassword.isEmpty());
        assertNotSame(initialPassword, newPassword);

        // Submit the form
        driver.findElement(By.id("confirmUserCreateForm_btn")).sendKeys(Keys.RETURN);

        // Wait for the user to be created and verify its existence in the repository
        wait.until(ignored -> userRepository.findByEmailAddressIgnoreCase("max.ackermann@gmail.com").isPresent());
        val user = userRepository.findByEmailAddressIgnoreCase("max.ackermann@gmail.com");

        assertTrue(user.isPresent());
    }

    @Test
    void createProjectManagerBoardTest() {
        val projectId = projectRepository.findAll().get(0).getId();

        // Navigate to the project page
        driver.get("http://localhost:" + serverPort + "/projects/" + projectId + "/views?view=board");

        // Use explicit wait for modal elements that need to be visible
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // Find and click the "Create User" button
        val createUserButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("createUserBTN")));
        createUserButton.click();

        // Wait until the modal is fully visible
        WebElement createUserModal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("createUserModal"))
        );

        // Wait until the input field in the modal is interactable
        WebElement userNameInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("createUser_name")));
        userNameInput.sendKeys("Erika Mustermann");

        // Fill in email
        WebElement userEmailInput = driver.findElement(By.id("createUser_email"));
        userEmailInput.sendKeys("erika.mustermann@gmail.com");

        // Test if the password input is initially empty
        WebElement passwordInput = driver.findElement(By.id("createUser_password"));
        assertThat(passwordInput.getAttribute("value").isEmpty(), is(true));

        // Generate password and verify it is generated
        String initialPassword = passwordInput.getAttribute("value");
        driver.findElement(By.id("generatePW")).click();

        // Wait until the password field is updated
        wait.until(ExpectedConditions.not(
            ExpectedConditions.attributeToBe(By.id("createUser_password"), "value", initialPassword)));
        String newPassword = passwordInput.getAttribute("value");

        // Ensure password was generated and is different from the initial
        assertFalse(newPassword.isEmpty());
        assertNotSame(initialPassword, newPassword);

        // Click the label for "Project Manager" to select the radio button
        WebElement projectManagerLabel = driver.findElement(By.cssSelector("label[for='projectManager']"));
        projectManagerLabel.click();

        // Submit the form
        driver.findElement(By.id("confirmUserCreateForm_btn")).sendKeys(Keys.RETURN);

        // Wait for the user to be created and verify its existence in the repository
        wait.until(ignored -> userRepository.findByEmailAddressIgnoreCase("erika.mustermann@gmail.com").isPresent());
        val user = userRepository.findByEmailAddressIgnoreCase("erika.mustermann@gmail.com");

        // Verify that the user was created and has the role of "PROJECT_MANAGER"
        assertTrue(user.isPresent());
        assertThat(user.get().getRole(), is(Role.PROJECT_MANAGER));
    }


    /*@Test
    void addTaskTest() throws InterruptedException {
        driver.get("http://localhost:" + serverPort + "/projects/1/views?view=board");
        Thread.sleep(1420);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("createTaskButton"))).sendKeys(Keys.RETURN);

        val title = driver.findElement(By.id("createTask_name"));
        val description = driver.findElement(By.id("createTask_description"));
        val dueDate = driver.findElement(By.id("createTask_dueDate"));
        val secondUserField = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("userListTaskView")));

        title.sendKeys("Testaufgabe 1");
        description.sendKeys("project description :)");
        dueDate.sendKeys("11112025");
        secondUserField.click();

        driver.findElement(By.id("confirmTaskCreateForm_button")).sendKeys(Keys.RETURN);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("createTaskModal")));

        WebElement taskList = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("boardList_todo")));
        List<WebElement> taskItems = taskList.findElements(By.tagName("li"));

        boolean taskFound = false;
        for (WebElement item : taskItems) {
            WebElement taskTitleElement = item.findElement(By.id("taskTitle"));
            if (taskTitleElement.getText().equals("Testaufgabe 1")) {
                taskFound = true;
                break;
            }
        }

        assertTrue(taskFound);
    }

    @Test
    void projectNavbarTest() throws InterruptedException {
        driver.get("http://localhost:" + serverPort + "/projects/1/views?view=board");
        Thread.sleep(1000);
        driver.findElement(By.id("nav-list")).click();

        assertThat(driver.getCurrentUrl(), is("http://localhost:" + serverPort + "/projects/1/views?view=list"));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("nav-details"))).click();

        assertThat(driver.getCurrentUrl(), is("http://localhost:" + serverPort + "/projects/1/views" +
            "?view=details"));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("nav-board"))).click();
        assertThat(driver.getCurrentUrl(), is("http://localhost:" + serverPort + "/projects/1/views?view=board"));
    }*/

    @Test
    @Transactional
    void cardTest() {
        driver.get("http://localhost:" + serverPort + "/projects");

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));

        // Retrieve all projects from the repository
        List<Project> projects = projectRepository.findAll();

        // Find all elements with the class "card" and check if the number matches expected count
        By locator = By.cssSelector("[id^='projectCard']");
        List<WebElement> cards = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));

        assertThat(cards.size(), is(projects.size()));

        projects.forEach(project -> {

            val card = driver.findElement(By.id("projectCard" + project.getId()));

            val titleElement = card.findElement(By.className("card-title"));
            assertThat(titleElement.getText(), is(project.getName()));

            val projectManagerElement = card.findElement(By.className("card-subtitle"));
            assertThat(projectManagerElement.getText(), is(project.getProjectManager().getName()));

            val descriptionElement = card.findElement(By.className("card-text"));
            assertThat(descriptionElement.getText(), is(project.getDescription()));

            val buttonElement = card.findElement(By.id("selectProjectButton" + project.getId()));
            assertThat(
                buttonElement.getAttribute("href"),
                is("http://localhost:" + serverPort + "/projects/" + project.getId() + "/views?view=board")
            );

            if (project.getTasks().isEmpty()) {
                assertFalse(card.findElements(By.id("noTasksInProject" + project.getId())).isEmpty());
            } else {
                Map<Status, Long> statusCountMap = project.getTasks().stream()
                    .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));

                statusCountMap.forEach((status, count) ->
                    assertFalse(card.findElements(By.id(status.toString() + count + project.getId())).isEmpty())
                );
            }
            val memberSection = card.findElement(By.id("user"));
            val memberCountElement = memberSection.findElement(By.tagName("h5"));
            assertThat(Integer.valueOf(memberCountElement.getText()), is(project.getUsers().size()));
        });
    }

    @Test
    void notificationTest() {
        loginAsUser("erika.mustermann@mail.de", "erika");
        var notificationButton = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.className("mailbox-dropdown"))
        );
        notificationButton.click();

        assertThat(
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("mailbox-content"))).isDisplayed(),
            is(true)
        );

        val notificationList = driver.findElements(By.xpath("//a[contains(@class, 'notification-title')]"));
        assertThat(notificationList.size(), is(1));

        val notificationListItem = driver.findElement(By.xpath("//a[contains(@class, 'notification-title') and " +
            "contains" +
            "(text(), 'Task 1')]"));

        notificationListItem.click();

        val projectID = projectRepository.findByName("Project 1").get().getId();
        assertThat(
            driver.getCurrentUrl(),
            is("http://localhost:" + serverPort + "/projects/" + projectID + "/views?view=board")
        );

        val doneTasks = taskRepository.findAllByStatus(Status.DONE);
        assertThat(doneTasks.size(), is(0));

        val subTask1 = taskRepository.findAllByTitle("SubTask 1").get(0);
        subTask1.setStatus(Status.DONE);
        taskRepository.save(subTask1);
        val subTask2 = taskRepository.findAllByTitle("SubTask 2").get(0);
        subTask2.setStatus(Status.DONE);
        taskRepository.save(subTask2);
        val task1 = taskRepository.findAllByTitle("Task 1").get(0);
        task1.setStatus(Status.DONE);
        taskRepository.save(task1);

        val newDoneTasks = taskRepository.findAllByStatus(Status.DONE);
        assertThat(newDoneTasks.size(), is(3));
        assertThat(newDoneTasks.get(0).getTitle(), is("Task 1"));

        driver.get("http://localhost:" + serverPort + "/projects/" + projectID + "/views?view=board");

        notificationButton = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.className("mailbox-dropdown"))
        );
        notificationButton.click();

        assertThat(
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("mailbox-content"))).isDisplayed(),
            is(true)
        );

        val newNotificationList = driver.findElements(By.xpath("//a[contains(@class, 'notification-title')]"));
        assertThat(newNotificationList.size(), is(0));
    }

/*    @Test
    void notificationTestLocalWorking() throws InterruptedException {
        loginAsUser("erika.mustermann@mail.de", "erika");

        var notificationButton = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.className("mailbox-dropdown"))
        );
        notificationButton.click();

        assertThat(
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("mailbox-content"))).isDisplayed(),
            is(true)
        );
        val notificationList = driver.findElements(By.xpath("//a[contains(@class, 'notification-title')]"));
        assertThat(notificationList.size(), is(1));

        val notificationListItem = driver.findElement(By.xpath("//a[contains(@class, 'notification-title') and " +
            "contains" +
            "(text(), 'Task 1')]"));

        notificationListItem.click();

        val projectID = projectRepository.findByName("Project 1").get().getId();
        assertThat(
            driver.getCurrentUrl(),
            is("http://localhost:" + serverPort + "/projects/" + projectID + "/views?view=board")
        );

        val doneTasks = taskRepository.findAllByStatus(Status.DONE);
        assertThat(doneTasks.size(), is(0));

        val subTask1Id = taskRepository.findAllByTitle("SubTask 1").get(0).getId();
        val subTask2Id = taskRepository.findAllByTitle("SubTask 2").get(0).getId();
        val task1Id = taskRepository.findAllByTitle("Task 1").get(0).getId();

        moveItemToDone(subTask1Id);
        moveItemToDone(subTask2Id);
        moveItemToDone(task1Id);

        val newDoneTasks = taskRepository.findAllByStatus(Status.DONE);
        assertThat(newDoneTasks.size(), is(3));
        assertThat(newDoneTasks.get(0).getTitle(), is("Task 1"));

        notificationButton = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.className("mailbox-dropdown"))
        );
        notificationButton.click();

        assertThat(
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("mailbox-content"))).isDisplayed(),
            is(true)
        );

        val newNotificationList = driver.findElements(By.xpath("//a[contains(@class, 'notification-title')]"));
        assertThat(newNotificationList.size(), is(0));
    }

    private void moveItemToDone(int taskId) throws InterruptedException {

        // Finde das <li> Element erneut, bevor es angeklickt wird, um StaleElement zu vermeiden
        val currentItem = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//li[@data-id='" + taskId + "']")));

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", currentItem);
        currentItem.click();

        val taskStatus = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("editTask_status")));

        val selection = new Select(taskStatus);
        if (selection.getFirstSelectedOption().getText().equals("Fertig")) {
            driver.findElement(By.id("editTask_close")).click();
            return;
        }

        selection.selectByVisibleText("Fertig");

        driver.findElement(By.id("confirmProjectCreateForm_btn")).click();
        Thread.sleep(1000);
    }*/

    @Test
    void resetPasswordTest() {
        driver.get("http://localhost:" + serverPort + "/login");

        val emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        val passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        val loginButton = driver.findElement(By.cssSelector("button[type='submit']"));

        emailField.sendKeys("max.mustermann@mail.de");
        passwordField.sendKeys("max");
        loginButton.sendKeys(Keys.RETURN);

        //check if redirect to the reset password page was successful for a new user
        assertThat(driver.getCurrentUrl(), is("http://localhost:" + serverPort + "/password"));

        val newPasswordField = driver.findElement(By.id("newPassword"));
        val confirmPasswordField = driver.findElement(By.id("confirmPassword"));

        newPasswordField.sendKeys("abc123DEF!ab");
        confirmPasswordField.sendKeys("abc123DEF!ab");

        val submitPasswordButton = driver.findElement(By.id("submitPassword"));
        submitPasswordButton.sendKeys(Keys.RETURN);

        //check if redirected back to login was successful after waiting for redirect back to login page
        wait.until(ExpectedConditions.urlContains("login"));
        assertThat(driver.getCurrentUrl(), is("http://localhost:" + serverPort + "/login?logout"));

        //search for relevant login fields again after redirect

        var emailFieldRelog = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        var passwordFieldRelog = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        var loginButtonRelog = driver.findElement(By.cssSelector("button[type='submit']"));
        emailFieldRelog.sendKeys("max.mustermann@mail.de");
        passwordFieldRelog.sendKeys("abc123DEF!ab");

        //try login with new credentials
        loginButtonRelog.sendKeys(Keys.RETURN);
        wait.until(ExpectedConditions.urlContains("projects"));
        //check if login and redirect to projects overview was successful
        assertThat(driver.getCurrentUrl(), is("http://localhost:" + serverPort + "/projects"));
    }

    @Test
    void testDropdownMenu() {
        driver.get("http://localhost:" + serverPort + "/projects");

        val userDropdown = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector(".user-dropdown")));
        userDropdown.click();

        val navbarUser = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("navbarUser")));
        val navbarUserEmail = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("navbarUserEmail")));
        val navbarUserRole = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("navbarUserRole")));

        assertThat(navbarUser.getText(), is("Projio Admin"));
        assertThat(navbarUserEmail.getText(), is("projio"));
        assertThat(navbarUserRole.getText(), is("Projektmanager"));
    }

//    @Test
//    void accessProjectListView() {
//        loginAsUser("erika.mustermann@mail.de", "erika");
//        driver.get("http://localhost:" + serverPort + "/projects/2/views?view=list");
//
//        assertThat(driver.getCurrentUrl(), containsString("/views?view=list"));
//
//        val taskTable = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tasksTable")));
//        assertThat(taskTable.isDisplayed(), is(true));
//    }

    @AfterEach
    public void teardownAll() {
        if (driver != null) {
            driver.quit(); //Comment out to leave chrome window open after test
        }
    }
}
