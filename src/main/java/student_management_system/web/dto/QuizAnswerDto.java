    // src/main/java/student_management_system/web/dto/QuizAnswerDto.java
    package student_management_system.web.dto;

    import lombok.Getter;
    import lombok.NoArgsConstructor;
    import lombok.Setter;
    import jakarta.validation.constraints.NotNull;

    /**
     * DTO for submitting a single answer from a student during a quiz attempt.
     * Simplified for Short Answer only.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public class QuizAnswerDto {
        @NotNull(message = "Question ID is required")
        private Long questionId;

        // Only field needed for short answer submission
        private String answerText;

        // Constructor for short answer (now the only one needed)
        public QuizAnswerDto(Long questionId, String answerText) {
            this.questionId = questionId;
            this.answerText = answerText;
        }
    }
    