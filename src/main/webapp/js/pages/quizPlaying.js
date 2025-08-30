
class QuizPlayingHost {
    constructor() {
        this.ws = null;
        this.config = {
            sessionPin: null,
            participantId: null,
            quizId: null
        };
        
        this.state = {
            countdownStarted: false,
            quizCompleted: false,
            canProceedToNext: false,
            showCorrectAnswer: false,
            timeRemaining: 0,
            currentQuestion: null,
            participants: [],
            participantScores: {}
        };
        
        this.timers = {
            countdown: null,
            question: null
        };
        
        this.elements = {};
        this.messageHandlers = this.createMessageHandlers();
        
        this.init();
    }

    init() {
        document.addEventListener('DOMContentLoaded', () => this.onDOMLoaded());
        window.addEventListener('beforeunload', () => this.cleanup());
    }

    onDOMLoaded() {
        this.showLoading(true);
        document.body.classList.add('quiz-playing');
        
        if (!this.parseURLParams()) {
            console.error('Missing URL parameters');
            return;
        }
        
        this.loadParticipants();
        this.connectWebSocket();
    }

    parseURLParams() {
        const params = new URLSearchParams(window.location.search);
        this.config.sessionPin = params.get('sessionPin');
        this.config.participantId = params.get('participantId');
        this.config.quizId = params.get('quizId');
        
        return Object.values(this.config).every(param => param !== null);
    }

    connectWebSocket() {
        const protocol = window.location.protocol === "https:" ? "wss" : "ws";
        const host = protocol === "wss" ? 
            window.location.hostname : 
            `${window.location.hostname}:8080`;
            
        const { sessionPin, participantId, quizId } = this.config;
        this.ws = new WebSocket(`${protocol}://${host}/trivia/playingQuiz/${sessionPin}/${participantId}/${quizId}`);
        
        this.ws.onopen = () => this.onWebSocketOpen();
        this.ws.onmessage = (event) => this.onWebSocketMessage(event);
        this.ws.onerror = (error) => console.error("WebSocket error:", error);
        this.ws.onclose = (event) => console.log("WebSocket closed:", event.code, event.reason);
    }

    onWebSocketOpen() {
        console.log("Connected to WebSocket");
        setTimeout(() => this.startInitialSequence(), 4000);
    }

    onWebSocketMessage(event) {
        try {
            const data = JSON.parse(event.data);
            const handler = this.messageHandlers[data.type];
            
            if (handler) {
                handler(data);
            } else {
                console.log('Unhandled message type:', data.type);
            }
        } catch (error) {
            console.error('Error parsing message:', error);
        }
    }

    createMessageHandlers() {
        return {
            'JOIN': () => console.log('New participant joined'),
            'PARTICIPANT_LEFT': (data) => this.handleParticipantLeft(data),
            'START_TIMER': () => console.log('Timer started'),
            'FIRST_QUESTION': (data) => this.handleQuestion(data),
            'NEXT_QUESTION': (data) => this.handleQuestion(data),
            'PARTICIPANT_ANSWERED': (data) => this.handleParticipantAnswered(data),
            'QUIZ_COMPLETED': (data) => this.handleQuizCompleted(data),
            'SCORE_UPDATE': (data) => this.handleScoreUpdate(data),
            'ERROR': (data) => this.handleError(data.message)
        };
    }

    async loadParticipants() {
        try {
            const response = await fetch(`/trivia/sessionToQuiz/${this.config.sessionPin}`);
            const data = await response.json();
            
            if (data.success) {
                this.state.participants = data.participants || [];
                console.log('Loaded participants:', this.state.participants);
            }
        } catch (error) {
            console.error('Error loading participants:', error);
        }
    }

    startInitialSequence() {
        if (this.ws?.readyState === WebSocket.OPEN) {
            this.sendMessage('START_TIMER', { sessionId: this.config.sessionPin });
            
            if (!this.state.countdownStarted) {
                this.startCountdown();
            }
        }
    }

