package service;

import model.Answer;
import model.Question;
import repository.AnswerRepository;
import java.util.List;
import java.util.Optional;

public class AnswerService {
    private AnswerRepository answerRepository;
    
    public AnswerService() {
        this.answerRepository = new AnswerRepository();
    }
    
    public Answer save(Answer answer) {
        return answerRepository.save(answer);
    }
    
    public Optional<Answer> findById(Long id) {
        return answerRepository.findById(id);
    }
    
    public List<Answer> findAnswersByQuestion(Question question) {
        return answerRepository.findByQuestion(question);
    }
    
    public Optional<Answer> findCorrectAnswerForQuestion(Question question) {
        return answerRepository.findCorrectAnswerForQuestion(question);
    }
    
    public void delete(Answer answer) {
        answerRepository.delete(answer);
    }
    
    public void deleteByQuestion(Question question) {
        answerRepository.deleteByQuestion(question);
    }
    
    public List<Answer> saveAll(List<Answer> answers) {
        return answerRepository.saveAll(answers);
    }
    
    public Integer getNextAnswerOrder(Question question) {
        return answerRepository.getMaxAnswerOrder(question) + 1;
    }
}