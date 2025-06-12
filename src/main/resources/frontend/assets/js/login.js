addEventListener("DOMContentLoaded", async () => {
    //We wait so the user has visual feedback that the page is loading and the login was unsuccessful
    await sleep(200)
    const error = getQueryParam("error");
    if (error != null) {
        displayErrorText("Passwort oder Benutzerdaten falsch!", "warning")
    }
    const logout = getQueryParam("logout");
    if (logout != null) {
        displayErrorText("Erfolgreich ausgeloggt.", "dark-gray")
    }
});

//submits the information on keypress enter
function passwordEnterKey(event) {
    if (event.key === "Enter") {
        const form = document.getElementById("loginForm");
        form.submit();
    }
}

//focuses the next input on tab
function handleKeyPress(event, nextFieldId) {
    if (event.keyCode === 13) {
        document.getElementById(nextFieldId).focus();
        return false; // Prevent form submission
    }
    return true;
}

function getQueryParam(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function displayErrorText(message, color) {
    document.getElementById("errorField").innerHTML = `<h6 
    style="color: var(${"--" + color}); margin-bottom: 20px">${message}</h6>`
}

const passwordEyeButton = document.getElementById("togglePassword");
const passwordEyeIcon = document.getElementById("passwordEye");
const password = document.getElementById("password");

//switches the password visibility on click on the eye symbol
passwordEyeButton.addEventListener("click", function () {
    if (password.type === "password") {
        password.type = "text";
        passwordEyeIcon.src = "assets/svg/eye-slash.svg"
    } else {
        password.type = "password";
        passwordEyeIcon.src = "assets/svg/eye.svg"
    }
});
