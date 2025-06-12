// ============================
// Global Variable Declarations
// ============================

// Create Task
let userListTaskView = document.getElementById("userListTaskView");
let searchInputTask = document.getElementById("userSearchTask");
let userCountInfoTask = document.getElementById("userCountInfoTask");
let relatedTaskListView = document.getElementById("relatedTaskListView");
let relatedTaskSearch = document.getElementById("relatedTaskSearch");
let taskCountInfo = document.getElementById("taskCountInfo");
let blockingTaskListView = document.getElementById("blockingTaskListView");
let blockingTaskSearch = document.getElementById("blockingTaskSearch");
let blockingTaskCountInfo = document.getElementById("blockingTaskCountInfo");

// Edit Task
let currentEditingTaskId = null;
let assignedUsersTasks = new Set();
let assignedUsersEditTask = new Set();
let assignedSubtasks = new Set();
let selectedParentTask = null;
let selectedBlockingTask = new Set();

// Global Details
let projectDetails = null;
let projectTaskDetails = null;
let userDetails = null;

let userIDMap = {};
let taskIDMap = {};

// =======================
// DOM Content Load
// =======================

document.addEventListener("DOMContentLoaded", async function () {

    await loadNavbar();

    const contentDiv = document.getElementById("content");
    const projectJson = await getProjectDetails();
    const projectTaskJson = await getProjectTasks();

    projectDetails = projectJson;
    projectTaskDetails = projectTaskJson;
    userIDMap = Object.fromEntries(
        projectDetails.users.map(user => [user.id, user.name])
    );
    taskIDMap = Object.fromEntries(
        projectTaskDetails.map(task => [String(task.id), task.title])
    );

    try {
        const response = await fetch('/api/v1/users/current');
        userDetails = await response.json();

    } catch (error) {
        console.error('Error fetching user data:', error);
    }

    updateNavbar();

    function updateNavbar() {
        //update second navbar to reflect new view
        const navItems = document.getElementById("viewNavbar").querySelectorAll(".subNavLink")
        if (!window.location.search) {

            navItems.forEach(navItem => {
                if (navItem != null) {
                    let navElementHref = navItem.getAttribute('href')
                    if (navElementHref === "?view=board") {
                        navItem.classList.add("subNavBarActive")
                    } else {
                        navItem.classList.remove("subNavBarActive")
                    }
                }
            })
        } else {

            //mark correct subNavBar entry as active if page is loaded with parameter
            navItems.forEach(navItem => {
                if (navItem != null) {
                    let navElementHref = navItem.getAttribute('href')
                    if (window.location.search.includes(navElementHref)) {
                        navItem.classList.add("subNavBarActive")
                    } else {
                        navItem.classList.remove("subNavBarActive")
                    }
                }
            })
        }
    }

    function loadView(view) {

        //load subpage into view
        fetch(`/projectview_subpages/${view}`)
            .then(response => response.text())
            .then(data => {
                contentDiv.innerHTML = data;
                subPageOpen(view, projectDetails, userDetails, projectTaskDetails);
            })
            .catch(error => {
                contentDiv.innerHTML = "<p>Error loading view.</p>";
                console.error("Error loading view:", error);
            });

    }

    function getQueryParam(param) {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(param);
    }

    const view = getQueryParam("view") || "board"; // Standardansicht ist "board"
    loadView(view);

    document.getElementById("nav-board").addEventListener("click", (event) => {
        event.preventDefault();
        loadView("board");
        window.history.pushState({}, "", "?view=board");
        updateNavbar();
    });
    document.getElementById("nav-list").addEventListener("click", (event) => {
        event.preventDefault();
        loadView("list");
        window.history.pushState({}, "", "?view=list");
        updateNavbar()
    });
    document.getElementById("nav-details").addEventListener("click", (event) => {
        event.preventDefault();
        loadView("details");
        window.history.pushState({}, "", "?view=details");
        updateNavbar()
    });

    // These eventListener are for creating a new user on one project site (board, list, details)
    // They are only visible for ProjectManager
    // Just copied the code from "navbar.js". The easy way, so it will finally work. Maybe it can be improved later if there is enough time
    let pw;
    document.getElementById("generatePW").addEventListener("click", async function () {
        pw = generatePassword(32);
        navigator.clipboard.writeText(pw).then(function () {
            document.getElementById("createUser_password").value = pw;
        }).catch(function (err) {
        });
    });
    document.getElementById("confirmUserCreateForm_btn").addEventListener("click", async function () {
        if (typeof pw !== "undefined") {
            navigator.clipboard.writeText(pw).then(function () {
            }).catch(function (err) {
            });
        }
    });
    document.getElementById("userCreateForm").addEventListener("submit", async function (e) {
        e.preventDefault();

        // Remove readonly attribute to trigger the required validation
        document.getElementById('createUser_password').removeAttribute('readonly');

        // Trigger form validation
        if (!this.reportValidity()) {
            document.getElementById('createUser_password').setAttribute("readonly", "")
            return; // If the form is invalid, stop the submission
        }

        let formData = new FormData(this)

        let formObj = {}
        formData.forEach(function (value, key) {
            formObj[key] = value;
        })

        let response = await fetch("/api/v1/users", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(formObj)
        })

        if (response.status === 409) {
            document.getElementById("createUser_status").innerHTML = "Konflikt mit existierendem User!"
        } else if (!response.ok) {
            document.getElementById("createUser_status").innerHTML = "Es ist ein Fehler bei der Erstellung des User aufgetreten!"
        } else {
            document.getElementById('userCreateForm').reset();
            document.getElementById('createUser_status').innerHTML = "";
            document.getElementById('createUserModalClose').click();
            document.getElementById('createUser_password').setAttribute("readonly", "")
            pw = generatePassword(32);
        }
    });

    window.addEventListener("popstate", () => {
        const view = getQueryParam("view") || "board";
        loadView(view);
        updateNavbar()
    });
});

