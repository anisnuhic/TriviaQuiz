
class QuizPlayingUser {
    constructor() {
        this.ws = null;
        this.config = {
            sessionPin: null,
            participantId: null,
            quizId: null
        };
        
        this.state = {
            currentQuestion: null,
            selectedAnswer: null,
            timeRemaining: 0,
            quizCompleted: false,
            answerSubmitted: false
        };
        
        this.timers = {
            countdown: null,
            question: null
        };
        
        this.messageHandlers = this.createMessageHandlers();
        this.init();
    }

    init() {
        document.addEventListener('DOMContentLoaded', () => this.onDOMLoaded());
        window.addEventListener('beforeunload', () => this.cleanup());
    }

    onDOMLoaded() {
        this.showLoading(true);
        console.clear();
        console.log('Loading quiz user player');
        
        document.body.classList.add('quiz-playing');
        
        if (!this.parseURLParams()) {
            console.error('Missing URL parameters');
            return;
        }
        
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
        
        this.ws.onopen = () => console.log("Connected to WebSocket");
        this.ws.onmessage = (event) => this.onWebSocketMessage(event);
        this.ws.onerror = (error) => console.error("WebSocket error:", error);
        this.ws.onclose = (event) => console.log("WebSocket closed:", event.code, event.reason);
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
            'START_TIMER': () => this.startCountdown(),
            'START_QUESTIONS': () => console.log('Questions starting...'),
            'FIRST_QUESTION': (data) => this.handleQuestion(data),
            'NEXT_QUESTION': (data) => this.handleQuestion(data),
            'HOST_LEFT': () => this.handleHostLeft(),
            'QUIZ_COMPLETED': (data) => this.handleQuizCompleted(data),
            'ERROR': (data) => this.handleError(data.message)
        };
    }

    startCountdown() {
        this.showLoading(false);
        
        const elements = {
            overlay: document.getElementById('timerOverlay'),
            number: document.getElementById('countdownNumber'),
            welcome: document.getElementById('welcomeMessage')
        };
        
        let count = 5;
        
        elements.overlay?.classList.add('active');
        elements.welcome?.classList.remove('show');
        
        this.timers.countdown = setInterval(() => {
            if (elements.number) {
                elements.number.textContent = count;
                this.animateCountdownNumber(elements.number);
            }
            
            if (--count < 0) {
                this.endCountdown(elements.overlay);
            }
        }, 1000);
    }

    animateCountdownNumber(element) {
        element.style.animation = 'none';
        setTimeout(() => {
            element.style.animation = 'countdownPulse 1s ease-in-out';
        }, 10);
    }

    endCountdown(overlay) {
        this.clearTimer('countdown');
        overlay?.classList.remove('active');
        console.log('Countdown finished - ready for questions!');
    }

    handleQuestion(questionData) {
        console.log('Handling question:', questionData);
        
        this.clearTimer('question');
        this.resetQuestionState(questionData);
        this.displayQuestion(questionData);
        this.startQuestionTimer(questionData.timeLimit);
    }

    resetQuestionState(questionData) {
        this.state.currentQuestion = questionData;
        this.state.selectedAnswer = null;
        this.state.timeRemaining = questionData.timeLimit;
        this.state.answerSubmitted = false;
    }

    displayQuestion(questionData) {
        const mainContent = document.querySelector('.quiz-playing-content');
        mainContent.innerHTML = this.createQuestionHTML(questionData);
        this.setupEventListeners(questionData);
    }

    createQuestionHTML(questionData) {
        const { questionOrder, questionText, points, timeLimit } = questionData;
        
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
                    </div>
                </div>
                
                <div class="question-timer" id="questionTimer">
                    <i class="fas fa-hourglass-half timer-icon"></i>
                    <span class="timer-text" id="timerText">${timeLimit}</span>
                    <div class="timer-progress" id="timerProgress"></div>
                </div>
                
                <div class="question-card-user">
                    ${this.createAnswersHTML(questionData)}
                    
                    <button class="btn-submit-answer" id="submitAnswerBtn" disabled>
                        <i class="fas fa-check"></i>
                        Potvrdi Odgovor
                    </button>
                </div>
            </div>
        `;
    }

    createAnswersHTML(questionData) {
        const { questionType, answers } = questionData;
        
        if (questionType === 'MULTIPLE_CHOICE' || questionType === 'TRUE_FALSE') {
            return this.createMultipleChoiceHTML(answers);
        }
        
        if (questionType === 'TEXT') {
            return this.createTextAnswerHTML();
        }
        
        return '';
    }

    createMultipleChoiceHTML(answers) {
        const sortedAnswers = [...answers].sort((a, b) => a.answerOrder - b.answerOrder);
        
        return `
            <div class="answer-options-user">
                ${sortedAnswers.map((answer, index) => `
                    <div class="answer-option-user" data-answer-id="${answer.id}" data-answer-index="${index}">
                        <div class="option-letter">${String.fromCharCode(65 + index)}</div>
                        <div class="option-text">${answer.answerText}</div>
                    </div>
                `).join('')}
            </div>
        `;
    }

    createTextAnswerHTML() {
        return `
            <div class="text-answer-container">
                <input type="text" 
                       class="text-answer-input" 
                       id="textAnswerInput"
                       placeholder="Unesite vaš odgovor..."
                       maxlength="200">
            </div>
        `;
    }

    setupEventListeners(questionData) {
        const submitBtn = document.getElementById('submitAnswerBtn');
        
        if (questionData.questionType === 'MULTIPLE_CHOICE' || questionData.questionType === 'TRUE_FALSE') {
            this.setupMultipleChoiceListeners(submitBtn);
        } else if (questionData.questionType === 'TEXT') {
            this.setupTextAnswerListeners(submitBtn);
        }
        
        submitBtn.addEventListener('click', () => this.submitAnswer());
    }

    setupMultipleChoiceListeners(submitBtn) {
        const options = document.querySelectorAll('.answer-option-user');
        
        options.forEach(option => {
            option.addEventListener('click', () => {
                if (this.state.answerSubmitted) return;
                
                options.forEach(opt => opt.classList.remove('selected'));
                
                option.classList.add('selected');
                
                this.state.selectedAnswer = {
                    answerId: option.dataset.answerId,
                    answerIndex: parseInt(option.dataset.answerIndex)
                };
                
                submitBtn.disabled = false;
                console.log('Selected answer:', this.state.selectedAnswer);
            });
        });
    }

    setupTextAnswerListeners(submitBtn) {
        const textInput = document.getElementById('textAnswerInput');
        
        textInput.addEventListener('input', () => {
            const text = textInput.value.trim();
            
            if (text.length > 0) {
                this.state.selectedAnswer = { text };
                submitBtn.disabled = false;
            } else {
                this.state.selectedAnswer = null;
                submitBtn.disabled = true;
            }
        });
        
        textInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !submitBtn.disabled) {
                this.submitAnswer();
            }
        });
    }

    startQuestionTimer(duration) {
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
        
        elements.progress.style.width = '100%';
        
        this.timers.question = setInterval(() => {
            this.state.timeRemaining--;
            this.updateTimerDisplay(elements, duration);
            
            if (this.state.timeRemaining <= 0) {
                this.onTimerExpired();
            }
        }, 1000);
    }

    updateTimerDisplay(elements, duration) {
        const { timeRemaining } = this.state;
        
        elements.text.textContent = timeRemaining;
        elements.progress.style.width = `${(timeRemaining / duration) * 100}%`;
        
        elements.timer.className = elements.timer.className.replace(/\b(warning|danger)\b/g, '');
        if (timeRemaining <= 5) {
            elements.timer.classList.add('danger');
        } else if (timeRemaining <= 10) {
            elements.timer.classList.add('warning');
        }
    }

    onTimerExpired() {
        this.state.timeRemaining = 0;
        this.clearTimer('question');
        this.autoSubmitAnswer();
    }

    submitAnswer() {
        if (this.state.answerSubmitted || this.timers.question === null) {
            return;
        }
        
        this.state.answerSubmitted = true;
        this.clearTimer('question');
        
        const { isCorrect, answerText } = this.validateAnswer();
        this.updateUI(isCorrect, answerText);
        this.sendAnswerToServer(isCorrect);
    }

    validateAnswer() {
        const correctAnswer = this.state.currentQuestion.answers.find(answer => answer.isCorrect);
        let isCorrect = false;
        let answerText = '';
        
        if (this.state.currentQuestion.questionType === 'TEXT') {
            const userText = this.state.selectedAnswer?.text?.trim().toLowerCase() || '';
            const correctText = correctAnswer?.answerText?.trim().toLowerCase() || '';
            isCorrect = userText === correctText;
            answerText = correctAnswer.answerText;
        } else {
            isCorrect = this.state.selectedAnswer && this.state.selectedAnswer.answerId === correctAnswer.id;
            answerText = correctAnswer.answerText;
        }
        
        return { isCorrect, answerText };
    }

    updateUI(isCorrect, correctAnswerText) {
        this.disableSubmitButton();
        
        if (this.state.currentQuestion.questionType === 'TEXT') {
            this.updateTextAnswerUI(isCorrect, correctAnswerText);
        } else {
            this.updateMultipleChoiceUI(isCorrect);
        }
        
        this.showResultMessage(isCorrect, correctAnswerText);
    }

    disableSubmitButton() {
        const submitBtn = document.getElementById('submitAnswerBtn');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fas fa-check"></i> Odgovor Poslan';
    }

    updateTextAnswerUI(isCorrect, correctAnswerText) {
        const textInput = document.getElementById('textAnswerInput');
        if (textInput) {
            textInput.disabled = true;
        }
    }

    updateMultipleChoiceUI(isCorrect) {
        const options = document.querySelectorAll('.answer-option-user');
        const correctAnswer = this.state.currentQuestion.answers.find(answer => answer.isCorrect);
        
        options.forEach(option => {
            const answerId = option.dataset.answerId;
            option.style.pointerEvents = 'none';
            
            if (answerId === correctAnswer.id) {
                option.classList.add('correct-answer');
            }
            
            if (this.state.selectedAnswer && 
                answerId === this.state.selectedAnswer.answerId && 
                !isCorrect) {
                option.classList.add('wrong-answer');
            }
        });
    }

    showResultMessage(isCorrect, correctAnswerText) {
        const questionCard = document.querySelector('.question-card-user');
        const submitButton = document.getElementById('submitAnswerBtn');
        
        const resultMessage = document.createElement('div');
        resultMessage.className = 'answer-result';
        
        if (isCorrect) {
            resultMessage.innerHTML = `
                <div class="result-message correct">
                    <span>Tačno! (+${this.state.currentQuestion.points} bodova)</span>
                </div>
            `;
        } else {
            resultMessage.innerHTML = `
                <div class="result-message incorrect">
                    <span>Netačno! Tačan odgovor: ${correctAnswerText}</span>
                </div>
            `;
        }
        
        questionCard.insertBefore(resultMessage, submitButton);
    }

    sendAnswerToServer(isCorrect) {
        const answerData = {
            type: 'SUBMIT_ANSWER',
            questionId: this.state.currentQuestion.questionId,
            participantId: this.config.participantId,
            timeRemaining: this.state.timeRemaining,
            isCorrect,
            ...this.state.selectedAnswer
        };
        
        if (this.ws?.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(answerData));
            console.log('Answer sent:', answerData);
        } else {
            console.error('WebSocket not available');
        }
    }

    autoSubmitAnswer() {
        console.log('Time expired - auto submit');
        this.submitAnswer();
    }

    handleHostLeft() {
        this.state.quizCompleted = true;
        window.location.href = '/trivia';
    }

    handleQuizCompleted(data) {
        this.state.quizCompleted = true;
        this.clearAllTimers();
        
        document.querySelector('.quiz-playing-content').innerHTML = `
            <div class="quiz-completed-container">
                <div class="completion-icon">
                    <i class="fas fa-trophy"></i>
                </div>
                <h2 class="completion-title">Kviz Završen!</h2>
                <p class="completion-message">${data.message}</p>
                <div class="completion-actions">
                    <button class="btn-primary" onclick="window.location.href='/trivia'">
                        <i class="fas fa-home"></i>
                        Povratak na početnu
                    </button>
                     <button class="btn-primary" onclick="window.location.href='/trivia/scores.html?sessionPin=${this.config.sessionPin}&isHost=false'">
                        <i class="fas fa-home"></i> Prikaz rezultata
                    </button>
                </div>
            </div>
        `;
    }

    handleError(errorMessage) {
        document.querySelector('.quiz-playing-content').innerHTML = `
            <div class="error-container">
                <div class="error-icon">
                    <i class="fas fa-exclamation-triangle"></i>
                </div>
                <h2 class="error-title">Greška</h2>
                <p class="error-message">${errorMessage}</p>
                <div class="error-actions">
                    <button class="btn-primary" onclick="window.location.href='/trivia'">
                        <i class="fas fa-home"></i>
                        Povratak na početnu
                    </button>
                </div>
            </div>
        `;
    }

    // Utility methods
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
        if (loading) {
            loading.style.display = show ? 'flex' : 'none';
        }
    }

    cleanup() {
        console.log('Cleaning up user session...');
        
        if (!this.state.quizCompleted && this.ws?.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({
                type: 'PARTICIPANT_LEFT',
                participantId: this.config.participantId
            }));
            console.log('Sent PARTICIPANT_LEFT message');
        } else {
            console.log('Quiz completed - not sending PARTICIPANT_LEFT');
        }
        
        this.clearAllTimers();
        
        if (this.ws) {
            this.ws.close();
        }
    }
}

window.quizUser = new QuizPlayingUser();