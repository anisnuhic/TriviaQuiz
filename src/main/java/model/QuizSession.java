package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_sessions")
public class QuizSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;
    
    @Column(name = "session_pin", unique = true, nullable = false, length = 6)
    private String sessionPin;
    
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.WAITING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_question_id")
    private Question currentQuestion;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuizParticipant> participants = new ArrayList<>();
    
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
    
    
    public QuizSession() {}
    
    public QuizSession(Quiz quiz, User admin, String sessionPin) {
        this.quiz = quiz;
        this.admin = admin;
        this.sessionPin = sessionPin;
    }
    
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
    
    public User getAdmin() { return admin; }
    public void setAdmin(User admin) { this.admin = admin; }
    
    public String getSessionPin() { return sessionPin; }
    public void setSessionPin(String sessionPin) { this.sessionPin = sessionPin; }
    
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    
    public Question getCurrentQuestion() { return currentQuestion; }
    public void setCurrentQuestion(Question currentQuestion) { this.currentQuestion = currentQuestion; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public List<QuizParticipant> getParticipants() { return participants; }
    public void setParticipants(List<QuizParticipant> participants) { this.participants = participants; }
    
    public int getParticipantCount() {
        return participants.size();
    }
}