// =======================
// Get details from api
// =======================

async function getProjectDetails() {
    const projectId = getProjectIdFromUrl();
    const res = await fetch(`/api/v1/projects/${projectId}`);
    if (res.status !== 200) {
        window.location.replace("/projects")
    }
    return await res.json();
}

async function getProjectTasks() {
    const projectId = getProjectIdFromUrl();
    const tasks = await fetch(`/api/v1/projects/${projectId}/tasks`);
    return await tasks.json();
}

// =================
// Open subpage
// =================

function subPageOpen(pageName, projectJSON, userJSON, taskJSON) {
    var highestTimeoutId = setTimeout(";");
    for (var i = 0; i < highestTimeoutId; i++) {
        // Nur Timer löschen bis auf die Notification-Timer
        if (!notificationTimers.includes(i)) {
            clearTimeout(i);
        }
    }

    if (pageName === "details") {
        document.getElementById("taskFilterInput").classList.add("collapse");
        document.getElementById("taskFilterLabel").classList.add("collapse");
        document.getElementById("createTaskButton").classList.add("collapse");
        openDetailsView(projectJSON, userJSON);

    } else if (pageName === "list") {
        document.getElementById("taskFilterInput").classList.remove("collapse");
        document.getElementById("taskFilterLabel").classList.remove("collapse");
        document.getElementById("createTaskButton").classList.remove("collapse");
        openListView(projectJSON, taskJSON, userJSON);

    } else if (pageName === "board") {
        document.getElementById("taskFilterInput").classList.remove("collapse");
        document.getElementById("taskFilterLabel").classList.remove("collapse");
        document.getElementById("createTaskButton").classList.remove("collapse");
        openBoardView(projectJSON, taskJSON, userJSON);
    }
}

// =========================
// Filter in Board and List
// =========================

document.getElementById("taskFilterInput").addEventListener("input", debounce((e) => {
    const params = new URLSearchParams(window.location.search);
    const view = params.get("view");

    if (view === "list") {
        displayFilteredTasksList(e.target.value);
    } else if (view === "board") {
        displayFilteredTasksBoard(e.target.value);
    } else {
        console.warn("Current view not recognized. No tasks displayed.");
    }
}, 300));

function debounce(func, delay) {
    let debounceTimer;
    return function (...args) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => func.apply(this, args), delay);
    };
}

// =====================================
// Update List or Board View depending
// on where you are at the moment
// =====================================

async function updateListOrBoardView() {
    const params = new URLSearchParams(window.location.search);
    const view = params.get("view");

    if (view === "list") {
        await fetchTasksAndDisplayList();
    } else if (view === "board") {
        await fetchTasksAndDisplayBoard();
    } else {
        console.warn("Current view not recognized. No tasks displayed.");
    }
}

