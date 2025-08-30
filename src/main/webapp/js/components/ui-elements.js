export class UIElements {
    static showMessage(type, message, duration = 5000) {
        const messageDiv = document.getElementById(`${type}Message`);
        const messageText = document.getElementById(`${type}Text`);
        
        if (!messageDiv || !messageText) return;

        messageText.textContent = message;
        messageDiv.style.display = 'block';
        
        
        const otherType = type === 'error' ? 'success' : 'error';
        const otherDiv = document.getElementById(`${otherType}Message`);
        if (otherDiv) otherDiv.style.display = 'none';

        setTimeout(() => {
            messageDiv.style.display = 'none';
        }, duration);
    }

    static validateField(fieldId, isValid, errorMsg, successMsg = '') {
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

    static togglePasswordField(fieldId) {
        const passwordInput = document.getElementById(fieldId);
        const toggleIcon = passwordInput?.nextElementSibling;
        
        if (!passwordInput || !toggleIcon) return;

        if (passwordInput.type === 'password') {
            passwordInput.type = 'text';
            toggleIcon.textContent = 'visibility';
        } else {
            passwordInput.type = 'password';
            toggleIcon.textContent = 'visibility_off';
        }
    }

    static updatePasswordStrength(password) {
        const strengthFill = document.getElementById('strengthFill');
        const strengthText = document.getElementById('strengthText');
        
        if (!strengthFill || !strengthText) return;

        const { class: strengthClass, text } = Validator.passwordStrength(password);
        
        strengthFill.className = `strength-fill ${strengthClass}`;
        strengthText.textContent = text;
    }

    static setButtonLoading(buttonId, loadingText, loadingIcon = 'hourglass_empty') {
        const button = document.getElementById(buttonId);
        if (!button) return;

        button.innerHTML = `<i class="material-icons" style="vertical-align: middle; margin-right: 8px;">${loadingIcon}</i>${loadingText}`;
        button.disabled = true;
    }

    static setButtonNormal(buttonId, normalText, normalIcon) {
        const button = document.getElementById(buttonId);
        if (!button) return;

        button.innerHTML = `<i class="material-icons" style="vertical-align: middle; margin-right: 8px;">${normalIcon}</i>${normalText}`;
        button.disabled = false;
    }
}