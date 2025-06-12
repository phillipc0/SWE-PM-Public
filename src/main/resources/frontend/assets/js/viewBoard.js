// ============================
// Global Variable Declarations
// ============================

let showingOnlyMyTasks = false;
let boardLists = {};

// ============================
// Main Function: openBoardView
// ============================

async function openBoardView(projectJSON, taskJSON, userJSON) {
    setNavbarTitle(projectJSON.name);

    // Assign variables
    projectDetails = projectJSON;
    projectTaskDetails = taskJSON;
    userDetails = userJSON;

    // Create mappings
    boardLists = {
        "TODO": document.getElementById("boardList_todo"),
        "ASSIGNED": document.getElementById("boardList_assigned"),
        "IN_PROGRESS": document.getElementById("boardList_inprogress"),
        "READY_TO_REVIEW": document.getElementById("boardList_readytoreview"),
        "IN_REVIEW": document.getElementById("boardList_inreview"),
        "DONE": document.getElementById("boardList_done")
    };

    if (showingOnlyMyTasks) {
        document.getElementById("boardMyTasks").classList.add("btn-list-view-variant");
        document.getElementById("boardAllTasks").classList.remove("btn-list-view-variant");
    }

    // todo: check if necessary
    // Clear all existing timeouts
    let highestTimeoutId = setTimeout(";");
    for (let i = 0; i < highestTimeoutId; i++) {
        // Nur Timer löschen bis auf die Notification-Timer
        if (!notificationTimers.includes(i)) {
            clearTimeout(i);
        }
    }

    // Set up periodic task fetching every 10 seconds
    const timedRefresh = setInterval(fetchTasksAndDisplayBoard, 10000);

    // Get and show all Tasks on board on first load
    await fetchTasksAndDisplayBoard("");

    // ========================
    // Hyper Move System Jan R.
    // ========================

    const sortableOptions = {
        group: 'shared',
        animation: 150,
        onStart: function (evt) {
            Object.values(boardLists).forEach(list => list.classList.add("draggableHighlight"));
            evt.item.classList.add("taskItemActiveDragging");
            document.querySelectorAll(".hyperMove").forEach(hyperMoveElement => hyperMoveElement.classList.add("hyperMoveShow"));
        },
        onEnd: async function (evt) {
            Object.values(boardLists).forEach(list => list.classList.remove("draggableHighlight"));

            const itemId = evt.item.getAttribute("data-id");
            const targetList = evt.to.getAttribute("data-status");

            await moveTaskToCategory(itemId, targetList);

            evt.item.classList.remove("taskItemActiveDragging");
            document.querySelectorAll(".hyperMove").forEach(el => el.classList.remove("hyperMoveShow"));

            const userNotifications = await fetchUserNotifications();
            fillMailbox(userNotifications);
        }
    };

    // Initialize Sortable for each draggable area
    document.querySelectorAll('.hyperMove, .draggableListCustom').forEach(el => new Sortable(el, sortableOptions));

    async function moveTaskToCategory(taskID, categoryName) {
        const updateTask = {status: categoryName};
        try {
            const response = await fetch(`/api/v1/projects/${projectDetails.id}/tasks/${taskID}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(updateTask)
            });

            if (!response.ok) {
                // Versuche, die Fehlermeldung aus der Antwort zu extrahieren
                const customError = await response.text();
                const isPlainText = !/<[a-z][\s\S]*>/i.test(customError);
                const errorMessage = isPlainText && customError && response.status === 422
                    ? customError
                    : "Es ist ein Fehler beim Aktualisieren des Aufgabenstatus aufgetreten!";

                throw new Error(errorMessage);
            }

            const taskElement = projectTaskDetails.find(task => String(task.id) === taskID);
            if (taskElement) taskElement.status = categoryName;

            displayFilteredTasksBoard(document.getElementById("taskFilterInput").value);
        } catch (error) {
            console.error(error);
            // Zeigt die Fehlermeldung in einer Benachrichtigung an
            showNotification(error.message);
            displayFilteredTasksBoard(document.getElementById("taskFilterInput").value);
        }
    }

    // ==================================
    // Event Listener: Show only my tasks
    // ==================================

    document.getElementById("boardAllTasks").addEventListener("click", () => {

        const filter = document.getElementById("taskFilterInput").value;

        showingOnlyMyTasks = false;

        displayFilteredTasksBoard(filter, showingOnlyMyTasks);

        document.getElementById("boardAllTasks").classList.add("btn-list-view-variant");
        document.getElementById("boardMyTasks").classList.remove("btn-list-view-variant");
    });

    document.getElementById("boardMyTasks").addEventListener("click", () => {

        const filter = document.getElementById("taskFilterInput").value;

        showingOnlyMyTasks = true;

        displayFilteredTasksBoard(filter, showingOnlyMyTasks);

        document.getElementById("boardMyTasks").classList.add("btn-list-view-variant");
        document.getElementById("boardAllTasks").classList.remove("btn-list-view-variant");
    });

}

// =================================
// Show Tasks in Board View
// =================================

async function fetchTasksAndDisplayBoard() {
    try {
        const response = await fetch(`/api/v1/projects/${projectDetails.id}/tasks`);
        if (!response.ok) throw new Error("Failed to fetch tasks");
        projectTaskDetails = await response.json();
        displayFilteredTasksBoard(document.getElementById("taskFilterInput").value);
    } catch (error) {
        console.error("Error fetching tasks:", error);
    }
}

function displayFilteredTasksBoard(filter) {
    document.querySelectorAll('.taskPreviewBoardItem').forEach(e => e.remove());

    const filteredTasks = projectTaskDetails.filter(task =>
        (unescapeHTML(task.title.toLowerCase()).includes(filter.toLowerCase()) ||
            String("#" + task.id).includes(filter.toLowerCase()) ||
            apiDateToFrontendDate(task.dueDate).includes(filter.toLowerCase())
        ) &&
        (!showingOnlyMyTasks || task.assignedUsers.includes(userDetails.id))
    );

    // Sort tasks by dueDate
    filteredTasks.sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate));

    filteredTasks.forEach(task => {
        let userString = task.assignedUsers.map(id => userIDMap[id]).join("\n");

        const taskItem = createTaskItem(task, userString);
        boardLists[task.status].appendChild(taskItem);
    });
}

// =================================
// Create TaskCard in Board View
// =================================

function createTaskItem(task, userString) {
    const taskItem = document.createElement("li");
    taskItem.setAttribute("data-id", String(task.id));
    taskItem.className = "list-group-item taskPreviewBoardItem boardTaskDefault";
    taskItem.setAttribute("data-bs-toggle", "modal");
    taskItem.setAttribute("data-bs-target", "#taskDetailsModal");

    taskItem.addEventListener("click", () => {
        openTaskModal(task);
        renderUserListEditTask();
    });

    if (task.parentTask != null) {
        taskItem.addEventListener("mouseover", () => highlightParentTask(task.parentTask));
        taskItem.addEventListener("mouseleave", unhighlightTasks);
    } else if (task.subTasks.length !== 0) {
        taskItem.addEventListener("mouseover", () => highlightTasks(task.subTasks.map(String), "boardSubTaskHighlight"));
        taskItem.addEventListener("mouseleave", unhighlightTasks);
    }

    if (task.blockedBy.length > 0) {
        taskItem.addEventListener("mouseover", () => highlightTasks(task.blockedBy.map(String), "boardBlockedByHighlight"));
        taskItem.addEventListener("mouseleave", unhighlightTasks);
    }

    taskItem.appendChild(createTaskCardBody(task, userString));
    return taskItem;
}

function createTaskCardBody(task, userString) {
    const body = document.createElement("div");
    body.className = "card-body d-flex flex-column flex-grow-1";

    const head = document.createElement("div");
    head.className = "d-flex justify-content-between align-items-center";

    const name = document.createElement("h5");
    name.className = "card-title";
    name.id = "taskTitle";
    truncateText(name, task.title, 160);

    const taskIdSpan = document.createElement("span");
    taskIdSpan.className = "text-muted";
    taskIdSpan.style.color = "gray";
    taskIdSpan.textContent = ` #${task.id}`;

    name.appendChild(taskIdSpan);
    head.appendChild(name);

    const userDiv = document.createElement("div");
    userDiv.className = "d-inline-flex align-items-end";
    userDiv.innerHTML = `<h5 class="mb-0 ml-2">${task.assignedUsers.length}</h5>
                             <img src="/assets/svg/person.svg" id="personSvg" alt="Zugeordnete Mitarbeiter" title="Zugeordnete Mitarbeiter:\n${userString}" width="24" height="24">`;
    head.appendChild(userDiv);
    body.appendChild(head);

    const dueDiv = document.createElement("div");
    dueDiv.className = "d-inline-flex align-items-end";
    dueDiv.innerHTML = `<img src="/assets/svg/calendar-check.svg" id="zieldatumSvg" alt="Zieldatum" width="16" height="16">
                            <h6 class="mb-0 ml-2 text-body-secondary m-1 boardTaskText">${apiDateToFrontendDate(task.dueDate)}</h6>`;
    body.appendChild(dueDiv);

    if (task.parentTask === null && task.subTasks.length > 0) {
        const subtaskDiv = document.createElement("div");
        subtaskDiv.className = "d-inline-flex align-items-end";
        const subtaskTitles = task.subTasks.map(id => taskIDMap[String(id)]).join("\n");
        subtaskDiv.innerHTML = `<img src="/assets/svg/inboxes-fill.svg" alt="Unteraufgaben" id="inboxesFillSvg" title="Unteraufgaben:\n${subtaskTitles}" width="16" height="16">
                                    <h6 class="mb-0 ml-2 text-body-secondary m-1 boardTaskText">${task.subTasks.length}</h6>`;
        body.appendChild(subtaskDiv);
    }

    return body;
}

// =================================
// Helper Functions for Highlighting
// =================================

function highlightParentTask(highlightID) {
    const parentTaskElement = document.querySelector(`[data-id="${highlightID}"]`);
    if (parentTaskElement) {
        parentTaskElement.classList.remove("boardTaskDefault");
        parentTaskElement.classList.add("boardParentTaskHighlight");
    }
}

function highlightTasks(highlightIDs, highlightClass) {
    highlightIDs.forEach(id => {
        const taskElement = document.querySelector(`[data-id="${id}"]`);
        if (taskElement) {
            taskElement.classList.remove("boardTaskDefault");
            taskElement.classList.add(highlightClass);
        }
    });
}

function unhighlightTasks() {
    document.querySelectorAll(".boardSubTaskHighlight, .boardParentTaskHighlight, .boardBlockedByHighlight").forEach(el => {
        el.classList.remove("boardSubTaskHighlight", "boardParentTaskHighlight", "boardBlockedByHighlight");
        el.classList.add("boardTaskDefault");
    });
}