// ===============================================================
// Add Task Section
// ===============================================================

// ===========================================
// Functions to render lists in Add Task Modal
// ===========================================

function renderUserListAddTask(filter = "") {
    userListTaskView.innerHTML = "";

    const filteredUsers = projectDetails.users.filter(user =>
        user.name.toLowerCase().includes(filter.toLowerCase()) || user.emailAddress.toLowerCase().includes(filter.toLowerCase())
    );

    filteredUsers.forEach(user => {
        const userListItem = document.createElement("li");
        userListItem.className = "list-group-item";
        const userNameSpan = document.createElement("span");
        userNameSpan.textContent = user.name;
        const userEmailSpan = document.createElement("span");
        userEmailSpan.textContent = ` (${user.emailAddress})`;
        userEmailSpan.style.color = "gray";
        userListItem.appendChild(userNameSpan);
        userListItem.appendChild(userEmailSpan);
        userListItem.dataset.id = String(user.id);

        if (assignedUsersTasks.has(String(user.id))) {
            userListItem.classList.add("userListSelected");
        }

        userListItem.addEventListener("click", () => {
            userListItem.classList.toggle("userListSelected");
            const id = userListItem.dataset.id;

            if (userListItem.classList.contains("userListSelected")) {
                assignedUsersTasks.add(id);
            } else {
                assignedUsersTasks.delete(id);
            }

            if (assignedUsersTasks.size > 0) {
                document.getElementById("createTask_status").value = "Zugewiesen";
                userCountInfoTask.innerText = `${assignedUsersTasks.size} Mitarbeiter ausgewählt`;
            } else {
                document.getElementById("createTask_status").value = "To Do";
                userCountInfoTask.innerText = "Keine Mitarbeiter ausgewählt";
            }
        });

        userListTaskView.appendChild(userListItem);
    });
}

function renderTaskListAddTask(filter = "") {
    relatedTaskListView.innerHTML = "";

    const parentTasks = projectTaskDetails.filter(task => task.parentTask === null);
    const filteredTasks = parentTasks.filter(task =>
        unescapeHTML(task.title.toLowerCase()).includes(filter.toLowerCase()) || String("#" + task.id).includes(filter.toLowerCase())
    );

    filteredTasks.forEach(task => {
        const taskListItem = createTaskListItem(task);

        if (selectedParentTask === String(task.id)) {
            taskListItem.classList.add("managerListSelected");
        }

        taskListItem.addEventListener("click", () => {
            const isSelected = taskListItem.classList.contains("managerListSelected");

            document.querySelectorAll(".managerListSelected").forEach(el => el.classList.remove("managerListSelected"));

            if (isSelected) {
                selectedParentTask = null;
                taskCountInfo.innerText = "Keine übergeordnete Aufgabe ausgewählt";
            } else {
                taskListItem.classList.add("managerListSelected");
                selectedParentTask = taskListItem.dataset.id;
                taskCountInfo.innerText = "Übergeordnete Aufgabe ausgewählt";
            }
        });

        relatedTaskListView.appendChild(taskListItem);
    });
}

function renderBlockingTaskList(filter = "") {
    blockingTaskListView.innerHTML = "";

    const availableTasks = projectTaskDetails.filter(task =>
        (unescapeHTML(task.title.toLowerCase()).includes(filter.toLowerCase()) || String("#" + task.id).includes(filter.toLowerCase())) &&
        String(task.id) !== selectedParentTask
    );

    availableTasks.forEach(task => {
        const taskItem = createTaskListItem(task);

        if (selectedBlockingTask.has(String(task.id))) {
            taskItem.classList.add("blockedByTaskSelected");
        }

        taskItem.addEventListener("click", () => {
            taskItem.classList.toggle("blockedByTaskSelected");
            const id = taskItem.dataset.id;

            if (taskItem.classList.contains("blockedByTaskSelected")) {
                selectedBlockingTask.add(id);
            } else {
                selectedBlockingTask.delete(id);
            }

            blockingTaskCountInfo.innerText =
                selectedBlockingTask.size > 0 ? `${selectedBlockingTask.size} Aufgaben ausgewählt, welche diese Aufgabe blockieren` : "Keine blockierenden Aufgaben ausgewählt";
        });

        blockingTaskListView.appendChild(taskItem);
    });
}

