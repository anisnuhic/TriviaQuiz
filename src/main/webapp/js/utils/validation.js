export class Validator {
    static email(value) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
    }

    static username(value) {
        return /^[a-zA-Z0-9_]{3,20}$/.test(value);
    }

    static password(value) {
        return value.length >= 8 && 
               /[a-z]/.test(value) && 
               /[A-Z]/.test(value) && 
               /[0-9]/.test(value);
    }

    static fullName(value) {
        return value.trim().length >= 2 && value.includes(' ');
    }

    static passwordStrength(password) {
        if (password.length === 0) return { score: 0, text: 'Unesite lozinku' };

        let score = 0;
        if (password.length >= 8) score++;
        if (/[a-z]/.test(password)) score++;
        if (/[A-Z]/.test(password)) score++;
        if (/[0-9]/.test(password)) score++;
        if (/[^A-Za-z0-9]/.test(password)) score++;

        const levels = [
            { class: 'weak', text: 'Slaba lozinka' },
            { class: 'medium', text: 'Srednja lozinka' },
            { class: 'strong', text: 'Jaka lozinka' },
            { class: 'very-strong', text: 'Vrlo jaka lozinka' }
        ];

        const level = Math.min(score - 1, levels.length - 1);
        return { score, ...levels[Math.max(0, level)] };
    }
}