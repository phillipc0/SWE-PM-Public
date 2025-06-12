// ============================
// Global Variable Declarations
// ============================

let currentTasksView = "all";  // Tracks if we're viewing completed tasks or all tasks
let filteredTasks = null;
let currentSortColumn = null;
let isAscending = true;
let collapsedState = {};

let statusMapping = {
    TODO: "To Do",
    ASSIGNED: "Zugewiesen",
    IN_PROGRESS: "In Bearbeitung",
    READY_TO_REVIEW: "Ready to Review",
    IN_REVIEW: "In Review",
    DONE: "Fertig"
};
let statusOrder = {
    "TODO": 1, "ASSIGNED": 2, "IN_PROGRESS": 3, "READY_TO_REVIEW": 4, "IN_REVIEW": 5, "DONE": 6
};

// ============================
// Main Function: openListView
// ============================

async function openListView(projectJSON, taskJSON, userJSON) {
    projectDetails = projectJSON;
    currentUser = userJSON;
    setNavbarTitle(projectDetails.name);

    if (currentTasksView === "completed") {
        document.getElementById("filterCompletedTasks").classList.add("btn-list-view-variant");
        document.getElementById("filterAllTasks").classList.remove("btn-list-view-variant");
        document.getElementById("filterMyTasks").classList.remove("btn-list-view-variant");
    } else if (currentTasksView === "user") {
        document.getElementById("filterMyTasks").classList.add("btn-list-view-variant");
        document.getElementById("filterAllTasks").classList.remove("btn-list-view-variant");
        document.getElementById("filterCompletedTasks").classList.remove("btn-list-view-variant");
    }

    document.getElementById("tasksTable").style.setProperty("--bs-table-bg", "unset");

    projectTaskDetails = taskJSON;
    projectTaskDetails.sort((taskA, taskB) => taskB.id - taskA.id); // Default sorting by task id (highest id should be on top)

    filteredTasks = projectTaskDetails;  // Start by showing all tasks

    currentSortColumn = null;

    // Set up periodic task fetching every 10 seconds
    const timedRefresh = window.setInterval(fetchTasksAndDisplayList, 10000);

    // Populate on first load
    await fetchTasksAndDisplayList();

    // =====================================
    // List: all tasks vs completed tasks vs current user tasks
    // =====================================
    document.getElementById("filterAllTasks").addEventListener("click", () => {
        currentTasksView = "all";
        filteredTasks = projectTaskDetails;

        document.getElementById("filterAllTasks").classList.add("btn-list-view-variant");
        document.getElementById("filterCompletedTasks").classList.remove("btn-list-view-variant");
        document.getElementById("filterMyTasks").classList.remove("btn-list-view-variant");

        populateTasksTable(filteredTasks, false);

        const filterValue = document.getElementById("taskFilterInput").value;
        if (filterValue) {
            displayFilteredTasksList(filterValue);
        }
    });

    document.getElementById("filterCompletedTasks").addEventListener("click", () => {
        currentTasksView = "completed";

        filteredTasks = projectTaskDetails.filter(task => {
            if (task.status === "DONE") {
                return true;
            }
            // Show subtasks that are DONE but their parent task is not DONE
            if (task.subTasks && task.subTasks.length > 0) {
                return task.subTasks.some(subtaskId => {
                    const subtask = projectTaskDetails.find(t => t.id === subtaskId);
                    return subtask && subtask.status === "DONE";
                });
            }
            return false;  // Skip tasks that don't meet the criteria
        });

        document.getElementById("filterCompletedTasks").classList.add("btn-list-view-variant");
        document.getElementById("filterAllTasks").classList.remove("btn-list-view-variant");
        document.getElementById("filterMyTasks").classList.remove("btn-list-view-variant");

        // Sort by completion date descending (most recent first)
        filteredTasks.sort((taskA, taskB) => {
            const dateA = taskA.completionDate ? new Date(taskA.completionDate) : new Date(0);
            const dateB = taskB.completionDate ? new Date(taskB.completionDate) : new Date(0);
            return dateB - dateA;
        });

        populateTasksTable(filteredTasks, true);

        const filterValue = document.getElementById("taskFilterInput").value;
        if (filterValue) {
            displayFilteredTasksList(filterValue);
        }
    });

    document.getElementById("filterMyTasks").addEventListener("click", () => {

        currentTasksView = "user";

        filteredTasks = projectTaskDetails.filter(task => {

            if (task.assignedUsers.includes(currentUser.id)) {
                return true;
            }

            // Show subtasks that are DONE but their parent task is not DONE
            if (task.subTasks && task.subTasks.length > 0) {
                return task.subTasks.some(subtaskId => {
                    const subtask = projectTaskDetails.find(t => t.id === subtaskId);
                    return subtask && subtask.assignedUsers.includes(currentUser.id);
                });
            }
            return false;  // Skip tasks that don't meet the criteria
        });

        document.getElementById("filterMyTasks").classList.add("btn-list-view-variant");
        document.getElementById("filterAllTasks").classList.remove("btn-list-view-variant");
        document.getElementById("filterCompletedTasks").classList.remove("btn-list-view-variant");

        // Sort by completion date descending (most recent first)
        filteredTasks.sort((taskA, taskB) => {
            const dateA = taskA.completionDate ? new Date(taskA.completionDate) : new Date(0);
            const dateB = taskB.completionDate ? new Date(taskB.completionDate) : new Date(0);
            return dateB - dateA;
        });

        populateTasksTable(filteredTasks, false);

        const filterValue = document.getElementById("taskFilterInput").value;
        if (filterValue) {
            displayFilteredTasksList(filterValue);
        }
    });

    // =====================================
    // List: sort table after column
    // =====================================
    document.getElementById("title-header").addEventListener("click", () => sortAndPopulateTasksTable(0, projectTaskDetails));
    document.getElementById("assigned-header").addEventListener("click", () => sortAndPopulateTasksTable(1, projectTaskDetails));
    document.getElementById("status-header").addEventListener("click", () => sortAndPopulateTasksTable(2, projectTaskDetails));
    document.getElementById("due-date-header").addEventListener("click", () => sortAndPopulateTasksTable(3, projectTaskDetails));
    document.getElementById('completion-date-header').addEventListener('click', () => {
        sortAndPopulateTasksTable(4, projectTaskDetails, true);  // Sort by completion date when in completed tasks view
    });
}

