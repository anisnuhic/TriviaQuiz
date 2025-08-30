package service;

import model.Question;
import model.Quiz;
import repository.QuestionRepository;
import java.util.List;
import java.util.Optional;

public class QuestionService {
    private QuestionRepository questionRepository;
    
    public QuestionService() {
        this.questionRepository = new QuestionRepository();
    }
    
    public Question save(Question question) {
        return questionRepository.save(question);
    }
    
    public Optional<Question> findById(Long id) {
        return questionRepository.findById(id);
    }
    
    public Optional<Question> findByIdWithAnswers(Long id) {
        return questionRepository.findByIdWithAnswers(id);
    }
    
    public List<Question> findQuestionsByQuiz(Quiz quiz) {
        return questionRepository.findByQuiz(quiz);
    }
    
    public List<Question> findQuestionsByQuizWithAnswers(Quiz quiz) {
        return questionRepository.findByQuizWithAnswers(quiz);
    }
    
    public void delete(Question question) {
        questionRepository.delete(question);
    }
    
    public void deleteByQuiz(Quiz quiz) {
        questionRepository.deleteByQuiz(quiz);
    }
    
    public Long countByQuiz(Quiz quiz) {
        return questionRepository.countByQuiz(quiz);
    }
    
    public Integer getNextQuestionOrder(Quiz quiz) {
        return questionRepository.getMaxQuestionOrder(quiz) + 1;
    }
}