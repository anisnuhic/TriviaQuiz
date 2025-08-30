package service;

import model.*;
import repository.QuizRepository;
import repository.QuestionRepository;
import repository.AnswerRepository;
import java.util.List;
import java.util.Optional;

public class QuizService {
    
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    
    public QuizService() {
        this.quizRepository = new QuizRepository();
        this.questionRepository = new QuestionRepository();
        this.answerRepository = new AnswerRepository();
    }
    
    public Quiz createQuiz(String title, String description, String category, String quizImage, User creator) throws Exception {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Quiz title cannot be empty");
        }
        if (creator == null) {
            throw new IllegalArgumentException("Quiz must have a creator");
        }
        
        Quiz quiz = new Quiz(title, creator);
        quiz.setDescription(description);
        quiz.setCategory(category);
        quiz.setQuizImage(quizImage); 
        
        return quizRepository.save(quiz);
    }
    
    public Quiz updateQuiz(Long quizId, String title, String description, String category, String quizImage) throws Exception {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new Exception("Quiz not found"));
        
        if (title != null && !title.trim().isEmpty()) {
            quiz.setTitle(title);
        }
        if (description != null) quiz.setDescription(description);
        if (category != null) quiz.setCategory(category);
        if (quizImage != null) quiz.setQuizImage(quizImage);
        
        return quizRepository.save(quiz);
    }
    
    public Question addQuestion(Long quizId, String questionText, String questionImage, 
                              Integer timeLimit, Integer points, QuestionType type) throws Exception {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new Exception("Quiz not found"));
        
        if (questionText == null || questionText.trim().isEmpty()) {
            throw new IllegalArgumentException("Question text cannot be empty");
        }
        
        Integer questionOrder = questionRepository.getMaxQuestionOrder(quiz) + 1;
        
        Question question = new Question(questionText, quiz, questionOrder);
        question.setQuestionImage(questionImage);
        question.setTimeLimit(timeLimit != null ? timeLimit : 30);
        question.setPoints(points != null ? points : 1);
        question.setQuestionType(type != null ? type : QuestionType.MULTIPLE_CHOICE);
        
        return questionRepository.save(question);
    }
    
public List<Quiz> findAllQuizzes() {
    return quizRepository.findAllWithCreators();
}


    public Answer addAnswer(Long questionId, String answerText, Boolean isCorrect) throws Exception {
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new Exception("Question not found"));
        
        if (answerText == null || answerText.trim().isEmpty()) {
            throw new IllegalArgumentException("Answer text cannot be empty");
        }
        
        Integer answerOrder = answerRepository.getMaxAnswerOrder(question) + 1;
        
        Answer answer = new Answer(answerText, question, answerOrder, isCorrect != null ? isCorrect : false);
        
        return answerRepository.save(answer);
    }
    
    public void addMultipleAnswers(Long questionId, List<String> answerTexts, int correctAnswerIndex) throws Exception {
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new Exception("Question not found"));
        
        if (answerTexts == null || answerTexts.isEmpty()) {
            throw new IllegalArgumentException("At least one answer is required");
        }
        if (correctAnswerIndex < 0 || correctAnswerIndex >= answerTexts.size()) {
            throw new IllegalArgumentException("Invalid correct answer index");
        }
        
        
        answerRepository.deleteByQuestion(question);
        
        
        for (int i = 0; i < answerTexts.size(); i++) {
            Answer answer = new Answer(
                answerTexts.get(i), 
                question, 
                i + 1, 
                i == correctAnswerIndex
            );
            answerRepository.save(answer);
        }
    }
    
    public Quiz duplicateQuiz(Long quizId, User newCreator) throws Exception {
        Quiz originalQuiz = quizRepository.findByIdWithQuestions(quizId)
            .orElseThrow(() -> new Exception("Quiz not found"));
        
        
        Quiz newQuiz = new Quiz(originalQuiz.getTitle() + " (Copy)", newCreator);
        newQuiz.setDescription(originalQuiz.getDescription());
        newQuiz.setCategory(originalQuiz.getCategory());
        newQuiz = quizRepository.save(newQuiz);
        
        
        for (Question originalQuestion : originalQuiz.getQuestions()) {
            Question newQuestion = new Question(
                originalQuestion.getQuestionText(),
                newQuiz,
                originalQuestion.getQuestionOrder()
            );
            newQuestion.setQuestionImage(originalQuestion.getQuestionImage());
            newQuestion.setTimeLimit(originalQuestion.getTimeLimit());
            newQuestion.setPoints(originalQuestion.getPoints());
            newQuestion.setQuestionType(originalQuestion.getQuestionType());
            newQuestion = questionRepository.save(newQuestion);
            
            
            List<Answer> originalAnswers = answerRepository.findByQuestion(originalQuestion);
            for (Answer originalAnswer : originalAnswers) {
                Answer newAnswer = new Answer(
                    originalAnswer.getAnswerText(),
                    newQuestion,
                    originalAnswer.getAnswerOrder(),
                    originalAnswer.getIsCorrect()
                );
                answerRepository.save(newAnswer);
            }
        }
        
        return newQuiz;
    }
    
    public void deleteQuiz(Long quizId) throws Exception {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new Exception("Quiz not found"));
        quizRepository.delete(quiz);
    }
    
    public void deleteQuestion(Long questionId) throws Exception {
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new Exception("Question not found"));
        questionRepository.delete(question);
    }
    
    public void deleteAnswer(Long answerId) throws Exception {
        Answer answer = answerRepository.findById(answerId)
            .orElseThrow(() -> new Exception("Answer not found"));
        answerRepository.delete(answer);
    }
    
    public Quiz activateQuiz(Long quizId) throws Exception {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new Exception("Quiz not found"));
        quiz.setIsActive(true);
        return quizRepository.save(quiz);
    }
    
    public Quiz deactivateQuiz(Long quizId) throws Exception {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new Exception("Quiz not found"));
        quiz.setIsActive(false);
        return quizRepository.save(quiz);
    }
    
    
    public Optional<Quiz> findQuizById(Long id) {
        return quizRepository.findById(id);
    }
    
    public Optional<Quiz> findQuizWithQuestions(Long id) {
        return quizRepository.findByIdWithQuestions(id);
    }
    
    public List<Quiz> findQuizzesByCreator(User creator) {
        return quizRepository.findByCreator(creator);
    }

    
    public List<Quiz> findActiveQuizzes() {
        return quizRepository.findActiveQuizzes();
    }
    
    public List<Quiz> findQuizzesByCategory(String category) {
        return quizRepository.findByCategory(category);
    }
    
    public List<Quiz> searchQuizzes(String searchTerm) {
        return quizRepository.searchByTitle(searchTerm);
    }
    
    public List<String> getAllCategories() {
        return quizRepository.findAllCategories();
    }
    
    public List<Question> getQuizQuestions(Long quizId) throws Exception {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new Exception("Quiz not found"));
        return questionRepository.findByQuizWithAnswers(quiz);
    }
    
    public Long getQuizCount(User creator) {
        return quizRepository.countByCreator(creator);
    }
}