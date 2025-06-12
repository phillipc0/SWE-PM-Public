function getProjectIdFromUrl() {
    const urlSegments = window.location.pathname.split('/');
    return urlSegments[2];
}

//Password generation
function generatePassword(passwordLength) {
    const numberChars = "0123456789";
    const upperChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    const lowerChars = "abcdefghijklmnopqrstuvwxyz";
    const specialChars = "!\"§$%&/()=?+*#-_"
    const allChars = numberChars + upperChars + lowerChars;
    let randPasswordArray = Array(passwordLength);
    randPasswordArray[0] = numberChars;
    randPasswordArray[1] = upperChars;
    randPasswordArray[2] = lowerChars;
    randPasswordArray[3] = specialChars;
    randPasswordArray = randPasswordArray.fill(allChars, 4);
    return shuffleArray(randPasswordArray.map(function (x) {
        return x[Math.floor(Math.random() * x.length)]
    })).join('');
}

function shuffleArray(array) {
    for (let i = array.length - 1; i > 0; i--) {
        let j = Math.floor(Math.random() * (i + 1));
        let temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
    return array;
}

function truncateText(element, text, maxWidth) {
    const canvas = document.createElement("canvas");
    const context = canvas.getContext("2d");

    const style = window.getComputedStyle(element);
    context.font = `${style.fontSize} ${style.fontFamily}`;

    let truncatedText = text;
    let textWidth = context.measureText(truncatedText).width;

    while (textWidth > maxWidth && truncatedText.length > 0) {
        truncatedText = truncatedText.substring(0, truncatedText.length - 1);
        textWidth = context.measureText(truncatedText + '...').width;
    }

    if (truncatedText.length < text.length) {
        element.innerHTML = truncatedText + '...';
        element.title = unescapeHTML(text);
    } else {
        element.innerHTML = text;
    }
}

function apiDateToFrontendDate(apiDate) {
    if (apiDate !== null) {
        const date = new Date(apiDate);

        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0'); // Monate sind 0-basiert
        const year = date.getFullYear();

        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');

        return /^\d{4}-\d{2}-\d{2}$/.test(apiDate) ? `${day}.${month}.${year}` : `${day}.${month}.${year} ${hour}:${minute}`;
    } else {
        return "";
    }
}

function dateTimeToDate(dateTime) {
    if (dateTime === null) return null;
    const dateObject = new Date(dateTime);
    const year = dateObject.getFullYear();
    const month = String(dateObject.getMonth() + 1).padStart(2, "0");
    const day = String(dateObject.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function autoResize(textarea) {
    textarea.style.height = 'auto'; // Setze die Höhe auf 'auto', um eine neue Berechnung zu ermöglichen
    textarea.style.height = textarea.scrollHeight + 'px'; // Passe die Höhe an den Inhalt an
}

// Helper function to trigger the download of the ICS file
function downloadICSFile(projectJSON, userIDMap, task) {
    const endDate = new Date(new Date(task.dueDate).getTime() + 24 * 60 * 60 * 1000); // add one day
    const currentUrl = window.location.origin; // dynamically retrieves the base URL of the current page

    // HTML-Inhalt für X-ALT-DESC
    const htmlDescription = `
        <html lang="DE"><body>
        <b>Beschreibung:</b> ${task.description.replace(/\n/g, "<br>")}<br>
        <b>Projekt:</b> ${projectJSON.name}<br>
        <b>Projektleiter:</b> ${projectJSON.projectManager.name}, ${projectJSON.projectManager.emailAddress}<br>
        <b>Mitarbeiter:</b> ${task.assignedUsers.map(user => userIDMap[user]).filter(Boolean).join(", ")}<br>
        <b>Link:</b> <a href="${currentUrl}/projects/${projectJSON.id}/views?view=list">${currentUrl}/projects/${projectJSON.id}/views?view=list</a><br>
        <b>Stand vom:</b> ${formatDateForAllDay(new Date(), "DE")}
        </body></html>
    `;

    // ICS-Inhalt mit X-ALT-DESC
    const icsContent =
        `BEGIN:VCALENDAR
VERSION:2.0
BEGIN:VEVENT
DESCRIPTION:Beschreibung: ${task.description.replace(/\n/g, "\\n")}\\n
 Projekt: ${projectJSON.name}\\n
 Projektleiter: ${projectJSON.projectManager.name}, ${projectJSON.projectManager.emailAddress}\\n
 Mitarbeiter: ${task.assignedUsers.map(user => userIDMap[user]).filter(Boolean).join(", ")}\\n
 Link: ${currentUrl}/projects/${projectJSON.id}/views?view=list\\n
 Stand vom ${formatDateForAllDay(new Date(), "DE")}
X-ALT-DESC;FMTTYPE=text/html:${htmlDescription.trim()}
DTSTART;VALUE=DATE:${formatDateForAllDay(task.dueDate, "Google")}
DTEND;VALUE=DATE:${formatDateForAllDay(endDate.toISOString(), "Google")}
SUMMARY;LANGUAGE=de:${unescapeHTML(task.title)}
BEGIN:VALARM
END:VALARM
END:VEVENT
END:VCALENDAR`; // Do not add a linebreak, because it breaks the ics in some calendars

    const blob = new Blob([icsContent], {type: 'text/calendar'});
    const url = URL.createObjectURL(blob);

    const ics = document.createElement("a");
    ics.href = url;
    ics.download = `${unescapeHTML(task.title)}.ics`; // Setze den Dateinamen
    ics.click();

    URL.revokeObjectURL(url); // Speicher freigeben
}

// Helper function to format date for Outlook
function formatDateForAllDay(dateString, formatType) {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    if (formatType === "Google") {
        return `${year}${month}${day}`;
    }
    if (formatType === "Microsoft") {
        return `${year}-${month}-${day}`;
    }
    if (formatType === "DE") {
        return `${day}.${month}.${year}`;
    } else {
        return date;
    }
}

function renderCalendarLinks(projectJSON, userIDMap, task) {
    const startDateOutlook = formatDateForAllDay(task.dueDate, "Microsoft");
    const endDateOutlook = formatDateForAllDay(new Date(new Date(task.dueDate).getTime() + 24 * 60 * 60 * 1000), "Microsoft");
    const startDateGoogle = formatDateForAllDay(task.dueDate, "Google");
    const endDateGoogle = formatDateForAllDay(new Date(new Date(task.dueDate).getTime() + 24 * 60 * 60 * 1000), "Google");

    const descriptionForOutlook = task.description.replace(/\n/g, "<br>");
    let icsDiv = document.createElement("div");

    const currentUrl = window.location.origin;  // Nur die Basis-URL

    icsDiv.className = "calendar-links";
    icsDiv.innerHTML = `
        <a href="https://outlook.office.com/calendar/0/deeplink/compose?subject=${encodeURIComponent(replaceBrackets(unescapeHTML(task.title)))}&body=${encodeURIComponent("<b>Beschreibung:</b> " + descriptionForOutlook + "<br><b>Projekt:</b> " + projectJSON.name + "<br><b>Projektleiter:</b> " + projectJSON.projectManager.name + ", " + projectJSON.projectManager.emailAddress + "<br><b>Mitarbeiter:</b> " + task.assignedUsers.map(user => userIDMap[user]).filter(Boolean).join(", ") + "<br><b>Link:</b> " + currentUrl + "/projects/" + projectJSON.id + "/views?view=list<br><b>Stand vom:</b> " + formatDateForAllDay(new Date(), "DE"))}&startdt=${startDateOutlook}&enddt=${endDateOutlook}&allday=true" target="_blank" title="Zum Outlook Kalender hinzufügen"><img src="/assets/svg/microsoft.svg" alt="Outlook Kalender" class="calendar-icon"></a>
        <a href="https://www.google.com/calendar/render?action=TEMPLATE&text=${encodeURIComponent(task.title)}&dates=${startDateGoogle}/${endDateGoogle}&details=${encodeURIComponent("<b>Beschreibung:</b> " + task.description + "<br><b>Projekt:</b> " + projectJSON.name + "<br><b>Projektleiter:</b> " + projectJSON.projectManager.name + ", " + projectJSON.projectManager.emailAddress + "<br><b>Mitarbeiter:</b> " + task.assignedUsers.map(user => userIDMap[user]).filter(Boolean).join(", ") + "<br><b>Link:</b> " + currentUrl + "/projects/" + projectJSON.id + "/views?view=list<br><b>Stand vom:</b> " + formatDateForAllDay(new Date(), "DE"))}&location=&sf=true&output=xml" target="_blank" title="Zum Google Kalender hinzufügen"><img src="/assets/svg/google.svg" alt="Google Kalender" class="calendar-icon"></a>
        <a href="javascript:void(0);" id="download-ics-${task.id}" title="Als ICS-Datei herunterladen"><img src="/assets/svg/calendar-plus.svg" alt="ICS Datei" class="calendar-icon"></a>
    `;

    let isDownloading = false;  // Globale Variable zum Verhindern von Doppelklicks

    setTimeout(() => {  // Timeout to make sure the buttons are loaded in the DOM before the EventListener is added
        const icsButton = document.getElementById(`download-ics-${task.id}`);
        if (icsButton) {
            icsButton.addEventListener("click", function () {
                if (!isDownloading) {
                    isDownloading = true;
                    downloadICSFile(projectJSON, userIDMap, task);  // Pass the specific task

                    // Timeout to avoid duplicate downloads; Wait one second
                    setTimeout(() => {
                        isDownloading = false;
                    }, 1000);
                }
            });
        }
    }, 0);
    return icsDiv.innerHTML;
}

function unescapeHTML(str) {
    if (str === null) return null;
    return str.replace(/&amp;/g, "&")
        .replace(/&lt;/g, "<")
        .replace(/&gt;/g, ">")
        .replace(/&quot;/g, "\"")
        .replace(/&#039;/g, "'");
}

function escapeHTML(str) {
    if (str === null) return null;
    return str.replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function replaceBrackets(str) {
    if (str === null) return null;
    return str.replace(/</g, "[")
        .replace(/>/g, "]");
}

function toggleInfoText() {
    const infoText = document.getElementById('infoText');
    if (infoText.style.display === 'none' || infoText.style.display === '') {
        infoText.style.display = 'block';
    } else {
        infoText.style.display = 'none';
    }
}

function toggleInfoTextCreateTask() {
    const infoText = document.getElementById('infoTextCreateTask');
    if (infoText.style.display === 'none' || infoText.style.display === '') {
        infoText.style.display = 'block';
    } else {
        infoText.style.display = 'none';
    }
}

function toggleInfoTextEditTask() {
    const infoText = document.getElementById('infoTextEditTask');
    if (infoText.style.display === 'none' || infoText.style.display === '') {
        infoText.style.display = 'block';
    } else {
        infoText.style.display = 'none';
    }
}

function toggleInfoTextCreateProject() {
    const infoText = document.getElementById('infoTextCreateProject');
    if (infoText.style.display === 'none' || infoText.style.display === '') {
        infoText.style.display = 'block';
    } else {
        infoText.style.display = 'none';
    }
}

document.addEventListener("DOMContentLoaded", function () {
    // Überprüfen, ob Dark Mode im localStorage aktiviert ist
    if (localStorage.getItem('darkMode') === 'enabled') {
        document.body.classList.add('dark-mode');
    }
});