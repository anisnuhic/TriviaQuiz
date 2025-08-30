package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_participants")
public class QuizParticipant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSession session;
    
    @Column(name = "participant_name", nullable = false, length = 100)
    private String participantName;
    
    @Column(name = "participant_id", nullable = false, length = 50, unique = true)
    private String participantId; 
    
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParticipantStatus status = ParticipantStatus.WAITING;
    
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
    
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
    
    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;
    
    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers = 0;
    
    @Column(name = "total_answers", nullable = false)
    private Integer totalAnswers = 0;
    
    @Column(name = "average_response_time")
    private Double averageResponseTime; 
    
    
    public QuizParticipant() {}
    
    public QuizParticipant(QuizSession session, String participantName, String participantId) {
        this.session = session;
        this.participantName = participantName;
        this.participantId = participantId;
        this.joinedAt = LocalDateTime.now();
        this.status = ParticipantStatus.WAITING;
        this.totalScore = 0;
        this.correctAnswers = 0;
        this.totalAnswers = 0;
    }
    
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public QuizSession getSession() {
        return session;
    }
    
    public void setSession(QuizSession session) {
        this.session = session;
    }
    
    public String getParticipantName() {
        return participantName;
    }
    
    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }
    
    public String getParticipantId() {
        return participantId;
    }
    
    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }
    
    
    
    public ParticipantStatus getStatus() {
        return status;
    }
    
    public void setStatus(ParticipantStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }
    
    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
    
    public Integer getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }
    
    public Integer getCorrectAnswers() {
        return correctAnswers;
    }
    
    public void setCorrectAnswers(Integer correctAnswers) {
        this.correctAnswers = correctAnswers;
    }
    
    public Integer getTotalAnswers() {
        return totalAnswers;
    }
    
    public void setTotalAnswers(Integer totalAnswers) {
        this.totalAnswers = totalAnswers;
    }
    
    public Double getAverageResponseTime() {
        return averageResponseTime;
    }
    
    public void setAverageResponseTime(Double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }
    
    
    public double getAccuracyPercentage() {
        if (totalAnswers == 0) return 0.0;
        return (double) correctAnswers / totalAnswers * 100;
    }
    
    public boolean isFinished() {
        return status == ParticipantStatus.FINISHED;
    }
    
    public boolean isPlaying() {
        return status == ParticipantStatus.PLAYING;
    }
    
    public boolean isWaiting() {
        return status == ParticipantStatus.WAITING || status == ParticipantStatus.READY;
    }
    
    @Override
    public String toString() {
        return "QuizParticipant{" +
                "id=" + id +
                ", participantName='" + participantName + '\'' +
                ", participantId='" + participantId + '\'' +
                ", status=" + status +
                ", totalScore=" + totalScore +
                ", correctAnswers=" + correctAnswers +
                ", totalAnswers=" + totalAnswers +
                '}';
    }
}