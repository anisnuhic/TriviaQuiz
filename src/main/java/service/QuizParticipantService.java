package service;

import model.QuizParticipant;
import model.QuizSession;
import model.ParticipantStatus;
import repository.QuizParticipantRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class QuizParticipantService {
    
    private final QuizParticipantRepository participantRepository;
    
    public QuizParticipantService() {
        this.participantRepository = new QuizParticipantRepository();
    }
    
    public QuizParticipant createParticipant(QuizSession session, String participantName) {
        
        if (participantRepository.existsBySessionAndName(session, participantName)) {
            throw new RuntimeException("Učesnik sa imenom '" + participantName + "' već postoji u ovoj sesiji");
        }

        String participantId = UUID.randomUUID().toString(); 
        
        
        QuizParticipant participant = new QuizParticipant();
        participant.setSession(session);
        participant.setParticipantName(participantName);
        participant.setParticipantId(participantId);
        participant.setStatus(ParticipantStatus.WAITING);
        participant.setJoinedAt(LocalDateTime.now());
        participant.setTotalScore(0);
        participant.setCorrectAnswers(0);
        participant.setTotalAnswers(0);
        
        return participantRepository.save(participant);
    }
    
    public List<QuizParticipant> getParticipantsBySession(QuizSession session) {
        return participantRepository.findBySession(session);
    }
    
    public Optional<QuizParticipant> findById(Long id) {
        return participantRepository.findById(id);
    }
    
    public Optional<QuizParticipant> findByParticipantId(String participantId) {
        
        
        List<QuizParticipant> allParticipants = participantRepository.findAll();
        return allParticipants.stream()
                .filter(p -> participantId.equals(p.getParticipantId()))
                .findFirst();
    }
    
    public Optional<QuizParticipant> findBySessionAndName(QuizSession session, String participantName) {
        return participantRepository.findBySessionAndName(session, participantName);
    }
    
    public boolean existsBySessionAndName(QuizSession session, String participantName) {
        return participantRepository.existsBySessionAndName(session, participantName);
    }
    
    public void removeParticipant(String participantId) {
        Optional<QuizParticipant> participantOpt = findByParticipantId(participantId);
        if (participantOpt.isPresent()) {
            participantRepository.delete(participantOpt.get());
        }
    }
    
    public QuizParticipant updateParticipantStatus(String participantId, ParticipantStatus status) {
        Optional<QuizParticipant> participantOpt = findByParticipantId(participantId);
        if (!participantOpt.isPresent()) {
            throw new RuntimeException("Učesnik sa ID '" + participantId + "' ne postoji");
        }
        
        QuizParticipant participant = participantOpt.get();
        participant.setStatus(status);
        return participantRepository.save(participant);
    }
    
    public QuizParticipant updateParticipantScore(String participantId, int totalScore, int correctAnswers, int totalAnswers) {
        Optional<QuizParticipant> participantOpt = findByParticipantId(participantId);
        if (!participantOpt.isPresent()) {
            throw new RuntimeException("Učesnik sa ID '" + participantId + "' ne postoji");
        }
        
        QuizParticipant participant = participantOpt.get();
        participant.setTotalScore(totalScore);
        participant.setCorrectAnswers(correctAnswers);
        participant.setTotalAnswers(totalAnswers);
        return participantRepository.save(participant);
    }
    
    public List<QuizParticipant> getTopParticipants(QuizSession session, int limit) {
        return participantRepository.findTopParticipantsBySession(session, limit);
    }
    
    public Long countParticipantsInSession(QuizSession session) {
        return participantRepository.countBySession(session);
    }
    
    public void removeAllParticipantsFromSession(QuizSession session) {
        participantRepository.deleteBySession(session);
    }
    
    public void markQuizFinishedForSession(QuizSession session) {
        List<QuizParticipant> participants = participantRepository.findBySession(session);
        for (QuizParticipant participant : participants) {
            if (participant.getStatus() != ParticipantStatus.DISCONNECTED) {
            participant.setStatus(ParticipantStatus.FINISHED);
            participant.setFinishedAt(LocalDateTime.now());
            participantRepository.save(participant);
        }
        }
    }

    public void markParticipantsPlaying(QuizSession session) {
        List<QuizParticipant> participants = participantRepository.findBySession(session);
        for (QuizParticipant participant : participants) {
            participant.setStatus(ParticipantStatus.PLAYING);
            participantRepository.save(participant);
        }
    }
}