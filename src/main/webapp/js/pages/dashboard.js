import { AuthService } from '../services/auth.js';


const categoryIcons = {
    'Programiranje': 'code',
    'Historija': 'account_balance',
    'Matematika': 'calculate',
    'Priroda': 'nature',
    'Sport': 'sports_soccer',
    'Geografija': 'public',
    'Nauka': 'science',
    'Muzika': 'music_note',
    'Film': 'movie'
};

function logOut() {
    event.preventDefault();
    console.log("logoutam se");
    AuthService.logout();
}


async function loadUserQuizzes() {
    try {
        const userId = currentUser.getId();
        const response = await fetch(`/trivia/admin/api/quiz/${userId}`);

        if (!response.ok) {
            throw new Error('Greška pri učitavanju kvizova');
        }

        const quizzes = await response.json();
        displayQuizzes(quizzes);
        updateStats(quizzes);

    } catch (error) {
        console.error('Greška pri učitavanju kvizova:', error);
        showError('Nije moguće učitati kvizove. Molimo pokušajte ponovo.');
    }
}


function displayQuizzes(quizzes) {
    const quizzesGrid = document.querySelector('.quizzes-grid');


    const addQuizCard = quizzesGrid.querySelector('.add-quiz-card');


    const existingQuizCards = quizzesGrid.querySelectorAll('.quiz-card');
    existingQuizCards.forEach(card => card.remove());


    quizzes.forEach(quiz => {
        const quizCard = createQuizCard(quiz);
        quizzesGrid.appendChild(quizCard);
    });
}


function createQuizCard(quiz) {
    const quizCard = document.createElement('div');
    quizCard.className = 'quiz-card';


    let imageContent = '';
    if (quiz.quizImage && quiz.quizImage.trim() !== '') {

        imageContent = `<img src="/trivia/uploads/${quiz.quizImage}" alt="${quiz.title}" class="quiz-image-photo" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
                       <div class="quiz-image-fallback" style="display: none;">
                           <i class="material-icons">${categoryIcons[quiz.category] || 'quiz'}</i>
                       </div>`;
    } else {

        imageContent = `<i class="material-icons">${categoryIcons[quiz.category] || 'quiz'}</i>`;
    }

    quizCard.innerHTML = `
        <div class="quiz-image">
            ${imageContent}
        </div>
        <div class="quiz-content">
            <span class="quiz-category">${quiz.category}</span>
            <h3 class="quiz-title">${quiz.title}</h3>
            <p class="quiz-description">${quiz.description}</p>
            <div class="quiz-actions">
                <a href="#" class="btn-start" onclick="startQuiz(${quiz.id})">Započni kviz</a>
                <button class="btn-edit" onclick="editQuiz(${quiz.id})">
                    <i class="material-icons">edit</i>
                </button>
            </div>
        </div>
    `;


    if (!quiz.isActive) {
        quizCard.classList.add('disabled');
    }

    return quizCard;
}
function startQuiz(quizId) {
    event.preventDefault();
    console.log(`Započinje kviz sa ID: ${quizId}`);


    const url = `/trivia/admin/createQuizSession?quizId=${quizId}`;


    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Odgovor servera:', data);

            if (data.success) {

                console.log('Sesija kreirana! PIN:', data.session.sessionPin);


                window.location.href = `startQuiz.html?quizId=${quizId}&sessionPin=${data.session.sessionPin}`;

            } else {

                console.error('Greška:', data.message);
                alert('Greška pri kreiranju sesije: ' + data.message);
            }
        })
        .catch(error => {
            console.error('Greška pri slanju zahtjeva:', error);
            alert('Došlo je do greške pri kreiranju sesije. Pokušajte ponovo.');
        })
        .finally(() => {


        });
}


function editQuiz(quizId) {
    event.preventDefault();
    console.log(`Uređuje kviz sa ID: ${quizId}`);

    window.location.href = `createQuiz.html?id=${quizId}`;
}


function showError(message) {

    alert(message);
}


function updateStats(quizzes) {
    const activeQuizzes = quizzes.filter(quiz => quiz.isActive);
    const totalQuizzes = quizzes.length;


    const statCards = document.querySelectorAll('.stat-card');
    if (statCards.length > 0) {

        const createdQuizzesElement = statCards[0].querySelector('.stat-number');
        if (createdQuizzesElement) {
            createdQuizzesElement.textContent = totalQuizzes;
        }



    }
}

document.addEventListener('DOMContentLoaded', () => {
    const userDataString = localStorage.getItem("currentUser");
    if (userDataString) {
        const userData = JSON.parse(userDataString);
        document.querySelector(".dashboard-header h1").textContent = `Dobrodošli nazad, ${currentUser.getName()}!`;
        document.querySelector(".user-info h3").textContent = `${currentUser.getFullName()}`;
        document.querySelector(".user-info p").textContent = `@${currentUser.getUsername()}`;


        loadUserQuizzes();
    }

    console.log("Dashboard loaded");
});

function createQuiz() {
    console.log("Kreiram kviz");
    window.location.href = "/trivia/admin/createQuiz.html";
}


window.logOut = logOut;
window.startQuiz = startQuiz;
window.editQuiz = editQuiz;
window.createQuiz = createQuiz;