    startCountdown() {
        if (this.state.countdownStarted) return;
        
        this.state.countdownStarted = true;
        this.showLoading(false);
        
        const overlay = document.getElementById('timerOverlay');
        const number = document.getElementById('countdownNumber');
        const welcome = document.getElementById('welcomeMessage');
        
        let count = 5;
        overlay?.classList.add('active');
        welcome?.classList.remove('show');
        
        this.timers.countdown = setInterval(() => {
            if (number) {
                number.textContent = count;
                number.style.animation = 'none';
                setTimeout(() => number.style.animation = 'countdownPulse 1s ease-in-out', 10);
            }
            
            if (--count < 0) {
                this.endCountdown();
            }
        }, 1000);
    }

    endCountdown() {
        this.clearTimer('countdown');
        
        const overlay = document.getElementById('timerOverlay');
        const welcome = document.getElementById('welcomeMessage');
        
        overlay?.classList.remove('active');
        setTimeout(() => welcome?.classList.add('show'), 500);
        
        this.sendMessage('START_QUESTIONS', {
            sessionId: this.config.sessionPin,
            quizId: this.config.quizId
        });
    }

    handleQuestion(questionData) {
        this.clearTimer('question');
        
        this.state.currentQuestion = questionData;
        this.state.timeRemaining = questionData.timeLimit;
        this.state.canProceedToNext = false;
        this.state.showCorrectAnswer = false;
        
        this.resetParticipantStatus();
        this.displayQuestion(questionData);
        
        setTimeout(() => this.startQuestionTimer(questionData.timeLimit), 100);
    }

    resetParticipantStatus() {
        this.state.participants.forEach(participant => {
            participant.hasAnswered = false;
            participant.isCorrect = null;
        });
        this.updateParticipantsList();
    }

    displayQuestion(questionData) {
        const mainContent = document.querySelector('.quiz-playing-content');
        mainContent.innerHTML = `
            <div class="host-quiz-layout">
                <div class="host-question-section">
                    ${this.createQuestionHTML(questionData)}
                </div>
                <div class="host-participants-section">
                    ${this.createParticipantsHTML()}
                </div>
            </div>
        `;
        this.updateNextQuestionButton();
    }

    createQuestionHTML(questionData) {
        const { questionOrder, totalQuestions, questionText, timeLimit, points, questionType, answers } = questionData;
        const correctAnswer = answers.find(answer => answer.isCorrect);
        
        return `
            <div class="quiz-question-container">
                <div class="question-header">
                    <div class="question-number-badge">${questionOrder}</div>
                    <h2 class="question-text">${questionText}</h2>
                    <div class="question-meta">
                        <div class="meta-item">
                            <i class="fas fa-star"></i>
                            <span>${points} bodova</span>
                        </div>
                        <div class="meta-item">
                            <i class="fas fa-clock"></i>
                            <span>${timeLimit} sekundi</span>
                        </div>
                        <div class="meta-item">
                            <i class="fas fa-users"></i>
                            <span id="answeredCount">0</span>/<span id="totalParticipants">${this.state.participants.length}</span> odgovorilo
                        </div>
                    </div>
                </div>
                
                <div class="question-timer" id="questionTimer">
                    <i class="fas fa-hourglass-half timer-icon"></i>
                    <span class="timer-text" id="timerText">${timeLimit}</span>
                    <div class="timer-progress" id="timerProgress"></div>
                </div>
                
                <div class="question-card-user">
                    ${this.createAnswersHTML(questionData)}
                    
                    <div class="host-info" style="display: none;" id="hostInfo">
                        <p><i class="fas fa-crown"></i> HOST PRIKAZ - Tačan odgovor je označen</p>
                    </div>
                    
                    <div class="host-waiting-info" id="hostWaitingInfo">
                        <p><i class="fas fa-eye-slash"></i> Tačan odgovor će biti prikazan kada vrijeme istekne ili svi odgovore</p>
                    </div>
                </div>

                <div class="host-controls">
                    <button id="nextQuestionBtn" class="btn-next-question" onclick="quizHost.proceedToNextQuestion()" disabled>
                        <i class="fas fa-arrow-right"></i>
                        <span class="btn-text">Sljedeće pitanje</span>
                        <span class="btn-status" id="nextBtnStatus">Čekaj da vrijeme istekne ili da svi odgovore</span>
                    </button>
                </div>
            </div>
        `;
    }

