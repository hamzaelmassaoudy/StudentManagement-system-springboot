// src/main/java/student_management_system/model/QuizQuestion.java
package student_management_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private QuestionType questionType;

    @Column(nullable = false)
    private int points = 1; // Default points per question

    @Column(nullable = false)
    private int questionOrder; // To maintain question sequence

    // For MULTIPLE_CHOICE questions
    // Cascade ALL: If a question is deleted, its options are also deleted.
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER) // Eager fetch options with question
    private List<QuizOption> options = new ArrayList<>();

    // Enum for question types
    public enum QuestionType {
        MULTIPLE_CHOICE,
        SHORT_ANSWER
        // Add TRUE_FALSE, FILL_IN_BLANK etc. later if needed
    }

     // Helper method to add options and maintain the relationship
    public void addOption(QuizOption option) {
        options.add(option);
        option.setQuestion(this);
    }

    public void removeOption(QuizOption option) {
        options.remove(option);
        option.setQuestion(null);
    }
}
