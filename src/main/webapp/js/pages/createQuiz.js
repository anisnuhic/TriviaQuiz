

let questionCount = 0;
let questions = [];
let draggedElement = null;
let currentQuizId = null; 
let isEditMode = false; 


const categories = {
    'matematika': { name: 'Matematika', icon: 'functions' },
    'hemija': { name: 'Hemija', icon: 'science' },
    'biologija': { name: 'Biologija', icon: 'biotech' },
    'programiranje': { name: 'Programiranje', icon: 'code' },
    'historija': { name: 'Historija', icon: 'history_edu' },
    'geografija': { name: 'Geografija', icon: 'public' },
    'opsta-kultura': { name: 'Opšta kultura', icon: 'school' }
};


document.addEventListener('DOMContentLoaded', function() {
    checkEditMode();
    updateEmptyState();

    const deleteButton = document.getElementById('deleteBtn');
    if (!isEditMode) deleteButton.remove();
    
    
    document.getElementById('quizForm').addEventListener('submit', function(e) {
        e.preventDefault();
        saveQuiz(true);
    });
});


function checkEditMode() {
    const urlParams = new URLSearchParams(window.location.search);
    const quizId = urlParams.get('id');
    
    if (quizId) {
        isEditMode = true;
        currentQuizId = quizId;
        loadQuizForEdit(quizId);
        updatePageForEditMode();
    }
}


function updatePageForEditMode() {
    
    document.querySelector('.page-header h1').textContent = 'Uređivanje Kviza';
    document.querySelector('.page-header p').textContent = 'Uredite postojeći kviz mijenjanjem pitanja i postavki';
    
    
    const publishButton = document.querySelector('button[type="submit"]');
    const deleteButton = document.getElementById('deleteBtn');
    if (!isEditMode) deleteButton.remove();
    if (publishButton) {
        publishButton.textContent = 'Ažuriraj Kviz';
    }

    
    
}

async function deleteQuiz(event) {
    // Sprijeci bilo kakvu interakciju sa formom
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }

    // Provjeri da li smo u edit modu
    if (!isEditMode || !currentQuizId) {
        alert('Greška: Kviz se može brisati samo u edit modu');
        return;
    }

    // Potvrda brisanja
    const confirmDelete = confirm(
        'Da li ste sigurni da želite da obrišete ovaj kviz?\n\n' +
        'Ova akcija je nepovratna i svi podaci vezani za kviz će biti trajno obrisani.'
    );

    if (!confirmDelete) {
        return;
    }

    try {
        // Pokazuj loading indikator
        const deleteBtn = document.getElementById('deleteBtn');
        const originalText = deleteBtn.textContent;
        deleteBtn.disabled = true;
        deleteBtn.textContent = 'Brišem...';

        // Dobij trenutni user ID
        const currentUserId = currentUser.id;
        
        if (!currentUserId) {
            throw new Error('Ne mogu pronaći ID korisnika');
        }

        console.log('Šaljem DELETE zahtjev za kviz:', currentQuizId);
        
        // Posaljite DELETE zahtjev
        const response = await fetch(`/trivia/admin/api/quiz/${currentUserId}/${currentQuizId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        const responseData = await response.json();

        if (!response.ok) {
            // Specificne greske na osnovu status koda
            if (response.status === 401) {
                throw new Error('Niste prijavljeni. Molimo prijavite se ponovo.');
            } else if (response.status === 403) {
                throw new Error('Nemate dozvolu za brisanje ovog kviza.');
            } else if (response.status === 404) {
                throw new Error('Kviz nije pronađen ili je već obrisan.');
            } else {
                throw new Error(responseData.error || 'Greška pri brisanju kviza');
            }
        }

        console.log('Kviz uspješno obrisan:', responseData);

        // Uspjesno brisanje
        alert('Kviz je uspješno obrisan!');
        
        // Preusmjeri na dashboard
        window.location.href = 'dashboard.html';

    } catch (error) {
        console.error('Greška pri brisanju kviza:', error);
        alert('Greška pri brisanju kviza: ' + error.message);
        
        // Vrati dugme u normalno stanje
        const deleteBtn = document.getElementById('deleteBtn');
        if (deleteBtn) {
            deleteBtn.disabled = false;
            deleteBtn.textContent = originalText;
        }
    }
}


async function loadQuizForEdit(quizId) {
    try {
        
        
        const currentUserId = currentUser.id; 
        
        const response = await fetch(`/trivia/admin/api/quiz/${currentUserId}/${quizId}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                
            }
        });

        if (!response.ok) {
            throw new Error('Greška pri učitavanju kviza');
        }

        const quizData = await response.json();
        populateFormWithQuizData(quizData);
        
    } catch (error) {
        console.error('Greška pri učitavanju kviza:', error);
        alert('Greška pri učitavanju kviza. Molimo pokušajte ponovo.');
        window.location.href = 'dashboard.html';
    }
}

