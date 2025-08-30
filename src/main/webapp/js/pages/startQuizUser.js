let ws = null;
let quizPin = null;
let currentParticipantId = null;
let isJoined = false;
let quizId = null;
window.isIntentionalRedirect = false;

document.addEventListener('DOMContentLoaded', function () {
    const urlParams = new URLSearchParams(window.location.search);
    quizPin = urlParams.get('pin');
    console.log(quizPin);


    fetch(`/trivia/sessionToQuiz/${quizPin}`)
        .then(response => response.json())
        .then(data => {
            console.log('Odgovor sa servera:', data);
            if (data.success) {
                quizId = data.quiz.id;
                console.log('Quiz ID:', data.quiz.id);
                console.log('Participanti:', data.participants);


                populateQuizData(data.quiz);


                populateExistingParticipants(data.participants);
            } else {
                console.log('Greška:', data.message);

                alert('Kviz nije pronađen ili je sesija završena.');


                window.location.href = 'index.html';



            }
        })
        .catch(error => {
            console.error('Greška:', error);
            showError('Greška pri učitavanju kviza');
        });
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    const isSecure = protocol === "wss";

    const host = isSecure ? window.location.hostname : `${window.location.hostname}:8080`;
    ws = new WebSocket(`${protocol}://${host}/trivia/joinQuizSocket/${quizPin}`);

    ws.onopen = function () {
        console.log("povezan sa soketom.")
    }

    ws.onmessage = function (event) {
        try {
            console.log("=== WEBSOCKET MESSAGE ===");
            console.log("Raw message:", event.data);
            var data = JSON.parse(event.data);
            console.log("Parsed data:", data);

            if (data.type === 'JOIN') {
                console.log("Handling JOIN message");
                addParticipant(data.name, data.timestamp, data.participantId);


                if (data.name === document.getElementById('participantName').value.trim()) {
                    currentParticipantId = data.participantId;
                    isJoined = true;
                    updateJoinButton();
                }
            } else if (data.type === 'LEAVE') {
                console.log("Handling LEAVE message for participant:", data.participantId);
                removeParticipant(data.participantId);


                if (data.participantId === currentParticipantId) {
                    currentParticipantId = null;
                    isJoined = false;
                    updateJoinButton();
                }
            } else if (data.type === 'HOST_LEFT') {
                console.log("Host je napustio kviz!");
                handleHostLeft();
            } else if (data.type === 'QUIZ_STARTING') {
                window.isIntentionalRedirect = true;
                window.location.href = `quizPlayingUser.html?sessionPin=${quizPin}&participantId=${currentParticipantId}&quizId=${quizId}`;


            }
            console.log("=== END WEBSOCKET MESSAGE ===");
        } catch (e) {
            console.log("Greška pri parsiranju poruke:", e);
            console.log("Raw poruka:", event.data);
        }
    };

    ws.onerror = function (error) {
        console.error("WebSocket greška:", error);
    };

    ws.onclose = function (event) {
        console.log("WebSocket zatvorеn:", event.code, event.reason);
    };
});

function populateExistingParticipants(participants) {
    console.log('Popunjavam postojeće participante:', participants);

    const participantsList = document.getElementById('participantsList');
    const participantCount = document.getElementById('participantCount');

    if (!participantsList) return;


    participantsList.innerHTML = '';

    if (participants && participants.length > 0) {

        participants.forEach(participant => {
            addExistingParticipant(participant.participantName, participant.participantId);
        });


        if (participantCount) {
            participantCount.textContent = participants.length;
        }
    } else {

        participantsList.innerHTML = '<div class="empty-participants">Nema participanata</div>';
        if (participantCount) {
            participantCount.textContent = '0';
        }
    }
}


function addExistingParticipant(name, participantId) {
    const participantsList = document.getElementById('participantsList');

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

    console.log(`Dodao postojećeg participanta: ${name} (ID: ${participantId})`);
}

function removeParticipant(participantId) {
    console.log("=== REMOVE PARTICIPANT DEBUG ===");
    console.log("Trying to remove participant ID:", participantId);

    const participantsList = document.getElementById('participantsList');
    const participantCount = document.getElementById('participantCount');

    if (!participantsList) {
        console.log("ERROR: participantsList element not found!");
        return;
    }

    console.log("participantsList found, searching for element...");


    const allParticipants = participantsList.querySelectorAll('.participant-item');
    console.log("All participant elements:", allParticipants.length);

    allParticipants.forEach((element, index) => {
        const dataId = element.getAttribute('data-participant-id');
        console.log(`Element ${index}: data-participant-id = "${dataId}"`);
        console.log(`Comparing "${dataId}" === "${participantId}": ${dataId === participantId}`);
    });


    const participantElement = participantsList.querySelector(`[data-participant-id="${participantId}"]`);
    console.log("Found element to remove:", participantElement);

    if (participantElement) {
        participantElement.remove();
        console.log(`SUCCESS: Removed participant with ID: ${participantId}`);


        const currentCount = participantsList.querySelectorAll('.participant-item').length;
        console.log("New participant count:", currentCount);

        if (participantCount) {
            participantCount.textContent = currentCount;
        }


        if (currentCount === 0) {
            participantsList.innerHTML = '<div class="empty-participants">Nema participanata</div>';
            console.log("Added empty participants message");
        }
    } else {
        console.log("ERROR: Could not find element with data-participant-id:", participantId);
        console.log("Available participant IDs:");
        allParticipants.forEach(el => {
            console.log("- " + el.getAttribute('data-participant-id'));
        });
    }
    console.log("=== END REMOVE DEBUG ===");
}