    createAnswersHTML(questionData) {
        const { questionType, answers } = questionData;
        const correctAnswer = answers.find(answer => answer.isCorrect);
        
        if (questionType === 'MULTIPLE_CHOICE' || questionType === 'TRUE_FALSE') {
            const sortedAnswers = [...answers].sort((a, b) => a.answerOrder - b.answerOrder);
            
            return `
                <div class="answer-options-user">
                    ${sortedAnswers.map((answer, index) => `
                        <div class="answer-option-user ${this.state.showCorrectAnswer && answer.isCorrect ? 'correct-answer' : ''}" 
                             data-answer-id="${answer.id}">
                            <div class="option-letter">${String.fromCharCode(65 + index)}</div>
                            <div class="option-text">${answer.answerText}</div>
                            ${this.state.showCorrectAnswer && answer.isCorrect ? '<i class="fas fa-check-circle correct-indicator"></i>' : ''}
                        </div>
                    `).join('')}
                </div>
            `;
        }
        
        if (questionType === 'TEXT') {
            return `
                <div class="text-answer-container">
                    <div class="host-correct-answer" style="display: ${this.state.showCorrectAnswer ? 'block' : 'none'};">
                        <strong>Tačan odgovor:</strong> ${correctAnswer.answerText}
                    </div>
                </div>
            `;
        }
        
        return '';
    }

    createParticipantsHTML() {
        const sortedParticipants = [...this.state.participants]
            .sort((a, b) => (b.score || 0) - (a.score || 0));
        
        return `
            <div class="participants-card">
                <div class="participants-header">
                    <h3><i class="fas fa-trophy"></i> Rezultati</h3>
                    <div class="participant-count" id="participantCount">${this.state.participants.length}</div>
                </div>
                <div class="participants-list" id="participantsList">
                    ${sortedParticipants.map((participant, index) => this.createParticipantHTML(participant, index + 1)).join('')}
                </div>
            </div>
        `;
    }

    createParticipantHTML(participant, rank) {
        const score = participant.score || 0;
        const rankClass = rank === 1 ? 'rank-gold' : rank === 2 ? 'rank-silver' : rank === 3 ? 'rank-bronze' : '';
        
        return `
            <div class="participant-item ${participant.hasAnswered ? 'answered' : 'waiting'} ${rankClass}" 
                 data-participant-id="${participant.participantId}">
                <div class="participant-rank">
                    <span class="rank-number">${rank}</span>
                    ${rank === 1 ? '<i class="fas fa-crown rank-icon"></i>' : ''}
                </div>
                <div class="participant-avatar">
                    ${participant.participantName.charAt(0).toUpperCase()}
                </div>
                <div class="participant-info">
                    <div class="participant-name">${participant.participantName}</div>
                    <div class="participant-score">
                        <i class="fas fa-star"></i> ${score} bodova
                    </div>
                    <div class="participant-status">
                        <span class="status-waiting" style="display: ${participant.hasAnswered ? 'none' : 'block'};">Razmišlja...</span>
                        <span class="status-answered correct" style="display: ${participant.hasAnswered && participant.isCorrect ? 'block' : 'none'};">
                            <i class="fas fa-check"></i> Tačno
                        </span>
                        <span class="status-answered incorrect" style="display: ${participant.hasAnswered && !participant.isCorrect ? 'block' : 'none'};">
                            <i class="fas fa-times"></i> Netačno
                        </span>
                    </div>
                </div>
                <div class="participant-indicator">
                    <i class="fas fa-clock waiting-icon" style="display: ${participant.hasAnswered ? 'none' : 'block'};"></i>
                    <i class="fas fa-check correct-icon" style="display: ${participant.hasAnswered && participant.isCorrect ? 'block' : 'none'};"></i>
                    <i class="fas fa-times incorrect-icon" style="display: ${participant.hasAnswered && !participant.isCorrect ? 'block' : 'none'};"></i>
                </div>
            </div>
        `;
    }

    startQuestionTimer(duration) {
        this.clearTimer('question');
        this.state.timeRemaining = duration;
        
        const elements = {
            text: document.getElementById('timerText'),
            progress: document.getElementById('timerProgress'),
            timer: document.getElementById('questionTimer')
        };
        
        if (!Object.values(elements).every(el => el)) {
            console.error('Timer elements not found');
            return;
        }
        
        this.updateTimerDisplay(elements, duration, duration);
        
        this.timers.question = setInterval(() => {
            this.state.timeRemaining--;
            this.updateTimerDisplay(elements, this.state.timeRemaining, duration);
            
            if (this.state.timeRemaining <= 0) {
                this.onTimerExpired();
            }
        }, 1000);
    }