function displayExistingImage(imageName) {
    const uploadContent = document.getElementById('uploadContent');
    const container = uploadContent.parentElement;
    
    
    const existingImg = container.querySelector('.image-preview');
    const existingRemoveBtn = container.querySelector('.remove-image-btn');
    
    if (existingImg) existingImg.remove();
    if (existingRemoveBtn) existingRemoveBtn.remove();
    
    
    const img = document.createElement('img');
    img.src = `/trivia/uploads/${imageName}`; 
    img.className = 'image-preview';
    img.alt = 'Quiz image';
    
    
    const removeBtn = document.createElement('button');
    removeBtn.type = 'button';
    removeBtn.className = 'remove-image-btn';
    removeBtn.innerHTML = '<i class="material-icons">close</i>';
    removeBtn.onclick = function(event) {
        removeImage();
        event.stopPropagation();
    };
    
    
    uploadContent.style.display = 'none';
    
    
    container.appendChild(img);
    container.appendChild(removeBtn);
    container.classList.add('has-image');
    
    
    selectedImageName = imageName;
}


function populateFormWithQuizData(quizData) {
    
    document.getElementById('quizTitle').value = quizData.title || '';
    document.getElementById('quizDescription').value = quizData.description || '';
    
    
    const categoryMapping = {
        'Programiranje': 'programiranje',
        'Matematika': 'matematika',
        'Hemija': 'hemija',
        'Biologija': 'biologija',
        'Historija': 'historija',
        'Geografija': 'geografija',
        'Opšta kultura': 'opsta-kultura'
    };
    
    const mappedCategory = categoryMapping[quizData.category] || quizData.category.toLowerCase();
    document.getElementById('quizCategory').value = mappedCategory;

     if (quizData.quizImage) {
        selectedImageName = quizData.quizImage;
        displayExistingImage(quizData.quizImage);
    }

    
    questions = [];
    questionCount = 0;
    const questionsContainer = document.getElementById('questionsContainer');
    
    const existingQuestions = questionsContainer.querySelectorAll('.question-card');
    existingQuestions.forEach(q => q.remove());

    
    if (quizData.questions && quizData.questions.length > 0) {
        quizData.questions.forEach((questionData, index) => {
            addQuestionFromData(questionData, index + 1);
        });
    }
    
    updateEmptyState();
}


function addQuestionFromData(questionData, order) {
    questionCount++;
    const questionId = `question_${questionCount}`;
    
    
    const typeMapping = {
        'MULTIPLE_CHOICE': 'MULTIPLE_CHOICE',
        'TRUE_FALSE': 'TRUE_FALSE',
        'TEXT': 'TEXT'
    };
    
    
    let options = [];
    let correctAnswerIndex = 0;
    
    if (questionData.answers && questionData.answers.length > 0) {
        options = questionData.answers.map(answer => answer.text);
        correctAnswerIndex = questionData.answers.findIndex(answer => answer.isCorrect);
        if (correctAnswerIndex === -1) correctAnswerIndex = 0;
    }
    
    
    if (questionData.questionType === 'TRUE_FALSE') {
        options = ['Tačno', 'Netačno'];
        
        const correctAnswer = questionData.answers.find(answer => answer.isCorrect);
        if (correctAnswer) {
            correctAnswerIndex = correctAnswer.text === 'Tačno' ? 0 : 1;
        }
    }
    
    const transformedQuestionData = {
        id: questionId,
        originalId: questionData.id, 
        type: typeMapping[questionData.questionType] || 'MULTIPLE_CHOICE',
        text: questionData.text || '',
        options: options,
        correctAnswer: questionData.questionType === 'TEXT' ? 
            (questionData.answers[0]?.text || '') : correctAnswerIndex,
        points: questionData.points || 10,
        timeLimit: questionData.timeLimit || 30
    };
    
    questions.push(transformedQuestionData);
    renderQuestion(transformedQuestionData);
}


