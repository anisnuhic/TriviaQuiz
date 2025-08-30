import { User } from '../models/User.js';

export class AuthService {
    static async login(credentials) {
        console.log(credentials);
        console.log('auth.login');

        let req = new XMLHttpRequest();
        req.open('POST', '/trivia/login', true);
        req.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');

        req.onload = function () {
            console.log(req.status);

            if (req.status === 200) {
                const responseData = JSON.parse(this.response);
                console.log(responseData);


                localStorage.setItem('currentUser', JSON.stringify(responseData.user));
                if (responseData.isSuperadmin === true) {
                    window.location.href = '/trivia/admin/superadmin.html';
                    localStorage.setItem('currentUser', 'superadmin');
                } else {
                    window.location.href = '/trivia/admin/dashboard.html';
                }
            } else {
                window.location.href = '/trivia/loginPage.html?error=wrongCredentials';
            }
        };

        req.send(`usernameOrEmail=${credentials.username}&password=${credentials.password}`);
    }

    static async signup(userData) {
        try {

            const params = new URLSearchParams();
            params.append('username', userData.username);
            params.append('email', userData.email);
            params.append('password', userData.password);
            params.append('confirmPassword', userData.confirmPassword);


            if (userData.firstName) params.append('firstName', userData.firstName);
            if (userData.lastName) params.append('lastName', userData.lastName);


            const response = await fetch('/trivia/signup', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params,
                credentials: 'include'
            });


            const result = await response.json();

            return result;

        } catch (error) {
            console.error('Signup error:', error);
            return {
                success: false,
                message: 'Greška pri povezivanju sa serverom!'
            };
        }
    }

    static async joinQuiz(pin) {

        const validation = this.validatePin(pin);
        if (!validation.valid) {
            return {
                success: false,
                message: validation.message
            };
        }
        return {
            success: true,
            redirectUrl: ''
        };

        try {

            const response = await fetch(`/quiz/join?pin=${encodeURIComponent(pin)}`, {
                method: 'GET',
                credentials: 'include'
            });

            if (response.ok) {
                if (response.redirected) {

                    return {
                        success: true,
                        redirectUrl: response.url
                    };
                } else {

                    const text = await response.text();
                    const errorMessage = extractErrorMessage(text);
                    return {
                        success: false,
                        message: errorMessage || 'Kviz sa ovim PIN-om ne postoji!'
                    };
                }
            } else {
                return {
                    success: false,
                    message: `Server greška: ${response.status}`
                };
            }

        } catch (error) {
            console.error('Join quiz error:', error);
            return {
                success: false,
                message: 'Greška pri povezivanju sa serverom!'
            };
        }
    }


    static async logout() {
        localStorage.removeItem("currentUser");
        let req = new XMLHttpRequest();
        req.open('GET', '/trivia/admin/logout', true);
        req.send();
        req.onload = function () {
            window.location.href = '/trivia';
        }
    }


    static async checkAuth() {
        try {
            const response = await fetch('/auth/check', {
                method: 'GET',
                credentials: 'include'
            });

            if (response.ok) {
                const data = await response.json();
                return { authenticated: true, user: data };
            } else {
                return { authenticated: false };
            }

        } catch (error) {
            console.error('Auth check error:', error);
            return { authenticated: false };
        }
    }


    static validatePin(pin) {
        if (!pin) return { valid: false, message: 'PIN je obavezan!' };
        if (pin.length < 4) return { valid: false, message: 'PIN mora imati najmanje 4 karaktera!' };
        if (pin.length > 10) return { valid: false, message: 'PIN ne može biti duži od 10 karaktera!' };
        if (!/^[A-Z0-9]+$/.test(pin)) return { valid: false, message: 'PIN može sadržavati samo slova i brojeve!' };

        return { valid: true };
    }
}


function extractErrorMessage(html) {
    try {

        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');


        const errorElement = doc.querySelector('.error, .error-message, [class*="error"]');
        if (errorElement) {
            return errorElement.textContent.trim();
        }


        const match = html.match(/\$\{errorMessage\}|class="error"[^>]*>([^<]+)</);
        if (match && match[1]) {
            return match[1].trim();
        }

        return null;
    } catch (error) {
        console.error('Error parsing HTML:', error);
        return null;
    }
}