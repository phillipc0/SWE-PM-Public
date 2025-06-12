async function openDetailsView(projectJSON, userJSON) {
    setNavbarTitle(projectJSON.name)

    // Variablen
    const userList = document.getElementById("userList");
    const managerList = document.getElementById("managerList");
    const changeDescription_status = document.getElementById("changeDescription_status")
    const searchInput = document.getElementById("usersearch");
    const managerSearchInput = document.getElementById("managersearch");
    const addUserSearch = document.getElementById("addusersearch");
    let selectedUsers = new Set();
    let removeUserSet = new Set();
    let dieseFunktionSet = new Set();
    let selectedManagerID = projectJSON.projectManager.id;
    let users = [];
    let getEditUser;

    // Fetch projects from the API
    async function getUsers() {
        try {
            const response = await fetch("/api/v1/users");
            if (!response.ok) {
                // If response is not OK, throw an error
                new Error(`Response status: ${response.status}`);
            }
            // Parse and return JSON response
            return await response.json();
        } catch (error) {
            console.error(error.message);
        }
    }

    // User des aktuellen Projekts
    let currentProjectUsers = new Set();
    projectJSON.users.forEach(user => {
        currentProjectUsers.add(user.id);
    });

    // Set removeUserSet
    projectJSON.users.forEach(user => {
        removeUserSet.add(user.id);
    })
    removeUserSet.add(projectJSON.projectManager.id)

    // Users
    users = projectJSON.users;

    // Function for the Manager list and User in Project
    function renderUserList(filter = "", isManagerList = false) {
        const userListElement = isManagerList ? managerList : userList;
        userListElement.innerHTML = "";
        if (!isManagerList && userJSON.role === "PROJECT_MANAGER") {
            let managerButtonGroup = document.getElementById("userListBTNs")
            managerButtonGroup.innerHTML = ""
            let btnGroup = document.createElement("li");
            btnGroup.className = "btn-group";

            // Erstellen
            let createUserAndAddToProject = document.createElement("button");
            createUserAndAddToProject.className = "btn s btn-teal col addProjectUserBtn";
            createUserAndAddToProject.style.margin = "auto";
            createUserAndAddToProject.setAttribute("data-bs-toggle", "modal");
            createUserAndAddToProject.setAttribute("data-bs-target", "#detailsCreateUserModal");
            createUserAndAddToProject.id = "createUserBTN";
            createUserAndAddToProject.title = "Nutzer erstellen und zum Projekt hinzufügen"
            let createUserAndAddToProjectImg = document.createElement("img");
            createUserAndAddToProjectImg.src = "/assets/svg/person-add.svg";
            createUserAndAddToProjectImg.height = 32;
            createUserAndAddToProjectImg.width = 32;
            createUserAndAddToProject.appendChild(createUserAndAddToProjectImg);

            // Hinzufügen
            let addUserToProject = document.createElement("button");
            addUserToProject.className = "btn s btn-teal col addProjectUserBtn";
            addUserToProject.style.margin = "auto";
            addUserToProject.setAttribute("data-bs-toggle", "modal");
            addUserToProject.setAttribute("data-bs-target", "#addUserModal");
            addUserToProject.id = "addUserBTN";
            addUserToProject.title = "Nutzer zum Projekt hinzufügen"
            let addUserToProjectImg = document.createElement("img");
            addUserToProjectImg.src = "/assets/svg/person-down.svg";
            addUserToProjectImg.height = 32;
            addUserToProjectImg.width = 32;
            addUserToProject.appendChild(addUserToProjectImg);

            btnGroup.appendChild(createUserAndAddToProject);
            btnGroup.appendChild(addUserToProject);
            managerButtonGroup.appendChild(btnGroup);
        }
        let filteredUsers = users.filter(user =>
            (user.name.toLowerCase().includes(filter.toLowerCase()) || user.emailAddress.toLowerCase().includes(filter.toLowerCase())) &&
            (!isManagerList || user.role === "PROJECT_MANAGER") &&
            (!isManagerList ? selectedManagerID !== user.id : true)
        ).sort((a, b) => a.name.localeCompare(b.name));
        if (!isManagerList) {
            filteredUsers = filteredUsers.filter(user => user.id !== projectJSON.projectManager.id);
        }
        // Create list of users
        filteredUsers.forEach(user => {
            let userList = document.createElement("li");
            userList.className = "list-group-item d-flex justify-content-between align-items-center text-start";
            userList.textContent = user.name + " (" + user.emailAddress + ")";

            if (isManagerList) {
                userList.className = "list-group-item text-start";
                userList.textContent = "";
                const userNameSpan = document.createElement("span");
                userNameSpan.textContent = user.name;
                const userEmailSpan = document.createElement("span");
                userEmailSpan.textContent = ` (${user.emailAddress})`;
                userEmailSpan.style.color = "gray";
                userList.appendChild(userNameSpan);
                userList.appendChild(userEmailSpan);
            }

            userList.dataset.id = user.id;
            if (!isManagerList && userJSON.role === "PROJECT_MANAGER") {
                let mybtngroup1 = document.createElement("div");
                mybtngroup1.className = "btn-group";
                mybtngroup1.role = "group";
                mybtngroup1.setAttribute("aria-label", "Basic mixed styles example");

                // Hinzufügen
                let editButton = document.createElement("button");
                editButton.className = "btn btn-teal";
                editButton.style.margin = "auto";
                editButton.type = "button";
                editButton.setAttribute("data-bs-toggle", "modal");
                editButton.setAttribute("data-bs-target", "#editUserModal");
                editButton.id = "editUserBTN";
                editButton.addEventListener("click", () => {
                    loadUserDataIntoModal(user);
                });
                let editButtonImg = document.createElement("img");
                editButtonImg.src = "/assets/svg/pencil-square.svg";
                editButtonImg.height = 16;
                editButtonImg.width = 16;
                editButtonImg.title = "Nutzer bearbeiten";
                editButton.appendChild(editButtonImg);

                // Entfernen
                let removeButton = document.createElement("button");
                removeButton.className = "btn btn-danger";
                removeButton.type = "button";
                removeButton.style.margin = "auto";
                removeButton.id = "removeUserButton";
                removeButton.addEventListener("click", () => {
                    removeUserFunction(user);
                });
                let removeButtonImg = document.createElement("img");
                removeButtonImg.src = "/assets/svg/x-lg.svg";
                removeButtonImg.height = 16;
                removeButtonImg.width = 16;
                removeButtonImg.title = "Nutzer entfernen";
                removeButton.appendChild(removeButtonImg);

                mybtngroup1.appendChild(editButton);
                mybtngroup1.appendChild(removeButton);
                userList.appendChild(mybtngroup1);
            }
            if (isManagerList && user.id === selectedManagerID) {
                userList.classList.add("managerListSelected");
            }
            userList.addEventListener("click", () => {
                if (isManagerList) {
                    // If manager list, delete previous choice
                    document.querySelectorAll(".managerListSelected").forEach(el => el.classList.remove("managerListSelected"));
                    selectedManagerID = user.id;
                    selectedUsers.delete(user.id);
                    userList.classList.add("managerListSelected");
                    //resets employee list when new manager is chosen
                    searchInput.value = "";
                    renderUserList("", false);
                }
                changeDescription_status.innerText = "";
                if (selectedManagerID === -1) {
                    changeDescription_status.innerText = "Kein Manager ausgewählt!";
                }
            });
            userListElement.appendChild(userList);
            document.getElementById("projectEmployeeCount").innerHTML = projectJSON.users.length - 1 + " Mitarbeiter + 1 Projektleiter";
        });
    }

    // Function for add User to Project
    async function renderAddUserList(filter = "") {
        const userListElement = document.getElementById("addUserList");
        userListElement.innerHTML = "";

        // Alle User
        let allUsers = await getUsers();
        let allUsersSet = new Set();
        allUsers.forEach(user => {
            allUsersSet.add(user.id);
        })

        allUsers.forEach(user => {
            let isUserInProject = projectJSON.users.some(projectUser => projectUser.id === user.id);
            // Implement the filter
            // Check if the user's name or email matches the filter (case-insensitive)
            if (!isUserInProject && (user.name.toLowerCase().includes(filter.toLowerCase()) || user.emailAddress.toLowerCase().includes(filter.toLowerCase()))) {
                const userList = document.createElement("li");
                userList.className = "list-group-item";
                const userNameSpan = document.createElement("span");
                userNameSpan.textContent = user.name;
                const userEmailSpan = document.createElement("span");
                userEmailSpan.textContent = ` (${user.emailAddress})`;
                userEmailSpan.style.color = "gray";
                userList.appendChild(userNameSpan);
                userList.appendChild(userEmailSpan);
                userList.dataset.id = String(user.id);
                userList.addEventListener("click", () => {
                    userList.classList.toggle("userListSelected");
                    const id = userList.dataset.id;
                    if (userList.classList.contains("userListSelected")) {
                        selectedUsers.add(id);
                    } else {
                        selectedUsers.delete(id);
                    }
                });
                userListElement.appendChild(userList);
            } else if (isUserInProject) {
                selectedUsers.add(user.id);
            }
        });
    }

    // Suchfelder - Search field
    searchInput.addEventListener("input", (e) => {
        renderUserList(e.target.value, false);
    });
    managerSearchInput.addEventListener("input", (e) => {
        renderUserList(e.target.value, true);
    });
    addUserSearch.addEventListener("input", (e) => {
        renderAddUserList(e.target.value);
    });
    renderUserList("", false);
    renderUserList("", true);

    if (userJSON.role === "PROJECT_MANAGER") {
        renderAddUserList("");
    }

    if (userJSON.role === "EMPLOYEE") {
        document.getElementById("superwichtigebuttons").setAttribute("hidden", "");
    }

    // Erstellen eines Nutzers
    document.getElementById("detailsGeneratePW").addEventListener("click", async function () {
        pw = generatePassword(32);
        navigator.clipboard.writeText(pw).then(function () {
            document.getElementById("detailsCreateUser_password").value = pw;
        }).catch(function (err) {
        });
    });

    document.getElementById("detailsConfirmUserCreateForm_btn").addEventListener("click", async function () {
        if (typeof pw !== "undefined") {
            navigator.clipboard.writeText(pw).then(function () {
            }).catch(function (err) {
            });
        }
    });

    document.getElementById("detailsUserCreateForm").addEventListener("submit", async function (e) {
        e.preventDefault();

        document.getElementById("detailsCreateUser_password").removeAttribute('readonly');

        if (!this.reportValidity()) {
            document.getElementById("detailsCreateUser_password").setAttribute("readonly", "");
            return;
        }

        let formData = new FormData(this);
        let formObj = {};
        formData.forEach(function (value, key) {
            formObj[key] = value;
        });

        let response = await fetch("/api/v1/users", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(formObj)
        });

        if (response.status === 409) {
            document.getElementById("detailsCreateUser_status").innerHTML = "Konflikt mit existierendem User!";
        } else if (!response.ok) {
            document.getElementById("detailsCreateUser_status").innerHTML = "Es ist ein Fehler bei der Erstellung des Users aufgetreten!";
        } else {
            let jsonResponse = await response.json();
            projectJSON.users.forEach(user => {
                dieseFunktionSet.add(user.id);
            })
            dieseFunktionSet.add(jsonResponse.id);
            formObj = {"users": Array.from(dieseFunktionSet)};

            let projectResponse = await fetch(`/api/v1/projects/${projectJSON.id}`, {
                method: "PATCH",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(formObj)
            });

            if (!projectResponse.ok) {
                document.getElementById("detailsCreateUser_status").innerHTML = "Es ist ein Fehler bei der Hinzufügung zum Projekt aufgetreten!";
            } else {
                document.getElementById("detailsUserCreateForm").reset();
                document.getElementById("detailsCreateUser_status").innerHTML = "";
                document.getElementById("detailsCreateUserModalClose").click();
                document.getElementById("detailsCreateUser_password").setAttribute("readonly", "");
                pw = generatePassword(32);
                location.reload();
            }
        }
    });

    // Laden des Nutzers
    function loadUserDataIntoModal(user) {
        getEditUser = user;

        // Name laden
        document.getElementById("editUser_name").value = user.name;

        // E-Mail laden
        document.getElementById("editUser_email").value = user.emailAddress;

        // Rolle setzen
        if (user.role === "EMPLOYEE") {
            document.getElementById("editEmployee").checked = true;
            document.getElementById("editProjectManager").checked = false;
        } else if (user.role === "PROJECT_MANAGER") {
            document.getElementById("editEmployee").checked = false;
            document.getElementById("editProjectManager").checked = true;
        }
    }

    // remove user
    async function removeUserFunction(user) {
        removeUserSet.delete(user.id);
        let formObj = {};
        formObj["users"] = Array.from(removeUserSet);
        let response = await fetch("/api/v1/projects/" + projectJSON.id, {
            method: "PATCH",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(formObj)
        })
        if (!response.ok) {
            console.error("Komisch");
        } else {
            location.reload();
        }
    }

    // Bearbeitung eines Nutzers
    document.getElementById("userEditForm").addEventListener("submit", async function (e) {
        e.preventDefault();
        let formData = new FormData(this);
        let formObj = {};
        formData.forEach(function (value, key) {
            formObj[key] = value;
        })
        let response = await fetch("/api/v1/users/" + getEditUser.id, {
            method: "PATCH",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(formObj)
        })
        if (response.status === 409) {
            document.getElementById("editUser_status").innerHTML = "Konflikt mit existierendem User!";
        } else if (!response.ok) {
            document.getElementById("editUser_status").innerHTML = "Es ist ein Fehler bei der Bearbeitung des User aufgetreten!";
        } else {
            document.getElementById("userEditForm").reset();
            document.getElementById("editUser_status").innerHTML = "";
            document.getElementById("editUserModalClose").click();
            location.reload();
        }
    });

    // Poject Titel
    let projecttitle = document.getElementById("project_projecttitle");
    projecttitle.value = unescapeHTML(projectJSON.name);

    // E-Mail
    const projectManager_email = projectJSON.projectManager.emailAddress;
    document.getElementById("projectManager_name").innerHTML = projectJSON.projectManager.name + ' (<a href=\"mailto:' + projectManager_email + '">' + projectManager_email + '</a>)';

    // Beschreibung
    let mytextarea = document.getElementById("project_description");
    mytextarea.innerHTML = projectJSON.description;
    mytextarea.style.height = "auto";
    mytextarea.style.height = mytextarea.scrollHeight + "px";

    // Startdatum
    let startdatum = document.getElementById("project_createdOn");
    startdatum.value = apiDateToFrontendDate(dateTimeToDate(projectJSON.createdOn));
    startdatum.setAttribute("disabled", "");

    // Button Management

    document.getElementById("editProjectButton").addEventListener("click", async function () {
        document.getElementById("project_description").removeAttribute("disabled");
        document.getElementById("project_projecttitle").removeAttribute("disabled");
        document.getElementById("saveProjectButton").removeAttribute("hidden");
        document.getElementById("editProjectButton").setAttribute("hidden", "hidden");
        document.getElementById("Projektmanagerauswahl").removeAttribute("hidden");
        document.getElementById("Projektmanageranzeige").setAttribute("hidden", "hidden");
    })

    document.getElementById("saveProjectButton").addEventListener("click", async function (e) {
        e.preventDefault();
        let formData = new FormData(document.getElementById("projectEditForm"));
        let formObj = {};
        formData.forEach(function (value, key) {
            formObj[key] = value;
        })
        formObj["manager"] = selectedManagerID;
        let response = await fetch("/api/v1/projects/" + projectJSON.id, {
            method: "PATCH",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(formObj)
        })
        if (!response.ok) {
            document.getElementById("changeDescription_status").innerHTML = "Es ist ein Fehler bei der Hinzufügung zum Projekts aufgetreten!";
        }
        document.getElementById("project_description").setAttribute("disabled", "");
        document.getElementById("project_projecttitle").setAttribute("disabled", "");
        document.getElementById("editProjectButton").removeAttribute("hidden");
        document.getElementById("saveProjectButton").setAttribute("hidden", "hidden");
        document.getElementById("Projektmanageranzeige").removeAttribute("hidden");
        document.getElementById("Projektmanagerauswahl").setAttribute("hidden", "hidden");
        location.reload();
    })

