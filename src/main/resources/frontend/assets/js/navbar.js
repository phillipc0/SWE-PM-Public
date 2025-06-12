// activates all tooltips in project
const tooltipTriggerList = document.querySelectorAll(`[data-bs-toggle="tooltip"]`)
const triggers = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl))
let projectMap = {};

// function to load navbar on page open
async function loadNavbar() {
    const navbar = document.getElementById("navbar-placeholder");
    const currentPage = window.location.pathname.split("/").pop();

    const navbarUser = await (await fetch("/api/v1/users/current")).json();

    // Load all projects to map project IDs to names
    await fetchProjects();

    // Get notifications for navbar user
    const userNotifications = await fetchUserNotifications();

    // Loads the Navbar from a separate HTML file
    return fetch("/assets/templates/navbar")
        .then(response => response.text())
        .then(data => {
            navbar.innerHTML = data;

            // fill navbar user dropdown
            const navbarUserText = document.getElementById("navbarUser");
            const navbarEmailText = document.getElementById("navbarUserEmail");
            const navbarUserRole = document.getElementById("navbarUserRole");
            navbarUserText.textContent = navbarUser.name;  // Set the username
            navbarEmailText.textContent = navbarUser.emailAddress;  // Set the email

            if (navbarUser.role === "PROJECT_MANAGER") {
                navbarUserRole.textContent = "Projektmanager";
            } else if (navbarUser.role === "EMPLOYEE") {
                navbarUserRole.textContent = "Mitarbeiter";
            } else {
                navbarUserRole.textContent = "unknown";
            }
            showAdduserButtonIfManager(navbarUser.role);
            fillMailbox(userNotifications);

            // adds "active" to show as the active site
            const navLinks = navbar.querySelectorAll(".nav-link");
            navLinks.forEach(async link => {
                let linkPath = link.getAttribute("href");
                if (linkPath != null) {
                    linkPath = linkPath.split("/").pop();
                }

                if (linkPath === currentPage) {
                    link.classList.add("active");
                } else {
                    link.classList.remove("active");
                }
            });

            // Resolve the promise indicating that the navbar has been loaded
            return Promise.resolve();
        });
};

// Function to fetch all projects and store their IDs and names
async function fetchProjects() {
    try {
        const response = await fetch("/api/v1/projects");
        const projects = await response.json();

        // Create a map of project IDs to project names
        projectMap = projects.reduce((map, project) => {
            map[project.id] = project.name;
            return map;
        }, {});

        return projectMap;
    } catch (error) {
        console.error("Error fetching projects:", error);
        return {};
    }
}

// Helper function to get a project name by ID
function getProjectNameById(projectId) {
    return projectMap[projectId] || "Unbekanntes Projekt";
}

// Fetch user notifications and map project names
async function fetchUserNotifications() {
    try {
        const usersUpcomingTasks = await (await fetch("/api/v1/users/current/tasks/upcoming")).json();

        // Calculate urgency for each task and map project names
        return usersUpcomingTasks.map(task => {
            return {
                notificationText: task.title,
                notificationProject: getProjectNameById(task.project),
                notificationRef: `/projects/${task.project}/views?view=board`,
                notificationUrgency: calculateNotificationUrgency(task.dueDate),
                dueDate: task.dueDate
            };
        });
    } catch (error) {
        console.error('Error fetching user notifications:', error);
        return [];
    }
}

// Calculate urgency based on due date
function calculateNotificationUrgency(dueDate) {
    const due = new Date(dueDate);
    const now = new Date();

    due.setHours(0, 0, 0, 0);
    now.setHours(0, 0, 0, 0);

    const timeDiff = due - now; // difference in milliseconds
    const daysUntilDue = timeDiff / (1000 * 60 * 60 * 24); // convert to days

    if (daysUntilDue < 0) {
        return 4; // Task is overdue
    } else if (daysUntilDue < 1) {
        return 3; // Less than 1 day remaining
    } else if (daysUntilDue < 3) {
        return 2; // Less than 3 days remaining
    } else if (daysUntilDue < 7) {
        return 1; // Less than 7 days remaining
    } else {
        return 0; // 7 or more days remaining
    }
}

