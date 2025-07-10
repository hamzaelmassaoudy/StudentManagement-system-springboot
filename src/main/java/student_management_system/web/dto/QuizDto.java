
// src/main/java/student_management_system/web/dto/QuizDto.java
// *** THIS FILE IS NOW CORRECTED ***
package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotEmpty; // Example validation
import jakarta.validation.constraints.FutureOrPresent; // Example validation
import jakarta.validation.constraints.Min; // Example validation
import jakarta.validation.Valid; // To validate nested DTOs
import org.springframework.format.annotation.DateTimeFormat; // For date binding

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QuizDto {
    private Long id; // Useful for editing

    @NotEmpty(message = "Quiz title cannot be empty")
    private String title;

    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @FutureOrPresent(message = "Due date must be in the present or future, or left blank")
    private LocalDateTime dueDate; // Optional due date

    @Min(value = 0, message = "Time limit cannot be negative")
    private Integer timeLimitMinutes; // Optional time limit (null means no limit)

    @Valid
    private List<QuizQuestionDto> questions = new ArrayList<>();

    // Helper to add a default empty question for the form
    public void addEmptyQuestion() {
        QuizQuestionDto newQuestion = new QuizQuestionDto();
        // --- REMOVED: Calls to addEmptyOption() ---
        // newQuestion.addEmptyOption(); // No longer exists
        // newQuestion.addEmptyOption(); // No longer exists
        // --- END REMOVED ---
        this.questions.add(newQuestion);
    }
}
