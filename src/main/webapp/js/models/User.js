export class User {
    constructor(userData = null) {
        if (!userData) {
            const saved = localStorage.getItem('currentUser');
            if (saved) {
                try {
                    userData = JSON.parse(saved);
                } catch (e) {
                    console.error('Greška pri parsiranju korisnika iz localStorage:', e);
                    userData = {};
                }
            } else {
                userData = {};
            }
        }

        this.id = userData.id || null;
        this.username = userData.username || '';
        this.email = userData.email || '';
        this.firstName = userData.firstName || '';
        this.lastName = userData.lastName || '';
        this.profileImage = userData.profileImage || 'default-avatar.png';
        this.createdAt = userData.createdAt || null;
        this.lastLogin = userData.lastLogin || null;
    }


    getId() {
        return this.id;
    }

    getFullName() {
            return `${this.firstName} ${this.lastName}` || '';
    }

    getName() {
        return this.firstName || '';
    }

    getUsername() {
        return this.username || '';
    }

    getProfileImageUrl() {
        return `/images/profiles/${this.profileImage}`;
    }


    isProfileComplete() {
        return this.firstName && this.lastName && this.email;
    }

    getFormattedCreatedAt() {
        if (this.createdAt) {
            return new Date(this.createdAt).toLocaleDateString('sr-RS');
        }
        return 'Nepoznato';
    }

    getFormattedLastLogin() {
        if (this.lastLogin) {
            return new Date(this.lastLogin).toLocaleString('sr-RS');
        }
        return 'Nikad';
    }

    static fromJson(jsonData) {
        return new User(jsonData);
    }

    toJson() {
        return {
            id: this.id,
            username: this.username,
            email: this.email,
            firstName: this.firstName,
            lastName: this.lastName,
            profileImage: this.profileImage,
            createdAt: this.createdAt,
            lastLogin: this.lastLogin
        };
    }

    updateData(newData) {
        Object.keys(newData).forEach(key => {
            if (this.hasOwnProperty(key)) {
                this[key] = newData[key];
            }
        });
    }
}