    updateTimerDisplay(elements, current, total) {
        elements.text.textContent = current;
        elements.progress.style.width = `${Math.max(0, (current / total) * 100)}%`;
        
        elements.timer.className = elements.timer.className.replace(/\b(warning|danger)\b/g, '');
        if (current <= 5) elements.timer.classList.add('danger');
        else if (current <= 10) elements.timer.classList.add('warning');
    }

    onTimerExpired() {
        this.clearTimer('question');
        this.showCorrectAnswerToHost();
        this.state.canProceedToNext = true;
        this.updateNextQuestionButton();
    }

    showCorrectAnswerToHost() {
        this.state.showCorrectAnswer = true;
        
        if (this.state.currentQuestion) {
            const { questionType, answers } = this.state.currentQuestion;
            
            if (questionType === 'MULTIPLE_CHOICE' || questionType === 'TRUE_FALSE') {
                answers.forEach(answer => {
                    const element = document.querySelector(`[data-answer-id="${answer.id}"]`);
                    if (element && answer.isCorrect) {
                        element.classList.add('correct-answer');
                        element.innerHTML += '<i class="fas fa-check-circle correct-indicator"></i>';
                    }
                });
            } else if (questionType === 'TEXT') {
                const correctAnswerDiv = document.querySelector('.host-correct-answer');
                if (correctAnswerDiv) correctAnswerDiv.style.display = 'block';
            }
        }
        
        this.toggleElements([
            { selector: '#hostWaitingInfo', display: 'none' },
            { selector: '#hostInfo', display: 'block' }
        ]);
    }

    handleParticipantLeft(data) {
        this.state.participants = this.state.participants.filter(p => p.participantId !== data.participantId);
        this.updateParticipantsList();
        this.updateCounts();
        this.checkIfCanProceed();
    }

    handleParticipantAnswered(data) {
        const participant = this.state.participants.find(p => p.participantId === data.participantId);
        if (participant) {
            participant.hasAnswered = true;
            participant.isCorrect = data.isCorrect;
            this.updateParticipantInList(data.participantId, data.isCorrect);
            this.updateAnsweredCount();
            this.checkIfCanProceed();
        }
    }

    handleScoreUpdate(data) {
        this.state.participantScores = data.scores || {};
        this.state.participants.forEach(participant => {
            participant.score = this.state.participantScores[participant.participantId] || 0;
        });
        this.state.participants.sort((a, b) => (b.score || 0) - (a.score || 0));
        this.updateParticipantsList();
    }

    handleQuizCompleted(data) {
        this.state.quizCompleted = true;
        this.clearAllTimers();
        
        document.querySelector('.quiz-playing-content').innerHTML = `
            <div class="quiz-completed-container">
                <div class="completion-icon"><i class="fas fa-trophy"></i></div>
                <h2 class="completion-title">Kviz Završen!</h2>
                <p class="completion-message">${data.message}</p>
                <div class="completion-actions">
                    <button class="btn-primary" onclick="window.location.href='/trivia/admin/dashboard.html'">
                        <i class="fas fa-home"></i> Povratak na početnu
                    </button>
                    <button class="btn-primary" onclick="window.location.href='/trivia/scores.html?sessionPin=${this.config.sessionPin}&isHost=true'">
                        <i class="fas fa-home"></i> Prikaz rezultata
                    </button>
                </div>
            </div>
        `;
    }

    handleError(message) {
        document.querySelector('.quiz-playing-content').innerHTML = `
            <div class="error-container">
                <div class="error-icon"><i class="fas fa-exclamation-triangle"></i></div>
                <h2 class="error-title">Greška</h2>
                <p class="error-message">${message}</p>
                <div class="error-actions">
                    <button class="btn-primary" onclick="window.location.href='/trivia'">
                        <i class="fas fa-home"></i> Povratak na početnu
                    </button>
                </div>
            </div>
        `;
    }