function getCurrentUserId() {
    
    
    return sessionStorage.getItem('currentUserId') || 
           localStorage.getItem('currentUserId') || 
           1; 
}

function getQuestionTypeLabel(type) {
    switch (type) {
        case 'MULTIPLE_CHOICE': return 'Više izbora';
        case 'TRUE_FALSE': return 'Tačno/Netačno';
        case 'TEXT': return 'Tekst';
        default: return 'Nepoznato';
    }
}

function toggleQuestionCollapse(questionId) {
    const questionElement = document.getElementById(questionId);
    if (questionElement) {
        questionElement.classList.toggle('collapsed');
        
        
        if (questionElement.classList.contains('collapsed')) {
            updateQuestionSummary(questionId);
        }
    }
}

function updateQuestionSummary(questionId) {
    const question = questions.find(q => q.id === questionId);
    const questionElement = document.getElementById(questionId);
    
    if (question && questionElement) {
        const summaryElement = questionElement.querySelector('.question-summary');
        if (summaryElement) {
            summaryElement.innerHTML = `
                <strong>Pitanje:</strong> ${question.text || 'Nije unešeno pitanje'}
                <br>
                <strong>Tip:</strong> ${getQuestionTypeLabel(question.type)} | 
                <strong>Bodovi:</strong> ${question.points} | 
                <strong>Vrijeme:</strong> ${question.timeLimit}s
            `;
        }
    }
}


let selectedImageName = null;
function handleImageUpload(input) {
    const file = input.files[0];
    if (!file) return;
    
    
    if (!file.type.startsWith('image/')) {
        alert('Molimo odaberite sliku (PNG, JPG, JPEG)');
        input.value = '';
        return;
    }
    
    if (file.size > 5 * 1024 * 1024) {
        alert('Slika je prevelika. Maksimalna veličina je 5MB.');
        input.value = '';
        return;
    }
    
    
    
    selectedImageName = null;
    
    const reader = new FileReader();
    reader.onload = function(e) {
        const uploadContent = document.getElementById('uploadContent');
        const container = uploadContent.parentElement;
        
        
        const existingImg = container.querySelector('.image-preview');
        const existingRemoveBtn = container.querySelector('.remove-image-btn');
        
        if (existingImg) existingImg.remove();
        if (existingRemoveBtn) existingRemoveBtn.remove();
        
        
        const img = document.createElement('img');
        img.src = e.target.result; 
        img.className = 'image-preview';
        img.alt = 'Quiz image';
        
        
        const removeBtn = document.createElement('button');
        removeBtn.type = 'button';
        removeBtn.className = 'remove-image-btn';
        removeBtn.innerHTML = '<i class="material-icons">close</i>';
        removeBtn.onclick = function(event) {
            removeImage();
            event.stopPropagation();
        };
        
        
        uploadContent.style.display = 'none';
        
        
        container.appendChild(img);
        container.appendChild(removeBtn);
        container.classList.add('has-image');
    };
    
    reader.readAsDataURL(file);
}


function removeImage() {
    const uploadContent = document.getElementById('uploadContent');
    const imageUpload = uploadContent.parentElement;
    const fileInput = document.getElementById('quizImage');
    
    
    selectedImageName = null;
    
    
    const existingImg = imageUpload.querySelector('.image-preview');
    const existingRemoveBtn = imageUpload.querySelector('.remove-image-btn');
    
    if (existingImg) existingImg.remove();
    if (existingRemoveBtn) existingRemoveBtn.remove();
    
    
    imageUpload.classList.remove('has-image');
    fileInput.value = '';
    
    
    uploadContent.style.display = 'flex';
    uploadContent.innerHTML = `
        <i class="material-icons">cloud_upload</i>
        <p>Kliknite da odaberete sliku</p>
        <small>PNG, JPG do 5MB</small>
    `;
    
    console.log('Image removed, selectedImageName set to null');
}