// ===========================================
// Event Listeners for Add Task Modal Searches
// ===========================================

searchInputTask.addEventListener("input", (e) => {
    renderUserListAddTask(e.target.value);
});

relatedTaskSearch.addEventListener("input", (e) => {
    renderTaskListAddTask(e.target.value);
});

blockingTaskSearch.addEventListener("input", (e) => {
    renderBlockingTaskList(e.target.value);
});

// ==================================
// Functions to Create Task
// ==================================

const taskCreateForm = document.getElementById("TaskCreateForm");
document.getElementById("createTaskModal").addEventListener("show.bs.modal", function () {
    taskCreateForm.reset();

    document.getElementById("userCountInfoTask").innerText = "Kein Mitarbeiter ausgewählt";
    document.getElementById("taskCountInfo").innerText = "Keine übergeordnete Aufgabe ausgewählt";
    document.getElementById("blockingTaskCountInfo").innerText = "0 Aufgaben ausgewählt, welche diese Aufgabe blockieren";

    const today = new Date().toISOString().split("T")[0];
    const nextWeek = new Date();
    nextWeek.setDate(nextWeek.getDate() + 7);
    document.getElementById("createTask_dueDate").value = nextWeek.toISOString().split("T")[0];
    document.getElementById("createTask_dueDate").setAttribute("min", today);

    assignedUsersTasks.clear();
    selectedParentTask = null;
    selectedBlockingTask.clear();

    renderUserListAddTask();
    renderTaskListAddTask();
    renderBlockingTaskList();
});

if (!taskCreateForm.dataset.listenerAdded) {
    // This check fix the problem that a task is created more than once
    taskCreateForm.addEventListener("submit", async function (e) {
        e.preventDefault();
        const errorContainer = document.getElementById("createTask_error");
        errorContainer.textContent = "";

        const formData = new FormData(this);

        const formObj = Object.fromEntries(formData.entries());
        formObj["status"] = "TODO";
        formObj["assignedUsers"] = Array.from(assignedUsersTasks);
        formObj["parentTask"] = selectedParentTask;
        formObj["blockedBy"] = Array.from(selectedBlockingTask);

        try {
            const response = await fetch(`/api/v1/projects/${projectDetails.id}/tasks`, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(formObj)
            });

            if (!response.ok) {
                document.getElementById("createTask_error").innerHTML = "Es ist ein Fehler bei der Erstellung der Aufgabe aufgetreten!";
            } else {
                await updateListOrBoardView();
                document.getElementById("createTaskModalClose").click();
                document.getElementById("createTask_error").innerHTML = "";

                const userNotifications = await fetchUserNotifications();
                fillMailbox(userNotifications);
            }
        } catch (error) {
            document.getElementById("createTask_error").innerHTML = "Es ist ein Fehler bei der Erstellung der Aufgabe aufgetreten!";
            console.error(error);
        }
    });

    taskCreateForm.dataset.listenerAdded = true;
}

// ===============================================================
// Edit Task Section
// ===============================================================

function openTaskModal(task) {
    if (!task) {
        console.error('Task is undefined or null');
        return;
    }
    document.getElementById("editTask_error").innerText = "";

    if (!('parentTask' in task) || !('blockedBy' in task) || !('subTasks' in task)) {
        console.error('Task object is missing required properties');
        return;
    }

    currentEditingTaskId = String(task.id);

    // Fill Modal values
    const editTaskModalBody = document.getElementById("editTaskModalBody");
    editTaskModalBody.setAttribute("taskId", String(task.id));
    document.getElementById("editTask_name").value = unescapeHTML(task.title);
    document.getElementById("editTask_description").value = unescapeHTML(task.description);
    document.getElementById("editTask_status").value = task.status;
    assignedUsersEditTask = new Set(task.assignedUsers.map(String));

    // Date fill logic
    document.getElementById("editTask_dueDate").value = task.dueDate;
    const today = new Date().toISOString().split("T")[0];
    document.getElementById("editTask_dueDate").setAttribute("min", today);
    document.getElementById("editTask_creationDateTime").value = apiDateToFrontendDate(task.creationDateTime);

    // Initialize selections
    selectedParentTask = task.parentTask ? String(task.parentTask) : null;
    selectedBlockingTask = new Set(task.blockedBy.map(String));
    assignedSubtasks = new Set(task.subTasks.map(String));

    // Toggle visibility based on task selections
    toggleParentSubtaskSelectionVisibility();

    // Render lists with the current selections
    renderUserListEditTask();
    renderParentTaskListEdit();
    renderSubtaskListEdit();
    renderBlockingTaskListEdit();

    // Handle start and completion dates
    const startDateTimeDiv = document.getElementById("editTask_startDateTimeDiv");
    const completionDateTimeDiv = document.getElementById("editTask_completionDateTimeDiv");

    if (task.startDateTime !== null) {
        document.getElementById("editTask_startDateTime").value = apiDateToFrontendDate(task.startDateTime);
        startDateTimeDiv.style.display = "block";
    } else {
        startDateTimeDiv.style.display = "none";
    }

    if (task.completionDateTime !== null) {
        document.getElementById("editTask_completionDateTime").value = apiDateToFrontendDate(task.completionDateTime);
        completionDateTimeDiv.style.display = "block";
    } else {
        completionDateTimeDiv.style.display = "none";
    }

    fillTaskDeleteModal(task);
}