// Hinzufügen
    document.getElementById("userAddForm").addEventListener("submit", async function (e) {
        e.preventDefault();
        if (selectedManagerID !== -1) {
            let formObj = {};
            formObj["users"] = Array.from(selectedUsers);
            let response = await fetch("/api/v1/projects/" + projectJSON.id, {
                method: "PATCH",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(formObj)
            })
            if (!response.ok) {
                document.getElementById("addUser_status").innerHTML = "Es ist ein Fehler bei der Hinzufügung zum Projekts aufgetreten!";
            } else {
                location.reload();
            }
        }
    })

// Delete
    if (userJSON.role === "PROJECT_MANAGER") {
        let meinknopf = document.createElement("button");
        meinknopf.className = "btn btn-danger";
        meinknopf.setAttribute("data-bs-toggle", "modal");
        meinknopf.setAttribute("data-bs-target", "#deleteProjectModal")
        meinknopf.id = "deleteProjectButton";
        meinknopf.type = "button";
        meinknopf.innerHTML = "Projekt löschen";
        document.getElementById("superwichtigebuttons").appendChild(meinknopf);
    }

    document.getElementById("confirmDeleteProjectButton").addEventListener("click", function () {
        const isValid = validateProjectNameOnDelete();
        if (isValid) {
            deleteProjectById(projectJSON.id);
        } else {
            console.error("Project name validation failed.");
        }
    })

    document.getElementById("deleteProjectButton").addEventListener("click", function () {
        // ToDo: The value of the input field and the error message should be reset after you reopen the modal
        document.getElementById("projectNameInput").value = "";
        document.getElementById("error-message").style.display = "none";
        const projectName = projectJSON.name;
        const confirmationTextElement = document.getElementById("deleteConfirmationText");
        confirmationTextElement.innerHTML = `Sind Sie sicher, dass Sie das Projekt "${projectName}" löschen wollen?`;
    })

    function validateProjectNameOnDelete() {
        const input = escapeHTML(document.getElementById("projectNameInput").value);
        if (input === projectJSON.name) {
            document.getElementById("error-message").style.display = "none";
            return true;
        } else {
            document.getElementById("error-message").style.display = "block";
            return false;
        }
    }

    function deleteProjectById() {
        fetch("/api/v1/projects/" + projectJSON.id, {method: "DELETE"})
            .then(response => {
                if (response.ok) {
                    document.getElementById("deleteProjectModal").style.display = "none";
                    window.location.replace("/projects")

                }
            })
            .catch(error => console.error(error));
    }
}