function createImageUploadHTML() {
    return `
        <div class="form-group">
            <label>Naslovna Slika</label>
            <div class="image-upload" onclick="document.getElementById('quizImage').click()">
                <input type="file" 
                       id="quizImage" 
                       accept="image/*" 
                       onchange="handleImageUpload(this)">
                <div class="upload-content" id="uploadContent">
                    <i class="material-icons">cloud_upload</i>
                    <p>Kliknite da odaberete sliku</p>
                    <small>PNG, JPG do 5MB</small>
                </div>
                <button type="button" class="remove-image-btn" onclick="removeImage(); event.stopPropagation();" style="display: none;">
                    <i class="material-icons">close</i>
                </button>
            </div>
        </div>
    `;
}

function addQuestion() {
    questionCount++;
    const questionId = `question_${questionCount}`;
    
    const questionData = {
        id: questionId,
        type: 'MULTIPLE_CHOICE',
        text: '',
        options: ['', '', '', ''],
        correctAnswer: 0,
        points: 10,
        timeLimit: 30
    };
    
    questions.push(questionData);
    renderQuestion(questionData);
    updateEmptyState();
}


function renderQuestion(questionData) {
    const questionsContainer = document.getElementById('questionsContainer');
    const questionElement = createQuestionElement(questionData);
    questionsContainer.appendChild(questionElement);
}


function createQuestionElement(questionData) {
    const questionDiv = document.createElement('div');
    questionDiv.className = 'question-card';
    questionDiv.id = questionData.id;
    questionDiv.draggable = true;
    
    questionDiv.innerHTML = `
        <div class="question-header">
            <div class="question-number">${questions.indexOf(questionData) + 1}</div>
            <div class="question-actions">
                <button type="button" class="collapse-toggle" onclick="toggleQuestionCollapse('${questionData.id}')">
                    <i class="material-icons">expand_less</i>
                </button>
                <i class="material-icons drag-handle">drag_indicator</i>
                <button type="button" class="btn-icon delete" onclick="removeQuestion('${questionData.id}')">
                    <i class="material-icons">delete</i>
                </button>
            </div>
        </div>
        
        <!-- Question Summary (visible when collapsed) -->
        <div class="question-summary">
            <strong>Pitanje:</strong> ${questionData.text || 'Nije unešeno pitanje'}
            <br>
            <strong>Tip:</strong> ${getQuestionTypeLabel(questionData.type)} | 
            <strong>Bodovi:</strong> ${questionData.points} | 
            <strong>Vrijeme:</strong> ${questionData.timeLimit}s
        </div>
        
        <!-- Question Content (hidden when collapsed) -->
        <div class="question-content">
            <div class="form-group">
                <label>Tekst pitanja *</label>
                <div class="input-container">
                    <i class="material-icons input-icon">help_outline</i>
                    <input type="text" 
                           class="form-input" 
                           placeholder="Unesite tekst pitanja..."
                           value="${questionData.text}"
                           onchange="updateQuestionText('${questionData.id}', this.value)">
                </div>
            </div>
            
            <div class="form-group">
                <label>Tip pitanja</label>
                <div class="question-type-selector">
                    <div class="type-option ${questionData.type === 'MULTIPLE_CHOICE' ? 'active' : ''}" 
                         onclick="changeQuestionType('${questionData.id}', 'MULTIPLE_CHOICE')">
                        Više izbora
                    </div>
                    <div class="type-option ${questionData.type === 'TRUE_FALSE' ? 'active' : ''}" 
                         onclick="changeQuestionType('${questionData.id}', 'TRUE_FALSE')">
                        Tačno/Netačno
                    </div>
                    <div class="type-option ${questionData.type === 'TEXT' ? 'active' : ''}" 
                         onclick="changeQuestionType('${questionData.id}', 'TEXT')">
                        Tekst
                    </div>
                </div>
            </div>
            
            <div class="answer-options" id="answers_${questionData.id}">
                ${renderAnswerOptions(questionData)}
            </div>
            
            <div class="question-meta">
                <div class="meta-field">
                    <label>Bodovi</label>
                    <input type="number" 
                           class="meta-input" 
                           value="${questionData.points}" 
                           min="1" 
                           onchange="updateQuestionMeta('${questionData.id}', 'points', this.value)">
                </div>
                <div class="meta-field">
                    <label>Vrijeme (sekunde)</label>
                    <input type="number" 
                           class="meta-input" 
                           value="${questionData.timeLimit}" 
                           min="5"
                           max="60"
                           onchange="updateQuestionMeta('${questionData.id}', 'timeLimit', this.value)">
                </div>
            </div>
        </div>
    `;
    
    
    addDragListeners(questionDiv);
    
    return questionDiv;
}


