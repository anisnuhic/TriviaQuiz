package service;

import model.Answer;
import model.ParticipantAnswer;
import model.Question;
import repository.AnswerRepository;
import repository.ParticipantAnswerRepository;
import repository.QuestionRepository;

import java.util.Optional;

public class ParticipantAnswerService {
    
    private ParticipantAnswerRepository participantAnswerRepository;
    private QuestionRepository questionRepository;
    private AnswerRepository answerRepository;
    
    public ParticipantAnswerService() {
        this.participantAnswerRepository = new ParticipantAnswerRepository();
        this.questionRepository = new QuestionRepository();
        this.answerRepository = new AnswerRepository();
    }
    
    public boolean saveParticipantAnswer(String participantId, Long questionId, Long answerId, String textAnswer) {
        try {
            // Pronađi question
            Optional<Question> questionOpt = questionRepository.findById(questionId);
            
            if (!questionOpt.isPresent()) {
                System.out.println("Question nije pronađen: " + questionId);
                return false;
            }
            
            Question question = questionOpt.get();
            
            // Provjeri da li je vec odgovorio na ovo pitanje
            if (participantAnswerRepository.hasParticipantAnsweredQuestion(participantId, question)) {
                System.out.println("Participant je već odgovorio na ovo pitanje");
                return false;
            }
            
            ParticipantAnswer participantAnswer;
            
            // Ako je multiple choice ili true/false, pronadji answer
            if (answerId != null) {
                Optional<Answer> answerOpt = answerRepository.findById(answerId);
                if (answerOpt.isPresent()) {
                    Answer answer = answerOpt.get();
                    participantAnswer = new ParticipantAnswer(participantId, question, answer);
                } else {
                    System.out.println("Answer nije pronađen: " + answerId);
                    return false;
                }
            } else if (textAnswer != null && !textAnswer.trim().isEmpty()) {
                // Text odgovor
                participantAnswer = new ParticipantAnswer(participantId, question, textAnswer.trim());
            } else {
                // Prazan odgovor
                participantAnswer = new ParticipantAnswer(participantId, question, (Answer) null);
            }
            
            // Sacuvaj participant answer
            participantAnswerRepository.save(participantAnswer);
            
            return true;
            
        } catch (Exception e) {
            System.out.println("Greška pri čuvanju participant answer: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void deleteAnswersBySessionPin(String sessionPin) {
    // Ova metoda treba da obrise sve participant_answers 
    // povezane sa participantima iz date sesije
    participantAnswerRepository.deleteBySessionPin(sessionPin);
}
}