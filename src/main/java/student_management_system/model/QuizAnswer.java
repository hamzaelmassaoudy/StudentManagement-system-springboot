
// src/main/java/student_management_system/model/QuizAnswer.java
package student_management_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quiz_answers")
@Getter
@Setter
@NoArgsConstructor
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_attempt_id", nullable = false)
    private QuizAttempt quizAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    // For MULTIPLE_CHOICE answers
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id", nullable = true)
    private QuizOption selectedOption;

    // For SHORT_ANSWER answers
    @Lob
    @Column(columnDefinition = "TEXT", nullable = true)
    private String answerText;

    @Column(nullable = true) // Points awarded for this specific answer
    private Double pointsAwarded;

    public QuizAnswer(QuizAttempt quizAttempt, QuizQuestion question) {
        this.quizAttempt = quizAttempt;
        this.question = question;
    }
}
