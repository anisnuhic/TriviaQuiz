import { ThemeManager } from './utils/theme.js';
import { UIElements } from './components/ui-elements.js';
import { User } from './models/User.js';

// Globalni objekti
window.themeManager = new ThemeManager();
window.UIElements = UIElements;
window.currentUser = new User();

// Globalne funkcije koje mogu koristiti sve stranice
window.toggleTheme = () => window.themeManager.toggle();
window.togglePasswordField = window.UIElements.togglePasswordField;

// Inicijalizacija
document.addEventListener('DOMContentLoaded', function() {
    console.log('Trivia Quiz App initialized');
    
    // Input animacije za sve forme
    document.querySelectorAll('.form-input').forEach(input => {
        input.addEventListener('focus', function() {
            this.parentElement.style.transform = 'scale(1.02)';
        });
        
        input.addEventListener('blur', function() {
            this.parentElement.style.transform = 'scale(1)';
        });
    });
});