// =================================
// Show Tasks in List
// =================================

async function fetchTasksAndDisplayList() {
    const updatedTasks = await fetch(`/api/v1/projects/${projectDetails.id}/tasks`);
    projectTaskDetails = await updatedTasks.json();

    if (currentTasksView === "completed") {
        filteredTasks = projectTaskDetails.filter(task => {
            if (task.status === "DONE") {
                return true;
            }
            if (task.subTasks && task.subTasks.length > 0) {
                return task.subTasks.some(subtaskId => {
                    const subtask = projectTaskDetails.find(t => t.id === subtaskId);
                    return subtask && subtask.status === "DONE";
                });
            }
            return false;
        });
    } else if (currentTasksView === "user") {
        filteredTasks = projectTaskDetails.filter(task => {

            if (task.assignedUsers.includes(currentUser.id)) {
                return true;
            }

            // Show subtasks that are DONE but their parent task is not DONE
            if (task.subTasks && task.subTasks.length > 0) {
                return task.subTasks.some(subtaskId => {
                    const subtask = projectTaskDetails.find(t => t.id === subtaskId);
                    return subtask && subtask.assignedUsers.includes(currentUser.id);
                });
            }
            return false;
        });
    } else {
        filteredTasks = projectTaskDetails;
    }

    if (currentSortColumn !== null) {
        sortAndPopulateTasksTable(currentSortColumn, false);
    } else {
        populateTasksTable(filteredTasks, currentTasksView === "completed");
    }

    const filterValue = document.getElementById("taskFilterInput").value;
    if (filterValue) {
        displayFilteredTasksList(filterValue);
    }
}

