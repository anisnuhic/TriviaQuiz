package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    
    @Column(name = "question_image")
    private String questionImage;
    
    @Column(name = "time_limit", nullable = false)
    private Integer timeLimit = 30;
    
    @Column(nullable = false)
    private Integer points = 1;
    
    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private QuestionType questionType = QuestionType.MULTIPLE_CHOICE;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("answerOrder ASC")
    private List<Answer> answers = new ArrayList<>();
    
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ParticipantAnswer> participantAnswers = new ArrayList<>();
    
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    
    public Question() {}
    
    public Question(String questionText, Quiz quiz, Integer questionOrder) {
        this.questionText = questionText;
        this.quiz = quiz;
        this.questionOrder = questionOrder;
    }
    
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
    
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    
    public String getQuestionImage() { return questionImage; }
    public void setQuestionImage(String questionImage) { this.questionImage = questionImage; }
    
    public Integer getTimeLimit() { return timeLimit; }
    public void setTimeLimit(Integer timeLimit) { 
        this.timeLimit = (timeLimit != null && timeLimit > 60) ? 60 : timeLimit; 
    }
    
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    
    public Integer getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
    
    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<Answer> getAnswers() { return answers; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }
    
    public List<ParticipantAnswer> getParticipantAnswers() { return participantAnswers; }
    public void setParticipantAnswers(List<ParticipantAnswer> participantAnswers) { this.participantAnswers = participantAnswers; }
    
    public Answer getCorrectAnswer() {
        return answers.stream().filter(Answer::getIsCorrect).findFirst().orElse(null);
    }
}