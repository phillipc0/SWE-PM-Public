document.getElementById("submitPassword").addEventListener('click', onClickSubmit)

// Validates password and sends request to backend
async function onClickSubmit() {
    const password = document.getElementById("newPassword").value
    const confirmPassword = document.getElementById("confirmPassword").value

    if (await checkPasswords(password, confirmPassword)) {
        const data = {"password": password};
        const options = {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data)
        };
        const response = await fetch("api/v1/users/current/password", options);
        if (response.status === 409) {
            displayErrorInInfoField('Passwort ist gleich mit bestehendem!')
        } else if (response.status === 403) {
            displayErrorInInfoField('Passwort ist bereits geändert!')
        } else {
            navigator.clipboard.writeText(password).then(function () {

            }).catch(function (err) {
            });
            window.location = "/logout"
        }
    }
}

//Checks passwords before sending the request and shows errors in div
async function checkPasswords(password, confirmPassword) {
    if (password !== confirmPassword) {
        displayErrorInInfoField('Passwörter sind nicht gleich!')
        return false;
    }
    const regex = new RegExp('(?=.*[a-zäöüß])(?=.*[A-ZÄÖÜ])(?=.*\\d)(?=.*[\\^°~|!"§$%&/()=?+*#\\-_.:,;<>\'`´}\\]\\[{@]).{12,}');
    return regex.test(password);
}

// Generates password and enters it into both password fields
function requestPasswords() {
    let pwLength = 16;
    const pw = generatePassword(pwLength);
    navigator.clipboard.writeText(pw).then(function () {
        document.getElementById("newPassword").value = pw
        document.getElementById("confirmPassword").value = pw
    }).catch(function (err) {
    });
    validatePasswordChange();
}

function validatePasswordChange() {
    const colorConfirm = getComputedStyle(document.documentElement).getPropertyValue("--pw-confirm").trim()
    const colorInvalid = getComputedStyle(document.documentElement).getPropertyValue("--pw-invalid").trim()
    const passwordArea = document.getElementById('passwordInputInfo');
    const password = document.getElementById("newPassword").value
    passwordArea.innerHTML = "";

    let requirements = [
        {regex: /.{12,}/, message: "Mindestens 12 Zeichen"},
        {regex: /(?=.*[a-zäöüß])/, message: "Kleinbuchstabe"},
        {regex: /(?=.*[A-ZÄÖÜ])/, message: "Großbuchstabe"},
        {regex: /(?=.*\d)/, message: "Ziffer"},
        {regex: /(?=.*[\^°~|!"§$%&/()=?+*#\-_.:,;\\<>'`´}\]\[{@])/, message: "Sonderzeichen"}
    ];

    requirements.forEach(requirement => {
        let passwordInfo = document.createElement("h6");
        passwordInfo.innerHTML = requirement.message;
        passwordInfo.style.color = requirement.regex.test(password) ? colorConfirm : colorInvalid;
        passwordArea.appendChild(passwordInfo);
    })
}

// Toggling the readability of the password fields
const passwordNewEyeButton = document.getElementById('toggleNewPassword');
const passwordConfirmEyeButton = document.getElementById('toggleConfirmPassword');
const newPasswordEyeIcon = document.getElementById('newPasswordEye');
const confirmPasswordEyeIcon = document.getElementById('confirmPasswordEye');
const newPassword = document.getElementById('newPassword');
const confirmPassword = document.getElementById('confirmPassword');
passwordNewEyeButton.addEventListener('click', function () {
    if (newPassword.type === "password") {
        newPassword.type = "text";
        newPasswordEyeIcon.src = "assets/svg/eye-slash.svg"
    } else {
        newPassword.type = "password";
        newPasswordEyeIcon.src = "assets/svg/eye.svg"
    }
});
passwordConfirmEyeButton.addEventListener('click', function () {
    if (confirmPassword.type === "password") {
        confirmPassword.type = "text";
        confirmPasswordEyeIcon.src = "assets/svg/eye-slash.svg"
    } else {
        confirmPassword.type = "password";
        confirmPasswordEyeIcon.src = "assets/svg/eye.svg"
    }
});

// Utility method for error messages
function displayErrorInInfoField(message) {
    document.getElementById('passwordInputInfo').innerHTML = `<h6 
    style="color: var(--warning)">${message}</h6>`
}