function displayFilteredTasksList(filter) {
    const lowerFilter = filter.toLowerCase();

    // Track expanded state of each parent task before filtering
    const expandedTasks = {};
    document.querySelectorAll(".collapse").forEach(collapseElement => {
        const parentId = collapseElement.id.split("-")[1];
        expandedTasks[parentId] = collapseElement.classList.contains("show");
    });

    let tasksToSearch = [];
    if (currentTasksView === "completed") {
        tasksToSearch = projectTaskDetails.filter(task => {
            if (task.status === "DONE") {
                return true;
            }
            if (task.subTasks && task.subTasks.length > 0) {
                return task.subTasks.some(subtaskId => {
                    const subtask = projectTaskDetails.find(t => t.id === subtaskId);
                    return subtask && subtask.status === "DONE";
                });
            }
            return false;
        });
    } else if (currentTasksView === "user") {
        tasksToSearch = projectTaskDetails.filter(task => {

            if (task.assignedUsers.includes(currentUser.id)) {
                return true;
            }

            // Show subtasks that are DONE but their parent task is not DONE
            if (task.subTasks && task.subTasks.length > 0) {
                return task.subTasks.some(subtaskId => {
                    const subtask = projectTaskDetails.find(t => t.id === subtaskId);
                    return subtask && subtask.assignedUsers.includes(currentUser.id);
                });
            }
            return false;
        });
    } else {
        tasksToSearch = projectTaskDetails;
    }

    filteredTasks = tasksToSearch.filter(task => {
        const titleMatch = unescapeHTML(task.title.toLowerCase()).includes(lowerFilter);
        const idMatch = `#${task.id}`.includes(lowerFilter);

        const frontendStatus = statusMapping[task.status];
        const statusMatch = frontendStatus.toLowerCase().includes(lowerFilter);
        const userMatch = task.assignedUsers.map(user => userIDMap[user].toLowerCase()).some(user => user.includes(lowerFilter));

        const dueDateFormatted = apiDateToFrontendDate(task.dueDate);
        const dueDateTimeMatch = dueDateFormatted.includes(lowerFilter);

        const completionDateFormatted = apiDateToFrontendDate(task.completionDateTime);
        const completionDateMatch = currentTasksView === "completed" && completionDateFormatted.includes(lowerFilter);

        let taskMatches = titleMatch || idMatch || statusMatch || userMatch || dueDateTimeMatch || completionDateMatch;

        let subtaskMatches = false;
        if (task.subTasks && task.subTasks.length > 0) {
            const relatedSubtasks = tasksToSearch.filter(subtask => task.subTasks.includes(subtask.id));
            relatedSubtasks.forEach(subtask => {
                const subtaskTitleMatch = unescapeHTML(subtask.title.toLowerCase()).includes(lowerFilter);
                const subtaskIdMatch = `#${subtask.id}`.includes(lowerFilter);
                const subTaskfrontendStatus = statusMapping[subtask.status];
                const subtaskStatusMatch = subTaskfrontendStatus.toLowerCase().includes(lowerFilter);
                const subtaskUserMatch = subtask.assignedUsers && subtask.assignedUsers.map(user => userIDMap[user].toLowerCase()).some(user => user.includes(lowerFilter));

                const subtaskDueDateFormatted = apiDateToFrontendDate(subtask.dueDate);
                const subtaskDueDateMatch = subtaskDueDateFormatted.includes(lowerFilter);

                const subtaskCompletionDateFormatted = apiDateToFrontendDate(subtask.completionDateTime);
                const subtaskCompletionDateMatch = currentTasksView && subtaskCompletionDateFormatted.includes(lowerFilter);

                if (subtaskTitleMatch || subtaskIdMatch || subtaskStatusMatch || subtaskUserMatch || subtaskDueDateMatch || subtaskCompletionDateMatch) {
                    subtaskMatches = true;
                }
            });
        }

        return taskMatches || subtaskMatches;
    });

    populateTasksTable(filteredTasks, currentTasksView === "completed");

    Object.keys(expandedTasks).forEach(parentId => {
        const collapseElement = document.getElementById(`subtasks-${parentId}`);
        if (collapseElement && expandedTasks[parentId]) {
            new bootstrap.Collapse(collapseElement, {toggle: true}).show();
        }
    });
}

