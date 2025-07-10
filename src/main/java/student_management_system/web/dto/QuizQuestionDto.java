    // src/main/java/student_management_system/web/dto/QuizQuestionDto.java
    package student_management_system.web.dto;

    import lombok.Getter;
    import lombok.NoArgsConstructor;
    import lombok.Setter;
    import student_management_system.model.QuizQuestion.QuestionType; // Keep enum import
    import jakarta.validation.constraints.NotEmpty;
    import jakarta.validation.constraints.NotNull;
    import jakarta.validation.constraints.Min;

    /**
     * DTO for representing a quiz question in forms (Create/Edit).
     * Simplified for Short Answer only.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public class QuizQuestionDto {
        private Long id; // Useful for editing existing questions

        @NotEmpty(message = "Question text cannot be empty")
        private String questionText;

        // Question type is now implicitly SHORT_ANSWER, set in the service layer.

        @Min(value = 0, message = "Points cannot be negative")
        private int points = 1; // Default points

        private int questionOrder; // Order within the quiz

        // Constructor (optional, Lombok provides NoArgsConstructor)
        public QuizQuestionDto(String questionText, int points) {
            this.questionText = questionText;
            this.points = points;
        }
    }
    