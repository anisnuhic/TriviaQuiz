import { AuthService } from '../services/auth.js';
import { UIElements } from '../components/ui-elements.js';

async function joinQuiz() {
    
    const pin = document.getElementById('pinInput').value.trim();
    
    
    if (!pin) {
        UIElements.showMessage('error', 'Molimo unesite PIN kviza!');
        return;
    }
    
    if (pin.length < 4) {
        UIElements.showMessage('error', 'PIN mora imati najmanje 4 karaktera!');
        return;
    }

    
    UIElements.setButtonLoading('joinBtn', 'Pridružujem...', 'hourglass_empty');

    try {
        
        const result = await AuthService.joinQuiz(pin);
        
        if (result.success) {
            UIElements.showMessage('success', 'Uspješno pristupanje kvizu!');
                window.location.href = `/trivia/startQuizUser.html?pin=${pin}`;
            
        } else {
            UIElements.showMessage('error', result.message || 'Greška pri pristupanju kvizu!');
        }
    } catch (error) {
        console.error('Error joining quiz:', error);
        UIElements.showMessage('error', 'Došlo je do greške. Pokušajte ponovo.');
    } finally {
        
        UIElements.setButtonNormal('joinBtn', 'Pridruži se', 'play_arrow');
    }
}


document.addEventListener('DOMContentLoaded', function() {
    
    const pinInput = document.getElementById('pinInput');
    if (pinInput) {
        pinInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                joinQuiz();
            }
        });

        
        pinInput.addEventListener('input', function(e) {
            e.target.value = e.target.value.toUpperCase();
        });
    }
});


window.joinQuiz = joinQuiz;