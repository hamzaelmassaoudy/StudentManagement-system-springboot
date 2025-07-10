
// src/main/java/student_management_system/web/dto/QuizAnswerResultDto.java
package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO to represent the result of a single answer within an attempt
@Getter
@Setter
@NoArgsConstructor
public class QuizAnswerResultDto {
    private Long questionId;
    private String questionText;
    private String studentAnswerText; // Could be option text or written text
    private String correctAnswerText; // Correct option text or expected answer (if applicable)
    private boolean isCorrect; // Simple correct/incorrect flag
    private Double pointsAwarded;
    private int questionPoints; // Max points for the question
}