// Helper method for grouping the notifications by project
function groupByProject(dataObjects) {
    return dataObjects.reduce((acc, obj) => {
        const project = obj.notificationProject || "Unbekanntes Projekt";
        if (!acc[project]) {
            acc[project] = [];
        }
        acc[project].push(obj);
        return acc;
    }, {});
}

// Takes notifications, groups and displays them in the mailbox
function fillMailbox(notifications) {
    let container = document.getElementById("mailbox-content");
    container.innerHTML = ""; // Clear any existing content

    let hasUrgentNotification = false;
    let notificationImage = document.getElementById("mailboxImage");

    if (notifications.length === 0) {
        notificationImage.src = "/assets/svg/mailbox-empty.svg";
        let emptyMessage = document.createElement("h5");
        emptyMessage.innerHTML = "Keine Benachrichtigungen vorhanden!";
        container.appendChild(emptyMessage);
    } else {
        const groupedNotifications = groupByProject(notifications);
        for (const project in groupedNotifications) {
            const projectDiv = document.createElement("div");
            projectDiv.classList.add("project-section");

            const projectTitle = document.createElement("h2");
            projectTitle.innerHTML = project;
            projectDiv.appendChild(projectTitle);

            const projectRuler = document.createElement("hr");
            projectRuler.className = "project-divider";
            projectDiv.appendChild(projectRuler);

            groupedNotifications[project].forEach(notification => {
                const notificationDiv = document.createElement("div");
                notificationDiv.classList.add("notification-item");

                // Convert due date to a readable format
                const dueDate = new Date(notification.dueDate).toLocaleDateString();

                const priorityText = notification.notificationUrgency <= 3 ? "Priorität: " + notification.notificationUrgency : "Überfällig!";

                notificationDiv.innerHTML = `
                    <div class="notification-content">
                        <a href="${notification.notificationRef}" class="notification-title">${notification.notificationText}</a>
                        <div class="notification-details">
                            <span class="notification-priority">${priorityText}</span>
                            <span class="notification-duedate">Fällig: ${dueDate}</span>
                        </div>
                    </div>`;

                // Check for urgent notifications (Urgency 3 or higher)
                if (notification.notificationUrgency >= 3) {
                    notificationDiv.classList.add("notification-urgent");
                    hasUrgentNotification = true;
                }

                projectDiv.appendChild(notificationDiv);
            });

            container.appendChild(projectDiv);
        }

        // Check for urgent notifications
        if (hasUrgentNotification) {
            notificationImage.src = "/assets/svg/mailbox-important.svg";
        } else {
            notificationImage.src = "/assets/svg/mailbox.svg";
        }
    }
}

function toggleDarkMode() {
    if (document.body.classList.contains('dark-mode')) {
        document.body.classList.remove('dark-mode');
        localStorage.setItem('darkMode', 'disabled');
    } else {
        document.body.classList.add('dark-mode');
        localStorage.setItem('darkMode', 'enabled');
    }
}

function toggleDropdown(className) {
    let dropdowns = document.querySelectorAll(".dropdown-content");
    dropdowns.forEach(function (dropdown) {
        if (!dropdown.classList.contains(className)) {
            dropdown.style.display = "none";
        }
    });

    let dropdownContent = document.querySelector("." + className);
    if (dropdownContent.style.display === "block") {
        dropdownContent.style.display = "none";
    } else {
        dropdownContent.style.display = "block";
    }
}

function setNavbarTitle(navbarTitleText) {
    document.getElementById("projectTitleNav").innerHTML = navbarTitleText
}

window.onclick = function (event) {
    if (!event.target.closest(".dropdown")) {
        let dropdowns = document.querySelectorAll(".dropdown-content");
        dropdowns.forEach(function (dropdown) {
            dropdown.style.display = "none";
        });
    }
}

// ===========================================
// Add user Button for Project Overview Site
// ===========================================

function showAdduserButtonIfManager(userRole) {
    if (userRole === "PROJECT_MANAGER") {
        let createUserButton = document.getElementById("createUserBTN");
        createUserButton.style.visibility = "visible";
        createUserButton.title = "Nutzer erstellen";
    }
}

function Sleep(milliseconds) {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
}

//listener for loading a page
document.addEventListener("DOMContentLoaded", async () => {
    await loadNavbar();
    await Sleep(500);

    let pw;

    // Eventlistener for AddUserButton
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

});

