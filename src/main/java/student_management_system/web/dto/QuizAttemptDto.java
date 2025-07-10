
// src/main/java/student_management_system/web/dto/QuizAttemptDto.java
package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import student_management_system.model.QuizAttempt.AttemptStatus; // Import enum

import java.time.LocalDateTime;
import java.util.List;

// DTO for displaying quiz attempt results/status
@Getter
@Setter
@NoArgsConstructor
public class QuizAttemptDto {
    private Long id;
    private Long quizId;
    private String quizTitle;
    private String studentUsername;
    private String studentFullName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double score;
    private Integer maxScore;
    private AttemptStatus status;
    private List<QuizAnswerResultDto> answerResults; // Include detailed answer results

    // Add constructors or mapping logic as needed
}