function populateTasksTable(tasks, includeCompletionDate = false) {
    console.log(includeCompletionDate)
    const tableBody = document.getElementById("tasksTable").querySelector("tbody");
    tableBody.innerHTML = "";

    // Always include the completion date column but toggle its visibility
    const completionDateHeader = document.getElementById("completion-date-header");
    completionDateHeader.style.display = includeCompletionDate ? "" : "none";

    if (tasks.length === 0) {
        const noTaskRow = document.createElement("tr");
        const noTaskCell = document.createElement("td");
        noTaskCell.colSpan = 6;
        noTaskCell.textContent = "Es sind keine Aufgaben vorhanden.";
        noTaskCell.classList.add("text-center");
        noTaskRow.appendChild(noTaskCell);
        tableBody.appendChild(noTaskRow);
        return;
    }

    const topLevelTasks = tasks.filter(task => task.parentTask === null);
    const subTasks = tasks.filter(task => task.parentTask !== null);

    let rowCounter = 0;

    topLevelTasks.forEach(task => {
        const row = document.createElement("tr");
        row.classList.add("row-clickable");

        if (rowCounter % 2 === 0) {
            row.classList.add("white-row");
        } else {
            row.classList.add("light-gray-row");
        }
        rowCounter++;

        const toggleCell = document.createElement("td");
        const titleCell = document.createElement("td");
        const assigneesCell = document.createElement("td");
        const statusCell = document.createElement("td");
        const dueDateCell = document.createElement("td");
        const completionDateCell = document.createElement("td");

        const hasSubtasks = subTasks.some(subtask => subtask.parentTask === task.id);
        const isCollapsed = collapsedState[task.id] || false;  // Default is not collapsed (false)
        const arrow = isCollapsed ? '▸' : '▾';

        if (hasSubtasks) {
            const toggleButton = document.createElement("button");
            toggleButton.classList.add("toggle-arrow");
            toggleButton.innerHTML = arrow;
            toggleButton.setAttribute("data-task-id", task.id);
            toggleButton.setAttribute("data-collapsed", isCollapsed);

            toggleButton.addEventListener("click", function (event) {
                event.stopPropagation(); // Prevent modal from opening on toggle click
                toggleSubtasks(task.id);
            });

            toggleCell.appendChild(toggleButton);
        }

        toggleCell.style.width = "10px";
        toggleCell.style.maxWidth = "10px";
        titleCell.innerHTML = task.title;
        titleCell.style.width = "25%";
        titleCell.style.maxWidth = "25%";

        // Append task ID in gray, formatted as #x
        const taskIdSpan = document.createElement("span");
        taskIdSpan.classList.add("text-muted");
        taskIdSpan.classList.add("list-id-text-color");
        taskIdSpan.textContent = ` #${task.id}`;
        titleCell.appendChild(taskIdSpan);

        let userString = task.assignedUsers
            .map(user => userIDMap[user])
            .filter(Boolean)  // Filtere ungültige Benutzer (die nicht in userIDMap existieren)
            .sort()
            .join(", ");
        assigneesCell.textContent = userString;
        statusCell.textContent = statusMapping[task.status] || task.status;
        dueDateCell.textContent = task.dueDate ? apiDateToFrontendDate(task.dueDate) : "";

        completionDateCell.textContent = task.completionDateTime ? apiDateToFrontendDate(task.completionDateTime) : "";
        completionDateCell.classList.add("completion-date-cell");
        completionDateCell.style.display = includeCompletionDate ? "" : "none"; // Toggle visibility

        // Add event listeners to the relevant columns to open the modal
        [titleCell, assigneesCell, statusCell, dueDateCell, completionDateCell].forEach(cell => {
            cell.setAttribute("data-id", task.id)
            cell.setAttribute("data-bs-toggle", "modal")
            cell.setAttribute("data-bs-target", "#taskDetailsModal")
            cell.addEventListener("click", () => {
                openTaskModal(task);
                renderUserListEditTask("");
            });
            // Apply dark mode class
            cell.classList.add("list-text-color");
        });
        row.appendChild(toggleCell);
        row.appendChild(titleCell);
        row.appendChild(assigneesCell);
        row.appendChild(statusCell);
        row.appendChild(dueDateCell);
        row.appendChild(completionDateCell);

        // ICS
        const icsDivForTask = document.createElement("div");
        icsDivForTask.className = "btn-group justify-content-between d-flex gap-3";
        icsDivForTask.innerHTML = renderCalendarLinks(projectDetails, userIDMap, task);
        row.appendChild(icsDivForTask);

        tableBody.appendChild(row);

        // Subtasks, shown by default under the main task row
        const relatedSubtasks = subTasks.filter(subtask => subtask.parentTask === task.id);
        if (relatedSubtasks.length > 0) {
            relatedSubtasks.forEach(subtask => {
                const subtaskRow = document.createElement("tr");
                subtaskRow.classList.add(`subtasks-${task.id}`, "row-clickable"); // Group subtasks for toggling
                subtaskRow.style.display = isCollapsed ? "none" : "";  // Restore collapsed or expanded state

                if (rowCounter % 2 === 0) {
                    subtaskRow.classList.add("white-row");
                } else {
                    subtaskRow.classList.add("light-gray-row");
                }
                rowCounter++;

                const subtaskToggleCell = document.createElement("td");
                const subtaskTitleCell = document.createElement("td");
                const subtaskAssigneesCell = document.createElement("td");
                const subtaskStatusCell = document.createElement("td");
                const subtaskDueDateCell = document.createElement("td");

                subtaskToggleCell.style.width = "5px";
                subtaskToggleCell.style.maxWidth = "5px";
                subtaskTitleCell.innerHTML = `<span style="padding-left: 30px;">${subtask.title}</span>`;
                // Append subtask ID in gray, formatted as #x
                const subtaskIdSpan = document.createElement("span");
                subtaskIdSpan.classList.add("text-muted");
                subtaskIdSpan.classList.add("list-id-text-color");
                subtaskIdSpan.textContent = ` #${subtask.id}`;
                subtaskTitleCell.appendChild(subtaskIdSpan);

                subtaskAssigneesCell.textContent = subtask.assignedUsers.map(user => userIDMap[user]).join(", ");
                subtaskStatusCell.textContent = statusMapping[subtask.status] || subtask.status;
                subtaskDueDateCell.textContent = subtask.dueDate ? apiDateToFrontendDate(subtask.dueDate) : "";

                subtaskRow.appendChild(subtaskToggleCell);
                subtaskRow.appendChild(subtaskTitleCell);
                subtaskRow.appendChild(subtaskAssigneesCell);
                subtaskRow.appendChild(subtaskStatusCell);
                subtaskRow.appendChild(subtaskDueDateCell);

                // Completion date (conditionally added and formatted)
                if (includeCompletionDate) {
                    const completionDateCell = document.createElement("td");
                    completionDateCell.textContent = apiDateToFrontendDate(subtask.completionDateTime);
                    completionDateCell.classList.add("list-text-color");
                    subtaskRow.appendChild(completionDateCell);
                }

                const icsDivForSubTask = document.createElement("div");
                icsDivForSubTask.innerHTML = renderCalendarLinks(projectDetails, userIDMap, subtask);
                icsDivForSubTask.className = "btn-group justify-content-between d-flex gap-3";

                subtaskRow.appendChild(icsDivForSubTask);

                tableBody.appendChild(subtaskRow);
                [subtaskTitleCell, subtaskAssigneesCell, subtaskStatusCell, subtaskDueDateCell, completionDateCell].forEach(cell => {
                    cell.setAttribute("data-id", subtask.id)
                    cell.setAttribute("data-bs-toggle", "modal")
                    cell.setAttribute("data-bs-target", "#taskDetailsModal")
                    cell.addEventListener("click", () => {
                        openTaskModal(subtask);
                        renderUserListEditTask("");
                    });
                    // Apply dark mode class
                    cell.classList.add("list-text-color");
                });
            });
        }
    });
}

