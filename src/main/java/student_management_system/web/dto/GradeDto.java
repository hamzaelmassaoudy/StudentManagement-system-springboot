package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Import validation annotations
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size; // Keep for feedback if needed

/**
 * Data Transfer Object for submitting a grade and feedback for a submission.
 * Grade is now a numerical score between 0 and 100.
 */
@Getter
@Setter
@NoArgsConstructor
public class GradeDto {

    /**
     * The numerical grade assigned (e.g., 85.5, 90).
     * Must be between 0 and 100 inclusive.
     */
    @NotNull(message = "Grade score cannot be empty.")
    @Min(value = 0, message = "Grade must be at least 0.")
    @Max(value = 100, message = "Grade must be at most 100.")
    private Double grade; // Changed from String to Double

    /**
     * Text feedback from the teacher. Optional.
     */
    @Size(max = 5000, message = "Feedback cannot exceed 5000 characters.") // Example validation
    private String feedback;

    // Constructor updated to accept Double
    public GradeDto(Double grade, String feedback) {
        this.grade = grade;
        this.feedback = feedback;
    }
}