function toggleParentSubtaskSelectionVisibility() {
    const parentTaskField = document.getElementById("editParentTaskSearch").parentElement;
    const subtaskField = document.getElementById("editSubtasksSearch").parentElement;

    if (assignedSubtasks.size > 0) {
        // Wenn Subtasks ausgewählt sind, blende die Parenttask-Auswahl aus
        parentTaskField.style.display = "none";
        subtaskField.style.display = "block";
    } else if (selectedParentTask !== null) {
        // Wenn eine Parenttask ausgewählt ist, blende die Subtask-Auswahl aus
        subtaskField.style.display = "none";
        parentTaskField.style.display = "block";
    } else {
        // Wenn weder Parenttask noch Subtasks ausgewählt sind, zeige beide Felder an
        parentTaskField.style.display = "block";
        subtaskField.style.display = "block";
    }
}

// ===========================================
// Render Functions for Edit Task Modal
// ===========================================

function renderUserListEditTask(filter = "") {
    const userListEditTask = document.getElementById("editTask_userList");
    userListEditTask.innerHTML = "";

    const filteredUsers = projectDetails.users.filter(user =>
        user.name.toLowerCase().includes(filter.toLowerCase()) || user.emailAddress.toLowerCase().includes(filter.toLowerCase())
    );

    document.getElementById("editTask_userInfo").innerText =
        assignedUsersEditTask.size > 0 ? `${assignedUsersEditTask.size} Mitarbeiter ausgewählt` : "Kein Mitarbeiter ausgewählt";

    filteredUsers.forEach(user => {
        const userListItem = document.createElement("li");
        userListItem.className = "list-group-item";
        const userNameSpan = document.createElement("span");
        userNameSpan.textContent = user.name;
        const userEmailSpan = document.createElement("span");
        userEmailSpan.textContent = ` (${user.emailAddress})`;
        userEmailSpan.style.color = "gray";
        userListItem.appendChild(userNameSpan);
        userListItem.appendChild(userEmailSpan);
        userListItem.dataset.id = String(user.id);

        if (assignedUsersEditTask.has(String(user.id))) {
            userListItem.classList.add("userListSelected");
        }

        userListItem.addEventListener("click", () => {
            userListItem.classList.toggle("userListSelected");
            const id = userListItem.dataset.id;

            if (userListItem.classList.contains("userListSelected")) {
                assignedUsersEditTask.add(id);
            } else {
                assignedUsersEditTask.delete(id);
            }

            document.getElementById("editTask_userInfo").innerText =
                assignedUsersEditTask.size > 0 ? `${assignedUsersEditTask.size} Mitarbeiter ausgewählt` : "Kein Mitarbeiter ausgewählt";
        });

        userListEditTask.appendChild(userListItem);
    });
}