function handleHostLeft() {

    if (ws) {
        ws.close();
    }


    alert('Host je napustio kviz. Ova sesija kviza više ne postoji.');


    window.location.href = 'index.html';
}


function updateJoinButton() {
    const joinButton = document.getElementById('joinButton');
    const participantNameInput = document.getElementById('participantName');

    if (!joinButton) return;

    if (isJoined) {

        joinButton.textContent = 'Napusti';
        joinButton.onclick = leaveQuiz;
        joinButton.disabled = false;
        if (participantNameInput) {
            participantNameInput.disabled = true;
        }
    } else {

        joinButton.textContent = 'Pridruži se';
        joinButton.onclick = joinQuiz;
        joinButton.disabled = false;
        if (participantNameInput) {
            participantNameInput.disabled = false;
        }
    }
}

window.addEventListener('beforeunload', function (event) {
    if (window.isIntentionalRedirect) {
        ws.close();
        return;
    }
    if (ws && ws.readyState === WebSocket.OPEN && isJoined) {

        const leaveMessage = {
            type: 'LEAVE',
            participantId: currentParticipantId,
            sessionId: quizPin
        };
        ws.send(JSON.stringify(leaveMessage));
        ws.close();
    }
});


window.addEventListener('pagehide', function (event) {
    if (window.isIntentionalRedirect) {
        ws.close();
        return;
    }
    if (ws && ws.readyState === WebSocket.OPEN && isJoined) {
        const leaveMessage = {
            type: 'LEAVE',
            participantId: currentParticipantId,
            sessionId: quizPin
        };
        ws.send(JSON.stringify(leaveMessage));
        ws.close();
    }
});

function populateQuizData(quiz) {

    const quizTitle = document.getElementById('quizTitle');
    if (quizTitle) {
        quizTitle.textContent = quiz.title || 'Bez naziva';
    }


    const quizDescription = document.getElementById('quizDescription');
    if (quizDescription) {
        quizDescription.textContent = quiz.description || 'Nema opisa kviza';
    }


    const quizCategory = document.getElementById('quizCategory');
    if (quizCategory) {
        quizCategory.textContent = quiz.category || 'Opšta kategorija';
    }


    const quizImage = document.getElementById('quizImage');
    if (quizImage) {
        if (quiz.quizImage && quiz.quizImage.trim() !== '') {
            console.log('Postoji slika kviza:', quiz.quizImage);
            quizImage.innerHTML = `<img src="uploads/${quiz.quizImage}" alt="Quiz slika" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`;
        } else {
            console.log('Nema slike kviza, koristim default ikonu');

        }
    }


    const questionCount = document.getElementById('questionCount');
    if (questionCount && quiz.questions) {
        const count = quiz.questions.length;
        questionCount.textContent = `${count} ${count === 1 ? 'pitanje' : count < 5 ? 'pitanja' : 'pitanja'}`;
    }


    const totalPoints = document.getElementById('totalPoints');
    if (totalPoints && quiz.questions) {
        const points = quiz.questions.reduce((sum, question) => sum + (question.points || 0), 0);
        totalPoints.textContent = `${points} ${points === 1 ? 'bod' : points < 5 ? 'boda' : 'bodova'}`;
    }


    const estimatedTime = document.getElementById('estimatedTime');
    if (estimatedTime && quiz.questions) {
        const totalSeconds = quiz.questions.reduce((sum, question) => sum + (question.timeLimit || 30), 0);
        const minutes = Math.ceil(totalSeconds / 60);
        estimatedTime.textContent = `~${minutes} min`;
    }


    document.title = `${quiz.title || 'Kviz'} - Trivia`;

    console.log('Podaci uspješno popunjeni na stranici');
}

function showError(message) {

    const quizTitle = document.getElementById('quizTitle');
    const quizDescription = document.getElementById('quizDescription');
    const quizCategory = document.getElementById('quizCategory');

    if (quizTitle) quizTitle.textContent = 'Greška pri učitavanju';
    if (quizDescription) quizDescription.textContent = message;
    if (quizCategory) quizCategory.textContent = 'Greška';


    alert('Greška: ' + message);
}

function joinQuiz() {
    const participantName = document.getElementById('participantName').value.trim();

    if (!participantName) {
        alert('Molimo unesite vaše ime');
        return;
    }

    if (ws && ws.readyState === WebSocket.OPEN) {

        const message = {
            type: 'JOIN',
            name: participantName,
            sessionId: quizPin,
            timestamp: new Date().toISOString()
        };


        ws.send(JSON.stringify(message));
        document.getElementById('joinButton').disabled = true;
        console.log('Poslana JSON poruka:', message);
    } else {
        console.error('WebSocket nije spreman');
        alert('Greška: Konekcija nije uspostavljena');
    }
}

function leaveQuiz() {
    if (!isJoined || !currentParticipantId) {
        console.log('Korisnik nije pridružen kvizu');
        return;
    }

    if (ws && ws.readyState === WebSocket.OPEN) {

        const leaveMessage = {
            type: 'LEAVE',
            participantId: currentParticipantId,
            sessionId: quizPin
        };

        ws.send(JSON.stringify(leaveMessage));
        console.log('Poslana LEAVE poruka:', leaveMessage);


        const joinButton = document.getElementById('joinButton');
        if (joinButton) {
            joinButton.disabled = true;
        }
    } else {
        console.error('WebSocket nije spreman');
        alert('Greška: Konekcija nije uspostavljena');
    }


    setTimeout(() => {
        window.location.href = 'index.html';
    }, 500);
}


function addParticipant(name, timestamp, participantId) {
    const participantsList = document.getElementById('participantsList');
    const participantCount = document.getElementById('participantCount');

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