function renderAnswerOptions(questionData) {
    switch (questionData.type) {
        case 'MULTIPLE_CHOICE':
            return questionData.options.map((option, index) => `
                <div class="answer-option">
                    <div class="option-radio ${questionData.correctAnswer === index ? 'checked' : ''}" 
                         onclick="setCorrectAnswer('${questionData.id}', ${index})"></div>
                    <input type="text" 
                           class="option-input" 
                           placeholder="Opcija ${index + 1}"
                           value="${option}"
                           onchange="updateOption('${questionData.id}', ${index}, this.value)">
                    ${questionData.options.length > 2 ? `
                        <i class="material-icons remove-option" 
                           onclick="removeOption('${questionData.id}', ${index})">close</i>
                    ` : ''}
                </div>
            `).join('') + (questionData.options.length < 6 ? `
                <button type="button" class="btn-secondary" onclick="addOption('${questionData.id}')" style="margin-top: 10px;">
                    <i class="material-icons">add</i> Dodaj opciju
                </button>
            ` : '');
            
        case 'TRUE_FALSE':
            return `
                <div class="answer-option">
                    <div class="option-radio ${questionData.correctAnswer === 0 ? 'checked' : ''}" 
                         onclick="setCorrectAnswer('${questionData.id}', 0)"></div>
                    <span class="option-input">Tačno</span>
                </div>
                <div class="answer-option">
                    <div class="option-radio ${questionData.correctAnswer === 1 ? 'checked' : ''}" 
                         onclick="setCorrectAnswer('${questionData.id}', 1)"></div>
                    <span class="option-input">Netačno</span>
                </div>
            `;
            
        case 'TEXT':
            return `
                <div class="form-group">
                    <label>Tačan odgovor</label>
                    <input type="text" 
                           class="form-input" 
                           placeholder="Unesite tačan odgovor..."
                           value="${questionData.correctAnswer || ''}"
                           onchange="updateQuestionMeta('${questionData.id}', 'correctAnswer', this.value)">
                </div>
            `;
    }
}


function changeQuestionType(questionId, newType) {
    const question = questions.find(q => q.id === questionId);
    if (question) {
        question.type = newType;
        
        
        switch (newType) {
            case 'MULTIPLE_CHOICE':
                question.options = ['', '', '', ''];
                question.correctAnswer = 0;
                break;
            case 'TRUE_FALSE':
                question.options = ['Tačno', 'Netačno'];
                question.correctAnswer = 0;
                break;
            case 'TEXT':
                question.options = [];
                question.correctAnswer = '';
                break;
        }
        
        
        const questionElement = document.getElementById(questionId);
        const wasCollapsed = questionElement.classList.contains('collapsed');
        const newElement = createQuestionElement(question);
        
        
        if (wasCollapsed) {
            newElement.classList.add('collapsed');
        }
        
        questionElement.parentNode.replaceChild(newElement, questionElement);
    }
}