function renderParentTaskListEdit(filter = "") {
    const parentTaskListView = document.getElementById("editParentTaskList");
    parentTaskListView.innerHTML = "";

    if (!projectTaskDetails) {
        console.error('projectTaskDetails is not initialized');
        return;
    }

    const filteredTasks = projectTaskDetails.filter(task =>
        (unescapeHTML(task.title.toLowerCase()).includes(filter.toLowerCase()) || String("#" + task.id).includes(filter.toLowerCase())) &&
        task.parentTask === null &&
        String(task.id) !== currentEditingTaskId
    );

    filteredTasks.forEach(task => {
        const taskItem = createTaskListItem(task);

        if (selectedParentTask === String(task.id)) {
            taskItem.classList.add("parentTaskSelected");
            document.getElementById("editParentTaskInfo").innerText = "Übergeordnete Aufgabe ausgewählt";
        }

        taskItem.addEventListener("click", () => {
            const isSelected = taskItem.classList.contains("parentTaskSelected");

            document.querySelectorAll(".parentTaskSelected").forEach(el => el.classList.remove("parentTaskSelected"));

            if (isSelected) {
                selectedParentTask = null;
                document.getElementById("editParentTaskInfo").innerText = "Keine übergeordnete Aufgabe ausgewählt";
            } else {
                taskItem.classList.add("parentTaskSelected");
                selectedParentTask = task.id;
                document.getElementById("editParentTaskInfo").innerText = "Übergeordnete Aufgabe ausgewählt";
            }

            // Aktualisiere die Sichtbarkeit
            toggleParentSubtaskSelectionVisibility();
        });

        parentTaskListView.appendChild(taskItem);
    });
}

function renderSubtaskListEdit(filter = "") {
    const subtaskListView = document.getElementById("editSubtasksList");
    subtaskListView.innerHTML = "";

    const availableTasks = projectTaskDetails.filter(task =>
        (unescapeHTML(task.title.toLowerCase()).includes(filter.toLowerCase()) || String("#" + task.id).includes(filter.toLowerCase())) &&
        task.subTasks.length === 0 &&
        String(task.id) !== currentEditingTaskId
    );

    availableTasks.forEach(task => {
        const taskItem = createTaskListItem(task);

        if (assignedSubtasks.has(String(task.id))) {
            taskItem.classList.add("subtaskSelected");
        }

        taskItem.addEventListener("click", () => {
            taskItem.classList.toggle("subtaskSelected");
            const id = taskItem.dataset.id;

            if (taskItem.classList.contains("subtaskSelected")) {
                assignedSubtasks.add(id);
            } else {
                assignedSubtasks.delete(id);
            }

            document.getElementById("editSubtasksInfo").innerText =
                assignedSubtasks.size > 0 ? `${assignedSubtasks.size} Subtasks ausgewählt` : "Keine Subtasks ausgewählt";

            // Aktualisiere die Sichtbarkeit
            toggleParentSubtaskSelectionVisibility();
        });

        subtaskListView.appendChild(taskItem);
    });
    document.getElementById("editSubtasksInfo").innerText =
        assignedSubtasks.size > 0 ? `${assignedSubtasks.size} Subtasks ausgewählt` : "Keine Subtasks ausgewählt";
}

function renderBlockingTaskListEdit(filter = "") {
    const blockingTaskListView = document.getElementById("editBlockedByList");
    blockingTaskListView.innerHTML = "";

    const availableTasks = projectTaskDetails.filter(task =>
        (unescapeHTML(task.title.toLowerCase()).includes(filter.toLowerCase()) || String("#" + task.id).includes(filter.toLowerCase())) &&
        String(task.id) !== currentEditingTaskId
    );

    availableTasks.forEach(task => {
        const taskItem = createTaskListItem(task);

        if (selectedBlockingTask.has(String(task.id))) {
            taskItem.classList.add("blockedByTaskSelected");
        }

        taskItem.addEventListener("click", () => {
            taskItem.classList.toggle("blockedByTaskSelected");
            const id = taskItem.dataset.id;

            if (taskItem.classList.contains("blockedByTaskSelected")) {
                selectedBlockingTask.add(id);
            } else {
                selectedBlockingTask.delete(id);
            }

            document.getElementById("editBlockedByInfo").innerText =
                selectedBlockingTask.size > 0 ? `${selectedBlockingTask.size} blockierende Aufgaben ausgewählt` : "Keine blockierenden Aufgaben ausgewählt";
        });

        blockingTaskListView.appendChild(taskItem);
    });
    document.getElementById("editBlockedByInfo").innerText =
        selectedBlockingTask.size > 0 ? `${selectedBlockingTask.size} blockierende Aufgaben ausgewählt` : "Keine blockierenden Aufgaben ausgewählt";
}

