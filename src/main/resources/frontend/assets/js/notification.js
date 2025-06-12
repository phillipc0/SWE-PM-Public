let notificationTimers = [];
const notificationContainer = document.getElementById("notificationContainer");

function addNotificationTimer(timerId) {
    notificationTimers.push(timerId);
}

function showNotification(message) {
    const notification = document.createElement("div");
    notification.classList.add("notification");

    const icon = document.createElement("img");
    icon.id = "notificationWarningIcon";
    icon.src = "/assets/svg/warning-circle.svg";
    icon.alt = "Warnung";

    notification.appendChild(icon);
    notification.appendChild(document.createTextNode(message));

    notificationContainer.appendChild(notification);

    // Timer zum Entfernen der Benachrichtigung nach 5 Sekunden
    let hideTimeout = setTimeout(() => hideNotification(notification), 5000);
    addNotificationTimer(hideTimeout); // Timer zur Liste hinzufügen

    // Timer zurücksetzen, wenn die Maus über der Benachrichtigung ist
    notification.addEventListener("mouseenter", () => clearTimeout(hideTimeout));
    notification.addEventListener("mouseleave", () => {
        hideTimeout = setTimeout(() => hideNotification(notification), 5000);
        addNotificationTimer(hideTimeout); // Timer erneut hinzufügen
    });

    // Event-Listener für Doppelklick hinzufügen, um die Benachrichtigung sofort zu entfernen
    notification.addEventListener("dblclick", () => {
        hideNotification(notification);
    });
}

function hideNotification(notification) {
    notification.classList.add("hide");
    setTimeout(() => notification.remove(), 300); // Animation für das Ausblenden
}
