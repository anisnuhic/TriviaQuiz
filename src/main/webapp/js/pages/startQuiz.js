
import { Quiz } from '../models/Quiz.js';

let currentQuizId = null;
let participants = [];
let quizPin = null;
let ws = null;
window.isIntentionalRedirect = false;



let currentQuiz = new Quiz();

window.copyQuizPin = copyQuizPin;
window.startQuiz = startQuiz;
window.removeParticipant = removeParticipant;

const categories = {
    'matematika': { name: 'Matematika', icon: 'functions' },
    'hemija': { name: 'Hemija', icon: 'science' },
    'biologija': { name: 'Biologija', icon: 'biotech' },
    'programiranje': { name: 'Programiranje', icon: 'code' },
    'historija': { name: 'Historija', icon: 'history_edu' },
    'geografija': { name: 'Geografija', icon: 'public' },
    'opsta-kultura': { name: 'Opšta kultura', icon: 'school' }
};


window.addEventListener('beforeunload', function () {
    if (window.isIntentionalRedirect) {
        return; // Ne radi nista ako je namjerni redirect
    }
    console.log("Host napušta stranicu - cleanup...");


    if (ws && ws.readyState === WebSocket.OPEN) {
        console.log("Šaljem HOST_LEFT poruku");
        const hostLeftMessage = {
            type: 'HOST_LEFT',
            sessionId: quizPin,
            message: 'Host je napustio kviz'
        };
        ws.send(JSON.stringify(hostLeftMessage));

        setTimeout(() => {
            ws.close();
        }, 100);
    }


    if (currentQuiz) {
        console.log("Čistim currentQuiz iz sessionStorage");
        Quiz.clear();
        currentQuiz = null;
    }
});

window.addEventListener('pagehide', function () {
    if (window.isIntentionalRedirect) {
        return; // Ne radi nista ako je namjerni redirect
    }
    console.log("Host napušta (pagehide) - cleanup...");

    if (ws && ws.readyState === WebSocket.OPEN) {
        const hostLeftMessage = {
            type: 'HOST_LEFT',
            sessionId: quizPin,
            message: 'Host je napustio kviz'
        };
        ws.send(JSON.stringify(hostLeftMessage));
        ws.close();
    }


    if (currentQuiz) {
        Quiz.clear();
        currentQuiz = null;
    }
});

document.addEventListener('DOMContentLoaded', async function () {
    await initializePage();

    if (quizPin) {
         const protocol = window.location.protocol === "https:" ? "wss" : "ws";
         const isSecure = protocol === "wss";

const host = isSecure ? window.location.hostname : `${window.location.hostname}:8080`;
        ws = new WebSocket(`${protocol}://${host}/trivia/joinQuizSocket/${quizPin}`);

        ws.onopen = function () {
            console.log("povezan sa soketom.")
        }

        ws.onerror = function (error) {
            console.error("WebSocket greška:", error);
        };

        ws.onclose = function (event) {
            console.log("WebSocket zatvorеn:", event.code, event.reason);
        };

        ws.onmessage = function (event) {
            try {
                console.log("=== WEBSOCKET MESSAGE ===");
                console.log("Raw message:", event.data);
                var data = JSON.parse(event.data);
                console.log("Parsed data:", data);

                if (data.type === 'JOIN') {
                    console.log("Handling JOIN message");
                    addParticipant(data.name, data.timestamp, data.participantId);

                } else if (data.type === 'LEAVE') {
                    console.log("Handling LEAVE message for participant:", data.participantId);
                    removeParticipant(data.participantId);

                } else if (data.type === 'HOST_LEFT') {
                    console.log("Host je napustio kviz!");
                }
                console.log("=== END WEBSOCKET MESSAGE ===");
            } catch (e) {
                console.log("Greška pri parsiranju poruke:", e);
                console.log("Raw poruka:", event.data);
            }
        }
    }
});