function updateQuestionMeta(questionId, field, value) {
    const question = questions.find(q => q.id === questionId);
    if (question) {
        question[field] = field === 'points' || field === 'timeLimit' ? parseInt(value) : value;
        
        
        const questionElement = document.getElementById(questionId);
        if (questionElement && questionElement.classList.contains('collapsed')) {
            updateQuestionSummary(questionId);
        }
    }
}


function collapseAllQuestions() {
    questions.forEach(question => {
        const questionElement = document.getElementById(question.id);
        if (questionElement && !questionElement.classList.contains('collapsed')) {
            questionElement.classList.add('collapsed');
            updateQuestionSummary(question.id);
        }
    });
}


function expandAllQuestions() {
    questions.forEach(question => {
        const questionElement = document.getElementById(question.id);
        if (questionElement && questionElement.classList.contains('collapsed')) {
            questionElement.classList.remove('collapsed');
        }
    });
}

function setCorrectAnswer(questionId, answerIndex) {
    const question = questions.find(q => q.id === questionId);
    if (question) {
        question.correctAnswer = answerIndex;
        
        
        const answerContainer = document.getElementById(`answers_${questionId}`);
        const radios = answerContainer.querySelectorAll('.option-radio');
        radios.forEach((radio, index) => {
            radio.classList.toggle('checked', index === answerIndex);
        });
    }
}

function updateOption(questionId, optionIndex, value) {
    const question = questions.find(q => q.id === questionId);
    if (question && question.options) {
        question.options[optionIndex] = value;
    }
}

function addOption(questionId) {
    const question = questions.find(q => q.id === questionId);
    if (question && question.options && question.options.length < 6) {
        question.options.push('');
        
        
        const answerContainer = document.getElementById(`answers_${questionId}`);
        answerContainer.innerHTML = renderAnswerOptions(question);
    }
}

function removeOption(questionId, optionIndex) {
    const question = questions.find(q => q.id === questionId);
    if (question && question.options && question.options.length > 2) {
        question.options.splice(optionIndex, 1);
        
        
        if (question.correctAnswer >= optionIndex) {
            question.correctAnswer = Math.max(0, question.correctAnswer - 1);
        }
        
        
        const answerContainer = document.getElementById(`answers_${questionId}`);
        answerContainer.innerHTML = renderAnswerOptions(question);
    }
}

function removeQuestion(questionId) {
    if (confirm('Da li ste sigurni da želite da obrišete ovo pitanje?')) {
        questions = questions.filter(q => q.id !== questionId);
        document.getElementById(questionId).remove();
        updateQuestionNumbers();
        updateEmptyState();
    }
}

function updateEmptyState() {
    const emptyQuestions = document.getElementById('emptyQuestions');
    emptyQuestions.style.display = questions.length === 0 ? 'block' : 'none';
}


function addDragListeners(element) {
    element.addEventListener('dragstart', handleDragStart);
    element.addEventListener('dragover', handleDragOver);
    element.addEventListener('drop', handleDrop);
    element.addEventListener('dragend', handleDragEnd);
}

function handleDragStart(e) {
    draggedElement = this;
    this.classList.add('dragging');
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', this.id);
}

function handleDragOver(e) {
    if (e.preventDefault) {
        e.preventDefault();
    }
    e.dataTransfer.dropEffect = 'move';
    return false;
}

function handleDrop(e) {
    if (e.stopPropagation) {
        e.stopPropagation();
    }
    
    if (draggedElement && draggedElement !== this) {
        const draggedQuestionId = draggedElement.id;
        const targetQuestionId = this.id;
        
        
        const draggedIndex = questions.findIndex(q => q.id === draggedQuestionId);
        const targetIndex = questions.findIndex(q => q.id === targetQuestionId);
        
        if (draggedIndex !== -1 && targetIndex !== -1) {
            
            const draggedQuestion = questions[draggedIndex];
            questions.splice(draggedIndex, 1);
            questions.splice(targetIndex, 0, draggedQuestion);
            
            
            if (draggedIndex < targetIndex) {
                this.parentNode.insertBefore(draggedElement, this.nextSibling);
            } else {
                this.parentNode.insertBefore(draggedElement, this);
            }
            
            updateQuestionNumbers();
        }
    }
    
    return false;
}