    checkIfCanProceed() {
        const allAnswered = this.state.participants.every(p => p.hasAnswered);
        
        if (allAnswered && !this.state.showCorrectAnswer) {
            this.clearTimer('question');
            this.showCorrectAnswerToHost();
            this.state.canProceedToNext = true;
            this.updateNextQuestionButton();
        } else if (allAnswered || this.state.timeRemaining <= 0) {
            this.state.canProceedToNext = true;
            this.updateNextQuestionButton();
        }
    }

    proceedToNextQuestion() {
        if (!this.state.canProceedToNext) return;
        
        this.clearTimer('question');
        this.sendMessage('HOST_NEXT_QUESTION', {
            sessionId: this.config.sessionPin,
            quizId: this.config.quizId
        });
        
        this.state.canProceedToNext = false;
        this.updateNextQuestionButton();
    }

    updateNextQuestionButton() {
        const btn = document.getElementById('nextQuestionBtn');
        const status = document.getElementById('nextBtnStatus');
        if (!btn || !status) return;
        
        const allAnswered = this.state.participants.every(p => p.hasAnswered);
        const answeredCount = this.state.participants.filter(p => p.hasAnswered).length;
        const totalCount = this.state.participants.length;
        
        if (this.state.canProceedToNext || allAnswered || this.state.timeRemaining <= 0) {
            btn.disabled = false;
            btn.classList.add('enabled');
            status.textContent = 'Klikni za sljedeće pitanje';
        } else {
            btn.disabled = true;
            btn.classList.remove('enabled');
            status.textContent = answeredCount === totalCount ? 
                'Svi su odgovorili - možete ići dalje' : 
                `Čeka se ${totalCount - answeredCount} odgovora ili da vrijeme istekne`;
        }
    }

    updateParticipantInList(participantId, isCorrect) {
        const item = document.querySelector(`[data-participant-id="${participantId}"]`);
        if (!item) return;
        
        item.classList.remove('waiting');
        item.classList.add('answered', isCorrect ? 'correct-answer' : 'wrong-answer');
        
        this.toggleElements([
            { element: item.querySelector('.status-waiting'), display: 'none' },
            { element: item.querySelector('.waiting-icon'), display: 'none' },
            { element: item.querySelector(`.status-answered.${isCorrect ? 'correct' : 'incorrect'}`), display: 'block' },
            { element: item.querySelector(`.${isCorrect ? 'correct' : 'incorrect'}-icon`), display: 'block' }
        ]);
    }

    updateParticipantsList() {
        const list = document.getElementById('participantsList');
        if (!list) return;
        
        const sorted = [...this.state.participants].sort((a, b) => (b.score || 0) - (a.score || 0));
        list.innerHTML = sorted.map((participant, index) => this.createParticipantHTML(participant, index + 1)).join('');
    }

    updateAnsweredCount() {
        const count = this.state.participants.filter(p => p.hasAnswered).length;
        const element = document.getElementById('answeredCount');
        if (element) element.textContent = count;
    }

    updateCounts() {
        const participantCount = document.getElementById('participantCount');
        const totalParticipants = document.getElementById('totalParticipants');
        
        if (participantCount) participantCount.textContent = this.state.participants.length;
        if (totalParticipants) totalParticipants.textContent = this.state.participants.length;
    }

    toggleElements(configs) {
        configs.forEach(({ selector, element, display }) => {
            const el = element || document.querySelector(selector);
            if (el) el.style.display = display;
        });
    }

    sendMessage(type, data = {}) {
        if (this.ws?.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({ type, ...data }));
        }
    }

    clearTimer(timerName) {
        if (this.timers[timerName]) {
            clearInterval(this.timers[timerName]);
            this.timers[timerName] = null;
        }
    }

    clearAllTimers() {
        Object.keys(this.timers).forEach(timer => this.clearTimer(timer));
    }

    showLoading(show) {
        const loading = document.getElementById('loadingOverlay');
        if (loading) loading.style.display = show ? 'flex' : 'none';
    }

    cleanup() {
        console.log('Cleaning up...');
        this.clearAllTimers();
        
        if (this.ws && !this.state.quizCompleted) {
            if (this.ws.readyState === WebSocket.OPEN) {
                this.sendMessage('HOST_LEFT');
            }
            this.ws.close();
        } else if (this.ws) {
            this.ws.onclose = null;
            this.ws.close();
        }
    }
}

window.quizHost = new QuizPlayingHost();