function addParticipant(name, timestamp, participantId) {
    const participantsList = document.getElementById('participantsList');
    const participantCount = document.getElementById('participantCount');

    participants.push({ id: participantId, name: name, timestamp: timestamp });

    if (!participantsList) return;

    const emptyDiv = participantsList.querySelector('.empty-participants');
    if (emptyDiv) {
        emptyDiv.remove();
    }

    const participantDiv = document.createElement('div');
    participantDiv.className = 'participant-item';
    participantDiv.setAttribute('data-participant-id', participantId);
    participantDiv.innerHTML = `
        <div class="participant-avatar">
            ${name.charAt(0).toUpperCase()}
        </div>
        <div class="participant-info">
            <span class="participant-name">${name}</span>
        </div>
    `;

    participantsList.appendChild(participantDiv);

    const currentCount = participantsList.querySelectorAll('.participant-item').length;
    if (participantCount) {
        participantCount.textContent = currentCount;
    }

    console.log(`Dodao participanta: ${name} (ID: ${participantId})`);
}

async function initializePage() {
    try {
        showLoading(true);

        const urlParams = new URLSearchParams(window.location.search);
        currentQuizId = urlParams.get('quizId') || urlParams.get('id');
        quizPin = urlParams.get('sessionPin');

        if (!currentQuizId || !quizPin) {
            console.error('Nedostaju podaci u URL-u');
            alert('Greška: Nedostaju podaci o sesiji');
            window.history.back();
            return;
        }

        await loadQuizData();
        initializeUI();
        showLoading(false);

    } catch (error) {
        console.error('Greška pri inicijalizaciji:', error);
        showLoading(false);
    }
}

