package service;

import model.Quiz;
import model.QuizSession;
import model.User;
import repository.QuizSessionRepository;
import repository.QuizRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

public class QuizSessionService {
    private final QuizSessionRepository sessionRepository;
    private final QuizRepository quizRepository;
    private final SecureRandom random;

    public QuizSessionService() {
        this.sessionRepository = new QuizSessionRepository();
        this.quizRepository = new QuizRepository();
        this.random = new SecureRandom();
    }

    public QuizSession createQuizSession(Long quizId, User admin) throws Exception {

        Optional<Quiz> quizOpt = quizRepository.findById(quizId);
        if (!quizOpt.isPresent()) {
            throw new RuntimeException("Kviz sa ID " + quizId + " ne postoji");
        }

        Quiz quiz = quizOpt.get();

        String sessionPin = generateUniquePin();

        QuizSession session = new QuizSession(quiz, admin, sessionPin);

        return sessionRepository.save(session);
    }

    public Optional<QuizSession> findBySessionPinWithQuiz(String sessionPin) {
        // Ova metoda treba da eager-load Quiz objekat
        try {
            // Prvo pronadji sesiju
            Optional<QuizSession> sessionOpt = sessionRepository.findBySessionPin(sessionPin);

            if (sessionOpt.isPresent()) {
                QuizSession session = sessionOpt.get();

                // Eksplicitno ucitaj Quiz preko QuizService
                if (session.getQuiz() != null && session.getQuiz().getId() != null) {
                    QuizService quizService = new QuizService();
                    Optional<Quiz> quizOpt = quizService.findQuizById(session.getQuiz().getId());

                    if (quizOpt.isPresent()) {
                        // Setuj fully loaded quiz objekat
                        session.setQuiz(quizOpt.get());
                    }
                }
            }

            return sessionOpt;

        } catch (Exception e) {
            System.err.println("Greška pri učitavanju sesije sa kvizom: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private String generateUniquePin() {
        String pin;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            pin = generateRandomPin();
            attempts++;

            if (attempts > maxAttempts) {
                throw new RuntimeException("Nije moguće generisati jedinstven PIN nakon " + maxAttempts + " pokušaja");
            }
        } while (sessionRepository.existsBySessionPin(pin));

        return pin;
    }

    private String generateRandomPin() {
        int pin = 100000 + random.nextInt(900000);
        return String.valueOf(pin);
    }

    public Optional<QuizSession> findBySessionPin(String sessionPin) {
        return sessionRepository.findBySessionPin(sessionPin);
    }

    public Optional<QuizSession> findActiveSessionByPin(String sessionPin) {
        return sessionRepository.findActiveSessionByPin(sessionPin);
    }

    public QuizSession finishSession(Long sessionId) throws Exception {
        Optional<QuizSession> sessionOpt = sessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            throw new RuntimeException("Sesija sa ID " + sessionId + " ne postoji");
        }

        QuizSession session = sessionOpt.get();
        session.setStatus(model.SessionStatus.FINISHED);
        session.setFinishedAt(java.time.LocalDateTime.now());

        return sessionRepository.save(session);
    }

    public QuizSession startSession(Long sessionId) throws Exception {
        Optional<QuizSession> sessionOpt = sessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            throw new RuntimeException("Sesija sa ID " + sessionId + " ne postoji");
        }

        QuizSession session = sessionOpt.get();
        session.setStatus(model.SessionStatus.ACTIVE);

        return sessionRepository.save(session);
    }

    public void deleteSession(Long sessionId) throws Exception {
        Optional<QuizSession> sessionOpt = sessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            throw new RuntimeException("Sesija sa ID " + sessionId + " ne postoji");
        }

        QuizSession session = sessionOpt.get();
        sessionRepository.delete(session);
    }

    public List<QuizSession> getSessionsByQuizId(Long quizId) {
    return sessionRepository.findByQuizId(quizId);
}
}