function handleDragEnd(e) {
    if (this.classList) {
        this.classList.remove('dragging');
    }
    draggedElement = null;
}

function updateQuestionNumbers() {
    questions.forEach((question, index) => {
        if (question && question.id) {
            const questionElement = document.getElementById(question.id);
            if (questionElement) {
                const numberElement = questionElement.querySelector('.question-number');
                if (numberElement) {
                    numberElement.textContent = index + 1;
                }
            }
        }
    });
}


async function createNewQuiz(formData, publish) {
    const response = await fetch(`/trivia/admin/api/quiz/${currentUser.id}`, { 
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            ...formData,
            isActive: publish
        })
    });
    
    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || 'Greška pri kreiranju kviza');
    }
    
    return response.json();
}



function collectFormData() {
    const transformedQuestions = questions.map((question, index) => {
        console.log('Processing question:', question);
        
        const baseQuestion = {
            id: question.originalId || null,
            text: question.text,
            questionType: question.type,
            questionOrder: index + 1,
            timeLimit: question.timeLimit,
            points: question.points,
            answers: []
        };
        
        if (question.type === 'MULTIPLE_CHOICE') {
            baseQuestion.answers = question.options.map((option, optionIndex) => ({
                text: option,
                isCorrect: question.correctAnswer === optionIndex,
                answerOrder: optionIndex + 1
            }));
        } else if (question.type === 'TRUE_FALSE') {
            baseQuestion.answers = [
                {
                    text: 'Tačno',
                    isCorrect: question.correctAnswer === 0,
                    answerOrder: 1
                },
                {
                    text: 'Netačno', 
                    isCorrect: question.correctAnswer === 1,
                    answerOrder: 2
                }
            ];
        } else if (question.type === 'TEXT') {
            baseQuestion.answers = [{
                text: question.correctAnswer,
                isCorrect: true,
                answerOrder: 1
            }];
        }
        
        console.log('Transformed question:', baseQuestion);
        return baseQuestion;
    });
    
    const formData = {
        title: document.getElementById('quizTitle').value,
        description: document.getElementById('quizDescription').value,
        category: document.getElementById('quizCategory').value,
        
        questions: transformedQuestions
    };
    
    console.log('Final form data:', formData);
    return formData;
}
async function saveQuiz(publish = false) {
    console.log('Current questions array:', questions);
    
    try {
        
        let uploadedImageName = null;
        const fileInput = document.getElementById('quizImage');
        
        if (fileInput && fileInput.files[0]) {
            
            console.log('Uploading new image...');
            uploadedImageName = await uploadImage(fileInput.files[0]);
            console.log('New image uploaded with name:', uploadedImageName);
        } else if (selectedImageName) {
            
            console.log('Keeping existing image:', selectedImageName);
            uploadedImageName = selectedImageName;
        }
        
        
        
        const formData = collectFormData();
        formData.quizImage = uploadedImageName; 
        
        console.log('Final form data with image:', formData);
        
        if (!validateQuizData(formData)) {
            return;
        }
        
        
        let result;
        if (isEditMode) {
            result = await updateQuiz(formData, publish);
        } else {
            result = await createNewQuiz(formData, publish);
        }
        
        console.log('Server response:', result);
        
        if (publish) {
            alert(isEditMode ? 'Kviz je uspješno ažuriran!' : 'Kviz je uspješno objavljen!');
        } else {
            alert(isEditMode ? 'Izmjene su sačuvane!' : 'Kviz je sačuvan kao nacrt!');
        }
        
        window.location.href = 'dashboard.html';
        
    } catch (error) {
        console.error('Greška pri čuvanju kviza:', error);
        alert('Greška pri čuvanju kviza: ' + error.message);
    }
}

