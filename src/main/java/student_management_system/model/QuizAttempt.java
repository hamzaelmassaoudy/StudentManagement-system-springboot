

// src/main/java/student_management_system/model/QuizAttempt.java
package student_management_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private LocalDateTime startTime = LocalDateTime.now();

    @Column(nullable = true)
    private LocalDateTime endTime;

    @Column(nullable = true) // Score might be calculated later
    private Double score; // Final score (e.g., points or percentage)

    @Column(nullable = true) // Max possible score for this attempt
    private Integer maxScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    // Cascade ALL: If an attempt is deleted, its answers are deleted.
    @OneToMany(mappedBy = "quizAttempt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuizAnswer> answers = new ArrayList<>();

    public enum AttemptStatus {
        IN_PROGRESS,
        SUBMITTED,
        GRADED
    }

     public QuizAttempt(Quiz quiz, User student) {
        this.quiz = quiz;
        this.student = student;
    }

     // Helper method to add answers and maintain the relationship
    public void addAnswer(QuizAnswer answer) {
        answers.add(answer);
        answer.setQuizAttempt(this);
    }
}