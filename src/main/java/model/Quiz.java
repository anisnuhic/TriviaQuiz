package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 100)
    private String category;
    
    @Column(name = "quiz_image")
    private String quizImage;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("questionOrder ASC")
    private List<Question> questions = new ArrayList<>();
    
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuizSession> quizSessions = new ArrayList<>();
    
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    
    public Quiz() {}
    
    public Quiz(String title, User creator) {
        this.title = title;
        this.creator = creator;
    }
    
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getQuizImage() { return quizImage; }
    public void setQuizImage(String quizImage) { this.quizImage = quizImage; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
    
    public List<QuizSession> getQuizSessions() { return quizSessions; }
    public void setQuizSessions(List<QuizSession> quizSessions) { this.quizSessions = quizSessions; }
    
    public int getTotalQuestions() {
        return questions.size();
    }
    
    public int getTotalPoints() {
        return questions.stream().mapToInt(Question::getPoints).sum();
    }
}