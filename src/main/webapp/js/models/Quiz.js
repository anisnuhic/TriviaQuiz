
export class Quiz {
    constructor(quizData = null) {
        if (!quizData) {
            const saved = sessionStorage.getItem('currentQuiz');
            if (saved) {
                try {
                    quizData = JSON.parse(saved);
                } catch (e) {
                    console.error('Greška pri parsiranju kviza iz sessionStorage:', e);
                    quizData = {};
                }
            } else {
                quizData = {};
            }
        }

        this.id = quizData.id || null;
        this.title = quizData.title || '';
        this.description = quizData.description || '';
        this.category = quizData.category || '';
        this.quizImage = quizData.quizImage || null;
        this.questions = quizData.questions || [];
        this.createdAt = quizData.createdAt || null;
        this.updatedAt = quizData.updatedAt || null;
        this.authorId = quizData.authorId || null;
        this.authorName = quizData.authorName || '';
        this.isPublic = quizData.isPublic || false;
        this.difficulty = quizData.difficulty || 'medium';
        this.tags = quizData.tags || [];
    }

    
    getId() {
        return this.id;
    }

    getTitle() {
        return this.title || 'Nepoznat naziv';
    }

    getDescription() {
        return this.description || 'Nema opisa';
    }

    getCategory() {
        return this.category || '';
    }

    getQuestions() {
        return this.questions || [];
    }

    getQuestionCount() {
        return this.questions ? this.questions.length : 0;
    }

    getImageUrl() {
        return this.quizImage ? `/trivia/uploads/${this.quizImage}` : null;
    }

    
    getTotalPoints() {
        return this.questions.reduce((sum, question) => sum + (question.points || 10), 0);
    }

    getTotalTime() {
        return this.questions.reduce((sum, question) => sum + (question.timeLimit || 30), 0);
    }

    getEstimatedDurationMinutes() {
        const totalTime = this.getTotalTime();
        const questionCount = this.getQuestionCount();
        const estimatedSeconds = totalTime + (questionCount * 5); 
        return Math.ceil(estimatedSeconds / 60);
    }

    getFormattedQuestionCount() {
        const count = this.getQuestionCount();
        if (count === 1) return `${count} pitanje`;
        if (count < 5) return `${count} pitanja`;
        return `${count} pitanja`;
    }

    getFormattedTotalPoints() {
        const points = this.getTotalPoints();
        if (points === 1) return `${points} bod`;
        if (points < 5) return `${points} boda`;
        return `${points} bodova`;
    }

    getFormattedDuration() {
        return `~${this.getEstimatedDurationMinutes()} min`;
    }

    
    isValid() {
        return this.id && this.title && this.questions && this.questions.length > 0;
    }

    canStart() {
        return this.isValid();
    }

    getFormattedCreatedAt() {
        if (this.createdAt) {
            return new Date(this.createdAt).toLocaleDateString('sr-RS');
        }
        return 'Nepoznato';
    }

    
    static fromJson(jsonData) {
        return new Quiz(jsonData);
    }

    toJson() {
        return {
            id: this.id,
            title: this.title,
            description: this.description,
            category: this.category,
            quizImage: this.quizImage,
            questions: this.questions,
            createdAt: this.createdAt,
            updatedAt: this.updatedAt,
            authorId: this.authorId,
            authorName: this.authorName,
            isPublic: this.isPublic,
            difficulty: this.difficulty,
            tags: this.tags
        };
    }

    updateData(newData) {
        Object.keys(newData).forEach(key => {
            if (this.hasOwnProperty(key)) {
                this[key] = newData[key];
            }
        });
    }

    
    save() {
        sessionStorage.setItem('currentQuiz', JSON.stringify(this.toJson()));
    }

    
    static clear() {
        sessionStorage.removeItem('currentQuiz');
    }
}