function createTaskListItem(task) {
    const taskItem = document.createElement("li");
    taskItem.className = "list-group-item";
    taskItem.style.display = "flex";
    taskItem.style.justifyContent = "space-between";

    const titleSpan = document.createElement("span");
    titleSpan.innerHTML = task.title;

    const idLink = document.createElement("a");
    idLink.textContent = "#" + task.id;
    idLink.href = "#" + task.id;
    idLink.style.color = "gray";
    idLink.addEventListener("click", (e) => {
        e.preventDefault();
        openTaskModal(task);
    });

    taskItem.appendChild(titleSpan);
    taskItem.appendChild(idLink);
    taskItem.dataset.id = String(task.id);
    return taskItem;
}

// ===============================
// Event Listener: Edit Task Form
// ===============================

document.getElementById("editTaskForm").addEventListener("submit", async function (e) {
    e.preventDefault();
    const formData = new FormData(e.target);

    const formObj = Object.fromEntries(formData.entries());
    formObj["assignedUsers"] = Array.from(assignedUsersEditTask);
    formObj["subTasks"] = Array.from(assignedSubtasks);
    formObj["parentTask"] = selectedParentTask;
    formObj["blockedBy"] = Array.from(selectedBlockingTask);

    const id = document.getElementById("editTaskModalBody").getAttribute("taskId");
    const response = await fetch(`/api/v1/projects/${projectDetails.id}/tasks/${id}`, {
        method: "PATCH",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(formObj)
    });

    if (response.ok) {
        document.getElementById("editTask_close").click();
        await updateListOrBoardView();
        currentEditingTaskId = null;

        const userNotifications = await fetchUserNotifications();
        fillMailbox(userNotifications);
    } else {
        const customError = await response.text();
        const isPlainText = !/<[a-z][\s\S]*>/i.test(customError);
        document.getElementById("editTask_error").innerText = (customError === null || customError === "") && isPlainText ?
            "Es ist ein Fehler beim Bearbeiten der Aufgabe aufgetreten!" :
            customError;
    }
});

// ===================================
// Event Listener: Edit Task Search
// ===================================

document.getElementById("userEditSearchTask").addEventListener("input", (e) => {
    renderUserListEditTask(e.target.value);
});
document.getElementById("editSubtasksSearch").addEventListener("input", (e) => {
    renderSubtaskListEdit(e.target.value);
});
document.getElementById("editBlockedBySearch").addEventListener("input", (e) => {
    renderBlockingTaskListEdit(e.target.value);
});
document.getElementById("editParentTaskSearch").addEventListener("input", (e) => {
    renderParentTaskListEdit(e.target.value);
});

// ===================================
// Delete Task Modal
// ===================================

document.getElementById("confirmDeleteTaskButton").addEventListener("click", async function () {
    const taskId = document.getElementById("editTaskModalBody").getAttribute("taskId")

    // Send DELETE request to the API
    const response = await fetch(`/api/v1/projects/${projectDetails.id}/tasks/${taskId}`, {
        method: "DELETE",
        headers: {
            "Content-Type": "application/json",
        },
    });

    if (response.ok) {
        await updateListOrBoardView()
        document.getElementById("deleteTaskModalClose").click();
    } else {
        document.getElementById("deleteTask_error").innerHTML = "Es ist ein Fehler beim Löschen der Aufgabe aufgetreten!"
    }
});

async function fillTaskDeleteModal(task) {
    // Update modal title and subtasks
    document.getElementById("taskDeleteTitle").innerHTML = task.title;
    document.getElementById("deleteTask_error").innerHTML = ""

    const subtasks = task.subTasks;
    const subtaskListContainer = document.getElementById("subtaskListContainer");
    const subtaskList = document.getElementById("subtaskList");
    subtaskList.innerHTML = "";  // Clear existing subtasks

    if (subtasks.length > 0) {
        subtasks.forEach(subtaskId => {
            const subtask = projectTaskDetails.find(element => element.id === subtaskId);
            const li = document.createElement("li");
            li.className = "list-group-item";
            li.innerHTML = subtask.title;
            subtaskList.appendChild(li);
        });
        subtaskListContainer.style.display = "block";
    } else {
        subtaskListContainer.style.display = "none";
    }
}
