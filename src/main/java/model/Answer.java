package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "answers")
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;
    
    @Column(name = "is_correct")
    private Boolean isCorrect = false;
    
    @Column(name = "answer_order", nullable = false)
    private Integer answerOrder;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ParticipantAnswer> participantAnswers = new ArrayList<>();
    
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
    
    
    public Answer() {}
    
    public Answer(String answerText, Question question, Integer answerOrder, Boolean isCorrect) {
        this.answerText = answerText;
        this.question = question;
        this.answerOrder = answerOrder;
        this.isCorrect = isCorrect;
    }
    
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    
    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
    
    public Integer getAnswerOrder() { return answerOrder; }
    public void setAnswerOrder(Integer answerOrder) { this.answerOrder = answerOrder; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public List<ParticipantAnswer> getParticipantAnswers() { return participantAnswers; }
    public void setParticipantAnswers(List<ParticipantAnswer> participantAnswers) { this.participantAnswers = participantAnswers; }
}