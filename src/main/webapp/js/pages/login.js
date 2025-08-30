import { AuthService } from '../services/auth.js';

function showForgotPassword() {
    alert('Funkcionalnost za resetovanje lozinke će biti implementirana uskoro!');
}


async function handleLoginSubmit(e) {
    e.preventDefault();

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    if (!username) {
        UIElements.showMessage('error', 'Molimo unesite korisničko ime ili email!');
        return;
    }

    if (!password) {
        UIElements.showMessage('error', 'Molimo unesite lozinku!');
        return;
    }

    if (password.length < 6) {
        UIElements.showMessage('error', 'Lozinka mora imati najmanje 6 karaktera!');
        return;
    }

    UIElements.setButtonLoading('loginBtn', 'Prijavljujem...', 'hourglass_empty');

    try {
        console.log('Login attempt:', { username, password });
        AuthService.login({ username, password });
    } catch (error) {
        UIElements.showMessage('error', 'Greška pri prijavi. Pokušajte ponovo.');
    } finally {
        UIElements.setButtonNormal('loginBtn', 'Prijavi se', 'login');
    }
}


document.addEventListener('DOMContentLoaded', function () {
    const params = new URLSearchParams(window.location.search);
    const error = params.get("error");

    if (error === "wrongCredentials") {
        UIElements.showMessage('error', 'Pogrešni podaci. Pokušajte ponovo.', 1000 * 20);
    } else if (error === "notAuth") {
        UIElements.showMessage('error', 'Niste prijavljeni. Ne možete pristupiti admin stranici.', 1000 * 20);
    }

    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLoginSubmit);
    }
});


window.showForgotPassword = showForgotPassword;