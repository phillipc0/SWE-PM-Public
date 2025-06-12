document.addEventListener("DOMContentLoaded", async function () {
    await loadNavbar();
    await createCards();
    try {
        const response = await fetch("/api/v1/users/current/role").then(response => response.json());
        if (!response.ok) {
            // If response is not OK, throw an error
            new Error(`Response status: ${response.status}`);
        }
        if (response === "PROJECT_MANAGER") {
            createProjectCard();
        } else if (response === "EMPLOYEE") {
        } else {
            console.error("Bruder was bist du?");
        }
    } catch (error) {
        console.error(error.message);
    }
});

// Fetch projects from the API
async function getProjects() {
    try {
        const response = await fetch("/api/v1/projects");
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

// Create cards for projects and tasks
async function createCards() {
    let projects = await getProjects();

    let cards = document.getElementById("cards");
    let errorPlaceholder = document.getElementById("error-placeholder");

    if (projects.length === 0) {
        let newParagraph = document.createElement("p");
        newParagraph.className = "alert alert-warning card-gray";
        newParagraph.innerHTML = "Du hast keine zugeordneten Projekte";
        errorPlaceholder.appendChild(newParagraph);
    } else {
        projects.forEach(project => {
            // Create card for each project
            let card = document.createElement("div");
            card.className = "card d-flex flex-column";
            card.style.width = "21rem";
            card.id = "projectCard" + project.id;
            card.dataset.projectManager = project.projectManager.name;
            card.dataset.createdOn = project.createdOn;

            let body = document.createElement("div");
            body.className = "card-body d-flex flex-column flex-grow-1"; // Flexbox column with growing capability

            // Create card header with project name and member count
            let head = document.createElement("div");
            head.className = "d-flex justify-content-between align-items-center";

            let name = document.createElement("h5");
            name.className = "card-title";
            name.id = "projectTitle";
            truncateText(name, project.name, 130);
            head.appendChild(name);

            let user = document.createElement("div");
            user.className = "d-inline-flex align-items-center";
            user.id = "user";

            let img = document.createElement("img");
            img.src = "assets/svg/person.svg";
            img.id = "personSvg";
            img.alt = "Anzahl Mitarbeiter";
            img.title = "Anzahl Mitarbeiter: " + project.users.length;
            img.width = 24;
            img.height = 24;
            user.appendChild(img);

            let number = document.createElement("h5");
            number.className = "mb-0 ml-2";
            number.innerHTML = project.users.length;
            user.appendChild(number);

            head.appendChild(user);
            body.appendChild(head);

            // Add project manager
            let manager = document.createElement("h6");
            manager.className = "card-subtitle mb-2 text-body-secondary";
            manager.id = "projectManager";
            manager.innerHTML = project.projectManager.name;
            body.appendChild(manager);

            let description = document.createElement("p");
            description.className = "card-text flex-grow-1"; // Ensure it takes available space
            truncateText(description, project.description, 1330);
            body.appendChild(description);

            let footer = document.createElement("div");
            footer.className = "mt-auto"; // Margin top auto pushes it to the bottom

            // Progress bar
            let {TODO, ASSIGNED, IN_PROGRESS, READY_TO_REVIEW, IN_REVIEW, DONE} = project.taskStatusCount;

            if (TODO === 0 && ASSIGNED === 0 && IN_PROGRESS === 0 && READY_TO_REVIEW === 0 && IN_REVIEW === 0 && DONE === 0) {
                let newParagraph = document.createElement("p");
                newParagraph.className = "alert alert-warning card-gray mb-2";
                newParagraph.innerHTML = "Keine Aufgaben im Projekt vorhanden";
                newParagraph.id = "noTasksInProject" + project.id;
                footer.appendChild(newParagraph);
            } else {
                let progressbar = document.createElement("div");
                progressbar.className = "progress-stacked mb-2";

                const statusMapping = {
                    TODO: "To Do",
                    ASSIGNED: "Zugewiesen",
                    IN_PROGRESS: "In Bearbeitung",
                    READY_TO_REVIEW: "Ready to Review",
                    IN_REVIEW: "In Review",
                    DONE: "Fertig"
                };

                const colors = {
                    TODO: "var(--todo)",
                    ASSIGNED: "var(--assigned)",
                    IN_PROGRESS: "var(--inprogress)",
                    READY_TO_REVIEW: "var(--readytoreview)",
                    IN_REVIEW: "var(--inreview)",
                    DONE: "var(--completed)"
                };

                let totalTasks = TODO + ASSIGNED + IN_PROGRESS + READY_TO_REVIEW + IN_REVIEW + DONE;
                const statuses = ["TODO", "ASSIGNED", "IN_PROGRESS", "READY_TO_REVIEW", "IN_REVIEW", "DONE"];

                statuses.forEach(status => {
                    let count = project.taskStatusCount[status];
                    let percentage = (count / totalTasks) * 100;
                    if (percentage > 0) {
                        let progress = document.createElement("div");
                        progress.className = "progress";
                        progress.role = "progressbar";
                        progress.ariaLabel = statusMapping[status].toLowerCase();
                        progress.ariaValueNow = percentage + "";
                        progress.ariaValueMin = "0";
                        progress.ariaValueMax = "100";
                        progress.style.width = percentage + "%";
                        progress.title = statusMapping[status] + ": " + count;
                        progress.id = "" + status + count + project.id;

                        let progressBar = document.createElement("div");
                        progressBar.className = "progress-bar";
                        progressBar.style.backgroundColor = colors[status];
                        progressBar.style.color = "black";
                        progressBar.innerHTML = count;

                        progress.appendChild(progressBar);
                        progressbar.appendChild(progress);
                    }
                });
                footer.appendChild(progressbar);
            }

            // Add a button to select the project
            let select = document.createElement("a");
            select.type = "button";
            select.id = "selectProjectButton" + project.id;
            select.className = "btn btn-teal d-block";
            select.href = "projects/" + project.id + "/views?view=board";
            select.innerHTML = "Auswählen";
            footer.appendChild(select);

            body.appendChild(footer);
            card.appendChild(body);
            cards.appendChild(card);
        });
    }
    return 0;
}

// Sorting projects
let sortDirection = {
    name: true,
    members: true,
    taskStatus: true
};

function updateButtonLabel(criteria, isAscending) {
    const buttons = ["sortName", "sortManager", "sortMembers"];
    buttons.forEach(btn => {
        document.getElementById(btn).classList.remove('active-sort');
        document.getElementById(btn + 'Icon').innerHTML = '▲';
    });

    const activeButton = document.getElementById('sort' + criteria.charAt(0).toUpperCase() + criteria.slice(1));
    const icon = document.getElementById('sort' + criteria.charAt(0).toUpperCase() + criteria.slice(1) + 'Icon');
    activeButton.classList.add('active-sort');
    icon.innerHTML = isAscending ? '▲' : '▼';
}

function sortProjects(criteria) {
    let projectCards = Array.from(document.querySelectorAll('[id^="projectCard"]'));
    let createProjectCard = document.getElementById("createProjectCard");
    let cardsContainer = document.getElementById("cards");

    projectCards.sort((a, b) => {
        let comparison = 0;

        switch (criteria) {
            case "name":
                let nameA = a.querySelector('#projectTitle').innerText.toLowerCase();
                let nameB = b.querySelector('#projectTitle').innerText.toLowerCase();

                comparison = nameA.localeCompare(nameB);
                break;
            case "manager":

                let managerA = b.dataset.projectManager.toLowerCase();
                let managerB = a.dataset.projectManager.toLowerCase();

                comparison = managerA.localeCompare(managerB);
                break;
            case "members":
                let membersA = parseInt(b.querySelector('#user h5').innerText, 10);
                let membersB = parseInt(a.querySelector('#user h5').innerText, 10);

                comparison = membersA - membersB;
                break;
            default:
                break;
        }

        return sortDirection[criteria] ? comparison : -comparison;
    });

    sortDirection[criteria] = !sortDirection[criteria];

    updateButtonLabel(criteria, criteria === "manager" ? !sortDirection[criteria] : sortDirection[criteria]);

    // Refill the card container
    cardsContainer.innerHTML = ''; // Delete all cards
    projectCards.forEach(card => {
        cardsContainer.appendChild(card); // Add sorted cards again
    });
    cardsContainer.appendChild(createProjectCard); // Add create task card lastly
}
