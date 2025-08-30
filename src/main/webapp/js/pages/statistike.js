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

async function loadUserQuizzesForStats() {
    try {
        const userId = currentUser.getId();
        const response = await fetch(`/trivia/admin/api/quiz/${userId}`);

        if (!response.ok) {
            throw new Error('Greška pri učitavanju kvizova');
        }

        const quizzes = await response.json();
        displayQuizzesForStats(quizzes);

    } catch (error) {
        console.error('Greška pri učitavanju kvizova:', error);
        showError('Nije moguće učitati kvizove. Molimo pokušajte ponovo.');
    }
}

function displayQuizzesForStats(quizzes) {
    const quizzesGrid = document.getElementById('statistikeQuizzesGrid');

    if (quizzes.length === 0) {
        quizzesGrid.innerHTML = `
            <div class="empty-statistics">
                <i class="material-icons">bar_chart</i>
                <h3>Nema kvizova za statistike</h3>
                <p>Kreirajte kvizove da biste videli njihove statistike</p>
            </div>
        `;
        return;
    }

    quizzesGrid.innerHTML = '';

    quizzes.forEach(quiz => {
        const quizCard = createQuizCardForStats(quiz);
        quizzesGrid.appendChild(quizCard);
    });
}
function createQuizCardForStats(quiz) {
    const quizCard = document.createElement('div');
    quizCard.className = 'quiz-card statistike-card';

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
                <button class="btn-statistike" onclick="showQuizStatistics(${quiz.id}, '${quiz.title}')">
                    <i class="material-icons">bar_chart</i>
                    Prikaži statistike
                </button>
            </div>
        </div>
    `;

    return quizCard;
}

async function showQuizStatistics(quizId, quizTitle) {
    try {

        const modal = document.getElementById('statistikeModal');
        const modalTitle = document.getElementById('modalQuizTitle');
        modalTitle.textContent = `Statistike - ${quizTitle}`;
        modal.classList.add('active');


        document.getElementById('totalParticipants').textContent = '0';
        document.getElementById('totalSessions').textContent = '0';
        document.getElementById('avgScore').textContent = '0%';
        document.getElementById('bestResultsTable').innerHTML = '<p style="text-align: center; color: rgba(255,255,255,0.7); padding: 20px;">Učitavanje...</p>';


        const response = await fetch(`/trivia/scores?quizId=${quizId}`);

        if (!response.ok) {
            throw new Error('Greška pri učitavanju statistika');
        }

        const stats = await response.json();
        displayStatistics(stats);

    } catch (error) {
        console.error('Greška pri učitavanju statistika:', error);


        document.getElementById('bestResultsTable').innerHTML = `
            <div style="text-align: center; padding: 20px; color: var(--error-color);">
                <i class="material-icons" style="font-size: 2rem; margin-bottom: 10px;">error_outline</i>
                <p>Greška pri učitavanju statistika</p>
            </div>
        `;
    }
}


function displayStatistics(stats) {

    document.getElementById('totalParticipants').textContent = stats.totalParticipants || 0;
    document.getElementById('totalSessions').textContent = stats.totalSessions || 0;
    document.getElementById('avgScore').textContent = `${stats.avgScore || 0}%`;


    const bestResultsTable = document.getElementById('bestResultsTable');

    if (!stats.bestResults || stats.bestResults.length === 0) {
        bestResultsTable.innerHTML = `
            <div style="text-align: center; padding: 20px; color: rgba(255,255,255,0.7);">
                <i class="material-icons" style="font-size: 2rem; margin-bottom: 10px;">people_outline</i>
                <p>Nema rezultata za prikaz</p>
            </div>
        `;
        return;
    }

    let tableHTML = '';

    stats.bestResults.forEach((result, index) => {
        const rank = index + 1;
        const avatar = result.username.charAt(0).toUpperCase();
        const rankClass = rank <= 3 ? `rank-${rank}` : '';

        tableHTML += `
            <div class="table-row">
                <div class="rank-cell">
                    <div class="rank-number ${rankClass}">${rank}</div>
                </div>
                <div class="name-cell">
                    <div class="participant-avatar">${avatar}</div>
                    <span class="participant-name">${result.username}</span>
                </div>
                <div class="score-cell">
                    <span class="score-points">${result.score}</span>
                    <span class="score-label">bodova</span>
                </div>
            </div>
        `;
    });

    bestResultsTable.innerHTML = tableHTML;
}


function closeModal() {
    const modal = document.getElementById('statistikeModal');
    modal.classList.remove('active');
}


function showError(message) {
    alert(message);
}


document.addEventListener('DOMContentLoaded', () => {
    const userDataString = localStorage.getItem("currentUser");
    if (userDataString) {
        loadUserQuizzesForStats();
    }


    const closeModalBtn = document.getElementById('closeModal');
    const modalOverlay = document.getElementById('statistikeModal');

    closeModalBtn.addEventListener('click', closeModal);

    modalOverlay.addEventListener('click', (e) => {
        if (e.target === modalOverlay) {
            closeModal();
        }
    });


    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeModal();
        }
    });

    console.log("Statistike loaded");
});


window.showQuizStatistics = showQuizStatistics;
window.closeModal = closeModal;