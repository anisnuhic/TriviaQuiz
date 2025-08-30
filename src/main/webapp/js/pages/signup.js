import { Validator } from '../utils/validation.js';
import { AuthService } from '../services/auth.js';

function validateField(fieldId, isValid, errorMsg, successMsg = '') {
    const field = document.getElementById(fieldId);
    const icon = document.getElementById(fieldId + 'Icon');
    const error = document.getElementById(fieldId + 'Error');
    const success = document.getElementById(fieldId + 'Success');

    if (isValid) {
        field?.classList.remove('invalid');
        field?.classList.add('valid');
        icon?.classList.remove('invalid');
        icon?.classList.add('valid');
        if (error) error.style.display = 'none';
        if (success && successMsg) {
            success.textContent = successMsg;
            success.style.display = 'block';
        }
    } else {
        field?.classList.remove('valid');
        field?.classList.add('invalid');
        icon?.classList.remove('valid');
        icon?.classList.add('invalid');
        if (success) success.style.display = 'none';
        if (error) {
            error.textContent = errorMsg;
            error.style.display = 'block';
        }
    }

    return isValid;
}

function updatePasswordStrength(password) {
    const strengthFill = document.getElementById('strengthFill');
    const strengthText = document.getElementById('strengthText');
    
    if (!strengthFill || !strengthText) return;

    const { class: strengthClass, text } = Validator.passwordStrength(password);
    
    strengthFill.className = `strength-fill ${strengthClass}`;
    strengthText.textContent = text;
}


async function handleSignupSubmit(e) {
    e.preventDefault();
    
    const fullName = document.getElementById('fullName').value.trim();
    const username = document.getElementById('username').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    
    let isValid = true;
    
    if (!validateField('fullName', Validator.fullName(fullName), 'Ime i prezime mora imati najmanje 2 karaktera i razmak', 'Validno ime i prezime')) {
        isValid = false;
    }
    
    if (!validateField('username', Validator.username(username), 'Korisničko ime mora imati 3-20 karaktera (slova, brojevi, _)', 'Validno korisničko ime')) {
        isValid = false;
    }
    
    if (!validateField('email', Validator.email(email), 'Unesite validnu email adresu', 'Validna email adresa')) {
        isValid = false;
    }
    
    if (!validateField('password', Validator.password(password), 'Lozinka mora imati najmanje 8 karaktera, velika i mala slova, brojeve', '')) {
        isValid = false;
    }
    
    if (!validateField('confirmPassword', password === confirmPassword, 'Lozinke se ne poklapaju', 'Lozinke se poklapaju')) {
        isValid = false;
    }

    if (!isValid) {
        UIElements.showMessage('error', 'Molimo ispravite greške u formi!');
        return;
    }

    UIElements.setButtonLoading('signupBtn', 'Kreiram nalog...', 'hourglass_empty');

    try {
        
        const nameParts = fullName.split(' ');
        const firstName = nameParts[0];
        const lastName = nameParts.slice(1).join(' ');

        
        const userData = {
            username: username,
            email: email,
            password: password,
            confirmPassword: confirmPassword,
            firstName: firstName,
            lastName: lastName
        };

        
        const result = await AuthService.signup(userData);

        if (result.success) {
            UIElements.showMessage('success', result.message);
            UIElements.setButtonNormal('signupBtn', 'Nalog kreiran!', 'check_circle');
            
            
            setTimeout(() => {
                window.location.href = result.redirectUrl || 'loginPage.html';
            }, 2000);
        } else {
            UIElements.showMessage('error', result.message);
            UIElements.setButtonNormal('signupBtn', 'Kreiraj nalog', 'person_add');
        }
        
    } catch (error) {
        console.error('Signup error:', error);
        UIElements.showMessage('error', 'Greška pri kreiranju naloga. Pokušajte ponovo.');
        UIElements.setButtonNormal('signupBtn', 'Kreiraj nalog', 'person_add');
    }
}


document.addEventListener('DOMContentLoaded', function() {
    const signupForm = document.getElementById('signupForm');
    if (signupForm) {
        signupForm.addEventListener('submit', handleSignupSubmit);
    }

    
    const fullNameInput = document.getElementById('fullName');
    if (fullNameInput) {
        fullNameInput.addEventListener('input', function() {
            validateField('fullName', 
                Validator.fullName(this.value),
                'Ime i prezime mora imati najmanje 2 karaktera i razmak',
                'Validno ime i prezime'
            );
        });
    }

    const usernameInput = document.getElementById('username');
    if (usernameInput) {
        usernameInput.addEventListener('input', function() {
            validateField('username', 
                Validator.username(this.value),
                'Korisničko ime mora imati 3-20 karaktera (slova, brojevi, _)',
                'Validno korisničko ime'
            );
        });
    }

    const emailInput = document.getElementById('email');
    if (emailInput) {
        emailInput.addEventListener('input', function() {
            validateField('email', 
                Validator.email(this.value),
                'Unesite validnu email adresu',
                'Validna email adresa'
            );
        });
    }

    const passwordInput = document.getElementById('password');
    if (passwordInput) {
        passwordInput.addEventListener('input', function() {
            const password = this.value;
            updatePasswordStrength(password);
            
            validateField('password', 
                Validator.password(password),
                'Lozinka mora imati najmanje 8 karaktera, velika i mala slova, brojeve',
                ''
            );

            
            const confirmPassword = document.getElementById('confirmPassword');
            if (confirmPassword && confirmPassword.value) {
                validateField('confirmPassword', 
                    confirmPassword.value === password,
                    'Lozinke se ne poklapaju',
                    'Lozinke se poklapaju'
                );
            }
        });
    }

    const confirmPasswordInput = document.getElementById('confirmPassword');
    if (confirmPasswordInput) {
        confirmPasswordInput.addEventListener('input', function() {
            const password = document.getElementById('password').value;
            validateField('confirmPassword', 
                this.value === password,
                'Lozinke se ne poklapaju',
                'Lozinke se poklapaju'
            );
        });
    }
});