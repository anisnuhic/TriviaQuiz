import { AuthService } from '../services/auth.js';

const categoryIcons = {
    'programiranje': 'code',
    'historija': 'account_balance',
    'matematika': 'calculate',
    'priroda': 'nature',
    'sport': 'sports_soccer',
    'geografija': 'public',
    'nauka': 'science',
    'muzika': 'music_note',
    'film': 'movie',
    'hemija': 'science',
    'biologija': 'biotech',
    'fizika': 'physics',
    'opsta-kultura': 'nature'
};


let allQuizzes = [];
let filteredQuizzes = [];
let currentPage = 1;
const quizzesPerPage = 12;

async function loadAllQuizzes() {
    try {
        const response = await fetch('/trivia/admin/api/quiz/superadmin');

        if (!response.ok) {
            throw new Error('Greška pri učitavanju kvizova');
        }

        const quizzes = await response.json();
        allQuizzes = quizzes;
        filteredQuizzes = [...quizzes];
        
        updateQuizCount();
        displayQuizzes();
        setupPagination();

    } catch (error) {
        console.error('Greška pri učitavanju kvizova:', error);
        showError('Nije moguće učitati kvizove. Molimo pokušajte ponovo.');
    }
}

function updateQuizCount() {
    const countElement = document.getElementById('quizCount');
    countElement.textContent = `Ukupno kvizova: ${filteredQuizzes.length}`;
}

function displayQuizzes() {
    const quizzesGrid = document.getElementById('quizzesGrid');
    quizzesGrid.innerHTML = '';

    if (filteredQuizzes.length === 0) {
        quizzesGrid.innerHTML = `
            <div class="empty-statistics" style="grid-column: 1 / -1;">
                <i class="material-icons">quiz</i>
                <h3>Nema kvizova</h3>
                <p>Nisu pronađeni kvizovi koji odgovaraju kriterijumima pretrage.</p>
            </div>
        `;
        document.getElementById('paginationContainer').style.display = 'none';
        return;
    }

    
    const startIndex = (currentPage - 1) * quizzesPerPage;
    const endIndex = startIndex + quizzesPerPage;
    const quizzesToShow = filteredQuizzes.slice(startIndex, endIndex);

    quizzesToShow.forEach(quiz => {
        const quizCard = createQuizCard(quiz);
        quizzesGrid.appendChild(quizCard);
    });

    setupPagination();
}

function createQuizCard(quiz) {
    const quizCard = document.createElement('div');
    quizCard.className = 'quiz-card';

    let imageContent = '';
    if (quiz.quizImage && quiz.quizImage.trim() !== '') {
        imageContent = `<img src="/trivia/uploads/${quiz.quizImage}" alt="${quiz.title}" class="quiz-image-photo" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
                       <div class="quiz-image-fallback" style="display: none;">
                           <i class="material-icons">${categoryIcons[quiz.category.toLowerCase()] || 'quiz'}</i>
                       </div>`;
    } else {
        imageContent = `<i class="material-icons">${categoryIcons[quiz.category.toLowerCase()] || 'quiz'}</i>`;
    }

    quizCard.innerHTML = `
        <div class="quiz-image">
            ${imageContent}
        </div>
        <div class="quiz-content">
            <span class="quiz-category">${quiz.category}</span>
            <h3 class="quiz-title">${quiz.title}</h3>
            <p class="quiz-description">${quiz.description}</p>
            <div class="quiz-creator">
                <i class="material-icons">person</i>
                <span>Kreator: ${quiz.creator.firstName} ${quiz.creator.lastName}</span>
            </div>
            <div class="quiz-meta">
                <span class="question-count">
                    <i class="material-icons">help_outline</i>
                    ${quiz.questionCount} ${quiz.questionCount === 1 ? 'pitanje' : 'pitanja'}
                </span>
                <span class="quiz-status ${quiz.isActive ? 'active' : 'inactive'}">
                    <i class="material-icons">${quiz.isActive ? 'visibility' : 'visibility_off'}</i>
                    ${quiz.isActive ? 'Aktivan' : 'Neaktivan'}
                </span>
            </div>
            <div class="quiz-actions">
                <button class="btn-details" onclick="viewQuizDetails(${quiz.id})">
                    <i class="material-icons">visibility</i>
                    Detalji kviza
                </button>
            </div>
        </div>
    `;

    if (!quiz.isActive) {
        quizCard.classList.add('disabled');
    }

    return quizCard;
}

function setupPagination() {
    const totalPages = Math.ceil(filteredQuizzes.length / quizzesPerPage);
    const paginationContainer = document.getElementById('paginationContainer');
    const pageInfo = document.getElementById('pageInfo');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');

    if (totalPages <= 1) {
        paginationContainer.style.display = 'none';
        return;
    }

    paginationContainer.style.display = 'flex';
    pageInfo.textContent = `Stranica ${currentPage} od ${totalPages}`;
    
    prevBtn.disabled = currentPage === 1;
    nextBtn.disabled = currentPage === totalPages;
}

