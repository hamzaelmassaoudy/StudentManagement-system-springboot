package student_management_system.web.dto;

import jakarta.validation.Valid; // <<< IMPORT THIS
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for submitting answers to a quiz attempt.
 * Contains the quiz ID and a list of answers provided by the student.
 */
@Getter
@Setter
@NoArgsConstructor
public class QuizSubmissionDto {

    /**
     * The ID of the Quiz being submitted.
     * Must not be null.
     */
    @NotNull(message = "Quiz ID cannot be null")
    private Long quizId;

    /**
     * The ID of the specific QuizAttempt this submission corresponds to.
     * Note: This is often derived from the URL path parameter in the controller
     * rather than being submitted directly in the form body. If submitted,
     * ensure it's correctly populated. Validation might be optional depending on usage.
     */
    // @NotNull(message = "Attempt ID cannot be null") // Uncomment if needed and submitted via form
    private Long attemptId;

    /**
     * The list of answers provided by the student.
     * The list itself cannot be null, and each item within the list will be validated
     * based on annotations within QuizAnswerDto (due to @Valid).
     */
    @Valid // <<< Ensures validation rules within QuizAnswerDto objects are checked
    @NotNull(message = "Answers list cannot be null")
    private List<QuizAnswerDto> answers = new ArrayList<>();

    /**
     * Helper method to add an answer DTO to the list, initializing the list if necessary.
     * @param answer The QuizAnswerDto to add.
     */
    public void addAnswer(QuizAnswerDto answer) {
        if (this.answers == null) {
            this.answers = new ArrayList<>();
        }
        this.answers.add(answer);
    }
}