async function loadQuizData() {
    try {
        const response = await fetch(`/trivia/admin/api/quiz/${currentUser.id}/${currentQuizId}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!response.ok) {
            if (response.status === 404) {
                throw new Error('Kviz nije pronađen');
            } else if (response.status === 403) {
                throw new Error('Nemate dozvolu za pristup ovom kvizu');
            } else {
                throw new Error('Greška pri učitavanju kviza');
            }
        }

        const quizData = await response.json();
        currentQuiz = Quiz.fromJson(quizData);
        currentQuiz.save();

        displayQuizInfo();

    } catch (error) {
        console.error('Greška pri učitavanju kviza:', error);
        throw error;
    }
}

function displayQuizInfo() {
    if (!currentQuiz.isValid()) return;

    document.getElementById('quizTitle').textContent = currentQuiz.getTitle();
    document.getElementById('quizDescription').textContent = currentQuiz.getDescription();

    const categoryInfo = categories[currentQuiz.getCategory()] || {
        name: currentQuiz.getCategory(),
        icon: 'quiz'
    };
    const categoryElement = document.getElementById('quizCategory');
    categoryElement.innerHTML = `
        <i class="material-icons">${categoryInfo.icon}</i>
        ${categoryInfo.name}
    `;

    const quizImageElement = document.getElementById('quizImage');
    const imageUrl = currentQuiz.getImageUrl();
    if (imageUrl) {
        quizImageElement.innerHTML = `<img src="${imageUrl}" alt="Quiz image">`;
        quizImageElement.classList.add('has-image');
    } else {
        quizImageElement.innerHTML = `<i class="material-icons">quiz</i>`;
        quizImageElement.classList.remove('has-image');
    }

    displayQuizStats();
}

function displayQuizStats() {
    if (!currentQuiz.isValid()) return;

    document.getElementById('questionCount').textContent = currentQuiz.getFormattedQuestionCount();
    document.getElementById('estimatedTime').textContent = currentQuiz.getFormattedDuration();
    document.getElementById('totalPoints').textContent = currentQuiz.getFormattedTotalPoints();
}

function initializeUI() {
    const joinForm = document.getElementById('joinForm');
    const hostControls = document.getElementById('hostControls');
    const startButton = document.getElementById('startQuizBtn');
    const pinSection = document.getElementById('pinSection');

    if (joinForm) joinForm.style.display = 'none';
    if (hostControls) hostControls.style.display = 'block';
    if (startButton) startButton.style.display = 'flex';
    if (pinSection) pinSection.style.display = 'block';

    document.getElementById('quizPin').textContent = quizPin;
    updateParticipantsList();
}

function updateParticipantsList() {
    const participantsList = document.getElementById('participantsList');
    const participantCount = document.getElementById('participantCount');

    if (participantCount) {
        participantCount.textContent = participants.length;
    }

    if (participantsList) {
        if (participants.length === 0) {
            participantsList.innerHTML = `
                <div class="empty-participants">
                    <i class="material-icons">people_outline</i>
                    <p>Nema prijavljenih učesnika</p>
                    <small>Učesnici će se pojaviti ovde kada se prijave</small>
                </div>
            `;
        } else {
            participantsList.innerHTML = participants.map(participant => `
                <div class="participant-item">
                    <div class="participant-avatar">
                        ${participant.name.charAt(0).toUpperCase()}
                    </div>
                    <div class="participant-info">
                        <span class="participant-name">${participant.name}</span>
                        <small class="participant-status">${participant.status || 'Spreman'}</small>
                    </div>
                    <button class="participant-remove" onclick="removeParticipant('${participant.id}')">
                        <i class="material-icons">close</i>
                    </button>
                </div>
            `).join('');
        }
    }
}

function removeParticipant(participantId) {
    const participantsList = document.getElementById('participantsList');
    const participantCount = document.getElementById('participantCount');

    participants = participants.filter(p => p.id !== participantId);

    if (!participantsList) return;

    const participantElement = participantsList.querySelector(`[data-participant-id="${participantId}"]`);

    if (participantElement) {
        participantElement.remove();

        const currentCount = participantsList.querySelectorAll('.participant-item').length;
        if (participantCount) {
            participantCount.textContent = currentCount;
        }

        if (currentCount === 0) {
            participantsList.innerHTML = `
                <div class="empty-participants">
                    <i class="material-icons">people_outline</i>
                    <p>Nema prijavljenih učesnika</p>
                    <small>Učesnici će se pojaviti ovde kada se prijave</small>
                </div>
            `;
        }
    }
}

function startQuiz() {
    if (!currentQuiz.canStart()) {
        alert('Ovaj kviz nema pitanja i ne može se pokrenuti');
        return;
    }

    if (participants.length === 0) {
        const confirmed = confirm('Nema prijavljenih učesnika. Da li ipak želite da pokrenete kviz?');
        if (!confirmed) return;
    }

    console.log('Pokretanje kviza:', currentQuiz.getId());

    if (ws && ws.readyState === WebSocket.OPEN) {
        console.log("Šaljem QUIZ_STARTING poruku");
        const quizStartingMessage = {
            type: 'QUIZ_STARTING',
            sessionId: quizPin,
            message: 'Host je startao kviz'
        };
        ws.send(JSON.stringify(quizStartingMessage));

    }

    window.isIntentionalRedirect = true;
    ws.close();
    window.location.href = `quizPlaying.html?sessionPin=${quizPin}&participantId=host&quizId=${currentQuiz.getId()}`;
}

function copyQuizPin() {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(quizPin).then(() => {
            showCopyFeedback();
        }).catch(() => {
            fallbackCopyTextToClipboard(quizPin);
        });
    } else {
        fallbackCopyTextToClipboard(quizPin);
    }
}

function fallbackCopyTextToClipboard(text) {
    const textArea = document.createElement("textarea");
    textArea.value = text;
    textArea.style.position = "fixed";
    textArea.style.top = "0";
    textArea.style.left = "0";

    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
        document.execCommand('copy');
        showCopyFeedback();
    } catch (err) {
        console.log('Greška pri kopiranju PIN-a');
    }

    document.body.removeChild(textArea);
}

function showCopyFeedback() {
    const button = document.getElementById('copyPinBtn');
    if (button) {
        const originalText = button.innerHTML;
        button.innerHTML = '<i class="material-icons">check</i> Kopirano!';
        button.classList.add('copied');

        setTimeout(() => {
            button.innerHTML = originalText;
            button.classList.remove('copied');
        }, 2000);
    }
}

function showLoading(show) {
    const loadingOverlay = document.getElementById('loadingOverlay');
    if (loadingOverlay) {
        loadingOverlay.style.display = show ? 'flex' : 'none';
    }
}