function changePage(direction) {
    const totalPages = Math.ceil(filteredQuizzes.length / quizzesPerPage);
    
    if (direction === 1 && currentPage < totalPages) {
        currentPage++;
    } else if (direction === -1 && currentPage > 1) {
        currentPage--;
    }
    
    displayQuizzes();
    
    document.querySelector('.quizzes-section').scrollIntoView({ 
        behavior: 'smooth', 
        block: 'start' 
    });
}

function filterQuizzes() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const categoryFilter = document.getElementById('categoryFilter').value.toLowerCase();
    const statusFilter = document.getElementById('statusFilter').value;

    filteredQuizzes = allQuizzes.filter(quiz => {
        const matchesSearch = quiz.title.toLowerCase().includes(searchTerm) ||
                            quiz.description.toLowerCase().includes(searchTerm) ||
                            quiz.creator.firstName.toLowerCase().includes(searchTerm) ||
                            quiz.creator.lastName.toLowerCase().includes(searchTerm);
        
        const matchesCategory = !categoryFilter || quiz.category.toLowerCase() === categoryFilter;
        
        const matchesStatus = !statusFilter || 
                            (statusFilter === 'active' && quiz.isActive) ||
                            (statusFilter === 'inactive' && !quiz.isActive);

        return matchesSearch && matchesCategory && matchesStatus;
    });

    currentPage = 1; 
    updateQuizCount();
    displayQuizzes();
}

function viewQuizDetails(quizId) {
    event.preventDefault();
    console.log(`Prikazuju se detalji kviza sa ID: ${quizId}`);
    
    const quiz = allQuizzes.find(q => q.id === quizId);
    if (!quiz) {
        console.error('Kviz nije pronađen');
        return;
    }
    
    showQuizDetailsModal(quiz);
}

function showQuizDetailsModal(quiz) {
    
    let modalOverlay = document.getElementById('quiz-details-modal');
    if (!modalOverlay) {
        modalOverlay = document.createElement('div');
        modalOverlay.id = 'quiz-details-modal';
        modalOverlay.className = 'modal-overlay';
        document.body.appendChild(modalOverlay);
    }

    let imageContent = '';
    if (quiz.quizImage && quiz.quizImage.trim() !== '') {
        imageContent = `<img src="/trivia/uploads/${quiz.quizImage}" alt="${quiz.title}" style="width: 100%; height: 200px; object-fit: cover; border-radius: 15px;">`;
    } else {
        imageContent = `
            <div style="width: 100%; height: 200px; background: linear-gradient(135deg, var(--primary-color), var(--accent-color)); border-radius: 15px; display: flex; align-items: center; justify-content: center;">
                <i class="material-icons" style="font-size: 4rem; color: rgba(255, 255, 255, 0.8);">${categoryIcons[quiz.category.toLowerCase()] || 'quiz'}</i>
            </div>
        `;
    }

    modalOverlay.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>Detalji kviza</h3>
                <button class="modal-close" onclick="closeQuizDetailsModal()">
                    <i class="material-icons">close</i>
                </button>
            </div>
            <div class="modal-body">
                <div style="margin-bottom: 25px;">
                    ${imageContent}
                </div>
                
                <div style="margin-bottom: 20px;">
                    <span class="quiz-category" style="display: inline-block; background: rgba(240, 147, 251, 0.3); color: var(--text-light); padding: 6px 15px; border-radius: 15px; font-size: 0.9rem; margin-bottom: 15px;">
                        ${quiz.category}
                    </span>
                    <h2 style="color: var(--text-light); font-size: 1.8rem; font-weight: 500; margin-bottom: 15px;">
                        ${quiz.title}
                    </h2>
                    <p style="color: rgba(255, 255, 255, 0.8); font-size: 1rem; line-height: 1.6; margin-bottom: 25px;">
                        ${quiz.description}
                    </p>
                </div>

                <div class="stats-summary">
                    <div class="stat-card">
                        <div class="stat-value">${quiz.questionCount}</div>
                        <div class="stat-label">${quiz.questionCount === 1 ? 'Pitanje' : 'Pitanja'}</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-value" style="color: ${quiz.isActive ? 'var(--success-color)' : 'var(--error-color)'};">
                            ${quiz.isActive ? 'Aktivan' : 'Neaktivan'}
                        </div>
                        <div class="stat-label">Status</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-value">${quiz.creator.firstName} ${quiz.creator.lastName}</div>
                        <div class="stat-label">Kreator</div>
                    </div>
                </div>
            </div>
        </div>
    `;

    
    modalOverlay.classList.add('active');
    
    
    modalOverlay.onclick = function(e) {
        if (e.target === modalOverlay) {
            closeQuizDetailsModal();
        }
    };
}

function closeQuizDetailsModal() {
    const modalOverlay = document.getElementById('quiz-details-modal');
    if (modalOverlay) {
        modalOverlay.classList.remove('active');
    }
}

function showError(message) {
    alert(message);
}

document.addEventListener('DOMContentLoaded', () => {
    loadAllQuizzes();
    console.log("All quizzes page loaded");
});


window.viewQuizDetails = viewQuizDetails;
window.closeQuizDetailsModal = closeQuizDetailsModal;
window.changePage = changePage;
window.filterQuizzes = filterQuizzes;