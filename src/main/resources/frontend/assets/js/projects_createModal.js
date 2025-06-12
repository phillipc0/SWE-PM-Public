const userList = document.getElementById("userList");
const managerList = document.getElementById("managerList");
const userCountInfo = document.getElementById("userCountInfo")
const searchInput = document.getElementById("usersearch");
const managerSearchInput = document.getElementById("managersearch");
let selectedUsers = new Set();
let selectedManagerID = -1;
let users = [];

async function getCurrentUser() {
    try {
        const response = await fetch("/api/v1/users/current");
        if (!response.ok) {
            // If response is not OK, throw an error
            new Error(`Response status: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error(error.message);
    }
}

function createProjectCard() {
    let cards = document.getElementById("cards");

    // Create card div element
    let card = document.createElement("div");
    card.className = "card";
    card.style.width = "21rem";
    card.id = "createProjectCard";

    // Create button element
    let btn = document.createElement("button");
    btn.className = "btn btn-createproject card-body align-content-center";
    btn.setAttribute("data-bs-toggle", "modal");
    btn.setAttribute("data-bs-target", "#createProjectModal");
    btn.id = "createProjectBTN";
    btn.title = "Neues Projekt erstellen";

    btn.addEventListener("click", async function () {
        //prepare modal data
        document.getElementById("createProject_status").innerHTML = "";
        users = await getUsers();
        const currentUser = await getCurrentUser();
        if (currentUser && currentUser.role === "PROJECT_MANAGER") {
            selectedManagerID = currentUser.id;
        }
        renderUserList("", false);
        renderUserList("", true);
    })

    // Create img element
    let img = document.createElement("img");
    img.src = "assets/svg/newproject.svg";
    img.alt = "Neues Projekt anlegen";
    img.id = "createProjectBtnSvg";
    img.style.width = "69%";
    img.style.height = "69%";

    btn.appendChild(img);
    card.appendChild(btn);
    cards.appendChild(card);
}

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

//builds the createProject userLists using the filter parameter for name search and isManagerList for addressing the
//correct list with correct content
function renderUserList(filter = "", isManagerList = false) {

    const userListElement = isManagerList ? managerList : userList;
    userListElement.innerHTML = "";
    const filteredUsers = users.filter(user =>
        user.name.toLowerCase().includes(filter.toLowerCase()) &&
        (!isManagerList || user.role === "PROJECT_MANAGER") &&
        (!isManagerList ? selectedManagerID !== user.id : true)
    ).sort((a, b) => a.name.localeCompare(b.name));

    // Create list of users
    filteredUsers.forEach(user => {
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

        if (!isManagerList && selectedUsers.has(user.id)) {
            userList.classList.add("userListSelected");
        } else if (isManagerList && user.id === selectedManagerID) {
            userList.classList.add("managerListSelected");
        }

        userList.addEventListener("click", () => {
            if (isManagerList) {
                // If manager list, delete previous choice
                selectedManagerID = -1;
                document.querySelectorAll(".managerListSelected").forEach(el => el.classList.remove("managerListSelected"));
                selectedManagerID = user.id;
                selectedUsers.delete(user.id);
                userList.classList.add("managerListSelected");
                //resets employee list when new manager is chosen
                searchInput.value = "";
                renderUserList("", false);
            } else {
                // if normal employee list allow multiple choice
                userList.classList.toggle("userListSelected");
                const id = userList.dataset.id;
                if (userList.classList.contains("userListSelected")) {
                    selectedUsers.add(id);
                } else {
                    selectedUsers.delete(id);
                }
            }

            userCountInfo.innerText = "";
            if (selectedManagerID === -1) {
                userCountInfo.innerText = "Kein Manager ausgewählt!";
            } else {
                userCountInfo.innerText = selectedUsers.size + 1 + " Mitarbeiter ausgewählt";
            }
        });
        userListElement.appendChild(userList);
    });
}

searchInput.addEventListener("input", (e) => {
    renderUserList(e.target.value, false);
});

managerSearchInput.addEventListener("input", (e) => {
    renderUserList(e.target.value, true);
});

document.getElementById("projectCreateForm").addEventListener("submit", async function (e) {
    e.preventDefault();
    if (selectedManagerID !== -1) {
        let formData = new FormData(this);

        let formObj = {}
        formData.forEach(function (value, key) {
            formObj[key] = value;
        })

        // Check description length and show error if too long
        if (formObj["description"].length > 1024) {
            document.getElementById("createProject_status").innerHTML = "Beschreibung darf nicht mehr als 1024 Zeichen haben!";
            return; // Prevent submission
        }

        formObj["manager"] = selectedManagerID;

        formObj["users"] = Array.from(selectedUsers);

        let response = await fetch("/api/v1/projects", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(formObj)
        })

        if (response.status === 409) {
            document.getElementById("createProject_status").innerHTML = "Konflikt mit existierendem Projekt!";
        } else if (!response.ok) {
            document.getElementById("createProject_status").innerHTML = "Es ist ein Fehler bei der Erstellung des Projekts aufgetreten!";
        } else {
            location.reload();
        }
    } else {
        document.getElementById("createProject_status").innerHTML = "Projektleiter auswählen!";
    }
});
