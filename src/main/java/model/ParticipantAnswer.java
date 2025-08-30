package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "participant_answers")
public class ParticipantAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "participant_id", nullable = false)
    private String participantId;  
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
    private Answer answer;
    
    @Column(name = "text_answer")
    private String textAnswer;  
    
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
    
    @Column(name = "points_earned")
    private Integer pointsEarned = 0;
    
    public ParticipantAnswer() {}
    
    public ParticipantAnswer(String participantId, Question question, Answer answer) {
        this.participantId = participantId;
        this.question = question;
        this.answer = answer;
        this.answeredAt = LocalDateTime.now();
        this.pointsEarned = (answer != null && answer.getIsCorrect()) ? question.getPoints() : 0;
    }
    
    public ParticipantAnswer(String participantId, Question question, String textAnswer) {
        this.participantId = participantId;
        this.question = question;
        this.textAnswer = textAnswer;
        this.answeredAt = LocalDateTime.now();
        this.pointsEarned = 0; 
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getParticipantId() { return participantId; }
    public void setParticipantId(String participantId) { this.participantId = participantId; }
    
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    
    public Answer getAnswer() { return answer; }
    public void setAnswer(Answer answer) { this.answer = answer; }
    
    public String getTextAnswer() { return textAnswer; }
    public void setTextAnswer(String textAnswer) { this.textAnswer = textAnswer; }
    
    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
    
    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }
    
    public boolean isCorrect() {
        return answer != null && answer.getIsCorrect();
    }
}