// =================================
// Sort and Show Tasks in List
// =================================

function sortAndPopulateTasksTable(columnIndex, toggleDirection = true) {
    const expandedTasks = {};
    document.querySelectorAll(".collapse").forEach(collapseElement => {
        const parentId = collapseElement.id.split("-")[1];
        expandedTasks[parentId] = collapseElement.classList.contains("show");
    });

    // Toggle the sorting direction if the same column is clicked
    if (currentSortColumn === columnIndex && toggleDirection) {
        isAscending = !isAscending; // Just change direction if user clicked on column
    } else if (currentSortColumn !== columnIndex) {
        currentSortColumn = columnIndex;
        isAscending = true;
    }

    // Update sort icons
    updateSortIcons(columnIndex, isAscending);

    // Use the filteredTasks array if available, otherwise use all tasks
    const tasksToSort = filteredTasks.length > 0 ? filteredTasks : projectTaskDetails;

    // Sort the tasks based on the selected column
    tasksToSort.sort((taskA, taskB) => {
        let valueA, valueB;

        switch (columnIndex) {
            case 0: // Title column
                valueA = taskA.title.toLowerCase();
                valueB = taskB.title.toLowerCase();
                break;
            case 1: // Assigned users column
                valueA = taskA.assignedUsers.map(user => userIDMap[user]).join(", ").toLowerCase();
                valueB = taskB.assignedUsers.map(user => userIDMap[user]).join(", ").toLowerCase();
                break;
            case 2: // Status column
                valueA = statusOrder[taskA.status];
                valueB = statusOrder[taskB.status];
                break;
            case 3: // Due date column
                valueA = taskA.dueDate ? new Date(taskA.dueDate) : new Date(0); // Fallback to an early date if no due date
                valueB = taskB.dueDate ? new Date(taskB.dueDate) : new Date(0);
                break;
            case 4: // Completion date column (conditionally handled)
                if (currentTasksView === "completed") {  // Only sort by completion date when in completed tasks view
                    valueA = taskA.completionDateTime ? new Date(taskA.completionDateTime) : new Date(0);
                    valueB = taskB.completionDateTime ? new Date(taskB.completionDateTime) : new Date(0);
                } else {
                    return 0; // No sorting if no valid column is selected
                }
                break;
            default:
                return 0; // No sorting if no valid column is selected
        }
        if (valueA < valueB) return isAscending ? -1 : 1;
        if (valueA > valueB) return isAscending ? 1 : -1;
        return 0;
    });

    // Pass currentTasksView to retain completion date visibility during sorting
    populateTasksTable(tasksToSort, currentTasksView === "completed");

    // Restore expanded tasks after sorting
    Object.keys(expandedTasks).forEach(parentId => {
        const collapseElement = document.getElementById(`subtasks-${parentId}`);
        if (collapseElement && expandedTasks[parentId]) {
            new bootstrap.Collapse(collapseElement, {toggle: true}).show();
        }
    });
}

