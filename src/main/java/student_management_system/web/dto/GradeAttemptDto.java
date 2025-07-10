// src/main/java/student_management_system/web/dto/GradeAttemptDto.java
package student_management_system.web.dto; // Correct package

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min; // For points validation
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; // Import Collectors

/**
 * DTO for submitting grades for a quiz attempt.
 * Contains the attempt ID and a list of DTOs representing the grade for each answer.
 */
@Getter
@Setter
@NoArgsConstructor
public class GradeAttemptDto {

    @NotNull
    private Long attemptId;

    @Valid // Validate each item in the list
    private List<AnswerGradeDto> answerGrades = new ArrayList<>();

    // Constructor to initialize from QuizAnswerResultDto list (useful in controller)
    public GradeAttemptDto(Long attemptId, List<QuizAnswerResultDto> results) {
        this.attemptId = attemptId;
        if (results != null) {
            this.answerGrades = results.stream()
                .map(res -> new AnswerGradeDto(res.getQuestionId(), res.getPointsAwarded(), res.getQuestionPoints())) // Pass max points
                .collect(Collectors.toList());
        }
    }

    /**
     * Inner DTO representing the grade for a single answer.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class AnswerGradeDto {
        @NotNull
        private Long questionId;

        @NotNull(message = "Points awarded cannot be null.")
        @Min(value = 0, message = "Points cannot be negative.")
        // We'll validate against max points in the service layer as it requires fetching the question
        private Double pointsAwarded;

        private int maxPoints; // Store max points for potential validation/display

        // Optional: Add feedback field if grading individual answers
        // private String feedback;

        public AnswerGradeDto(Long questionId, Double pointsAwarded, int maxPoints) {
            this.questionId = questionId;
            this.pointsAwarded = pointsAwarded; // Can be null initially
            this.maxPoints = maxPoints;
        }
    }
}