async function uploadImage(file) {
    const formData = new FormData();
    formData.append('image', file);
    
    try {
        const response = await fetch('/trivia/upload', {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            throw new Error('Greška pri upload-u slike');
        }
        
        const result = await response.json();
        
        
        if (result.fileName) {
            return result.fileName;
        } else {
            throw new Error('Server nije vratio ime fajla');
        }
        
    } catch (error) {
        console.error('Greška pri upload-u slike:', error);
        throw new Error('Greška pri upload-u slike: ' + error.message);
    }
}


function updateQuestionText(questionId, text) {
    const question = questions.find(q => q.id === questionId);
    if (question) {
        console.log(`Updating question ${questionId} text from "${question.text}" to "${text}"`);
        question.text = text;
        
        const questionElement = document.getElementById(questionId);
        if (questionElement && questionElement.classList.contains('collapsed')) {
            updateQuestionSummary(questionId);
        }
    } else {
        console.error(`Question with ID ${questionId} not found in questions array`);
    }
}

function validateQuizData(data) {
    console.log('Validating quiz data:', data); 
    
    if (!data.title || !data.title.trim()) {
        alert('Molimo unesite naziv kviza.');
        return false;
    }
    
    if (!data.category) {
        alert('Molimo izaberite kategoriju kviza.');
        return false;
    }
    
    if (!data.questions || data.questions.length === 0) {
        alert('Molimo dodajte najmanje jedno pitanje.');
        return false;
    }
    
    
    for (let i = 0; i < data.questions.length; i++) {
        const question = data.questions[i];
        console.log(`Validating question ${i + 1}:`, question); 
        
        if (!question.text || !question.text.trim()) {
            alert(`Pitanje ${i + 1} mora imati tekst.`);
            return false;
        }
        
        if (!question.answers || question.answers.length === 0) {
            alert(`Pitanje ${i + 1} mora imati najmanje jedan odgovor.`);
            return false;
        }
        
        if (question.questionType === 'MULTIPLE_CHOICE' || question.questionType === 'TRUE_FALSE') {
            
            const emptyAnswer = question.answers.find(answer => !answer.text || !answer.text.trim());
            if (emptyAnswer) {
                alert(`Sve opcije odgovora za pitanje ${i + 1} moraju biti popunjene.`);
                return false;
            }
            
            
            const hasCorrectAnswer = question.answers.some(answer => answer.isCorrect);
            if (!hasCorrectAnswer) {
                alert(`Pitanje ${i + 1} mora imati označen tačan odgovor.`);
                return false;
            }
        }
        
        if (question.questionType === 'TEXT') {
            if (!question.answers[0] || !question.answers[0].text || !question.answers[0].text.trim()) {
                alert(`Tekstualno pitanje ${i + 1} mora imati tačan odgovor.`);
                return false;
            }
        }
    }
    
    console.log('Validation passed!'); 
    return true;
}




async function updateQuiz(formData, publish) {
    const currentUserId = getCurrentUserId();
    
    const response = await fetch(`/trivia/admin/api/quiz/${currentUserId}/${currentQuizId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            ...formData,
            id: currentQuizId,
            isActive: publish
        })
    });
    
    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || 'Greška pri ažuriranju kviza');
    }
    
    return response.json();
}


function getCurrentUserId() {
    
    if (typeof currentUser !== 'undefined' && currentUser && currentUser.id) {
        return currentUser.id;
    }
    
    console.error('Ne mogu pronaći ID korisnika - currentUser nije definisan');
    return null;
}

async function loadQuizForEdit(quizId) {
    try {
        const currentUserId = getCurrentUserId();
        
        if (!currentUserId) {
            throw new Error('Ne mogu pronaći ID korisnika');
        }
        
        const response = await fetch(`/trivia/admin/api/quiz/${currentUserId}/${quizId}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!response.ok) {
            if (response.status === 401) {
                throw new Error('Niste prijavljeni');
            } else if (response.status === 403) {
                throw new Error('Nemate dozvolu za uređivanje ovog kviza');
            } else if (response.status === 404) {
                throw new Error('Kviz nije pronađen');
            } else {
                throw new Error('Greška pri učitavanju kviza');
            }
        }

        const quizData = await response.json();
        populateFormWithQuizData(quizData);
        
    } catch (error) {
        console.error('Greška pri učitavanju kviza:', error);
        alert('Greška pri učitavanju kviza: ' + error.message);
        window.location.href = 'dashboard.html';
    }
}