function updateSortIcons(columnIndex, isAscending) {
    document.querySelectorAll(".sort-icon").forEach(icon => {
        icon.innerHTML = "&#9650;&#9660;";  // Both arrows (up and down)
    });

    const sortIcon = document.getElementById(`sort-icon-${columnIndex}`);
    if (isAscending) {
        sortIcon.innerHTML = "&#9650;";  // Up arrow
    } else {
        sortIcon.innerHTML = "&#9660;";  // Down arrow
    }
}

// =================================
// Toggle Subtasks in List
// =================================

function toggleSubtasks(taskId) {
    const subtasks = document.querySelectorAll(`.subtasks-${taskId}`);
    const arrowButton = document.querySelector(`button[data-task-id="${taskId}"]`);
    const isCollapsed = arrowButton.getAttribute("data-collapsed") === "true";

    subtasks.forEach(subtask => {
        if (isCollapsed) {
            // If it was collapsed, now expand it
            subtask.style.display = ""; // Show subtasks
            arrowButton.textContent = "▾"; // Change arrow to down
            arrowButton.setAttribute("data-collapsed", "false");
            collapsedState[taskId] = false;  // Update state to expanded
        } else {
            // If it was expanded, now collapse it
            subtask.style.display = "none"; // Hide subtasks
            arrowButton.textContent = "▸"; // Change arrow to right
            arrowButton.setAttribute("data-collapsed", "true");
            collapsedState[taskId] = true;  // Update state to collapsed
        }
    });
}
