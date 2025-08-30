export class ThemeManager {
    constructor() {
        this.loadSavedTheme();
    }

    toggle() {
        document.body.classList.toggle('dark-mode');
        const isDark = document.body.classList.contains('dark-mode');
        localStorage.setItem('theme', isDark ? 'dark' : 'light');
    }

  loadSavedTheme() {
    const savedTheme = localStorage.getItem('theme') || 'dark'; 
    if (savedTheme === 'dark') {
        document.body.classList.add('dark-mode');
    } else {
        document.body.classList.remove('dark-mode');
    }
}
}

