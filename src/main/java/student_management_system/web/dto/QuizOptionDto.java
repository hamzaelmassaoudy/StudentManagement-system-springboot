// src/main/java/student_management_system/web/dto/QuizOptionDto.java
package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotEmpty; // Example validation

@Getter
@Setter
@NoArgsConstructor
public class QuizOptionDto {
    private Long id; // Useful for editing existing options

    @NotEmpty(message = "Option text cannot be empty")
    private String optionText;

    private boolean isCorrect = false;

    // Constructor for convenience
    public QuizOptionDto(String optionText, boolean isCorrect) {
        this.optionText = optionText;
        this.isCorrect = isCorrect;
    }
}