    // src/main/java/student_management_system/service/QuizService.java
    package student_management_system.service;

    import student_management_system.model.Quiz;
    import student_management_system.model.SchoolClass;
    import student_management_system.model.User;
    import student_management_system.web.dto.QuizDto;

    import java.util.List;
    import java.util.Optional;

    /**
     * Interface for managing Quiz entities and related operations.
     */
    public interface QuizService {

        /**
         * Creates a new quiz based on the provided DTO and associates it with a class and teacher.
         * Handles mapping from DTO to Quiz entity, including questions and options.
         *
         * @param quizDto The DTO containing quiz details, questions, and options.
         * @param classId The ID of the class the quiz belongs to.
         * @param teacher The teacher creating the quiz.
         * @return The newly created and persisted Quiz entity.
         * @throws RuntimeException if the class is not found or the teacher is not authorized.
         */
        Quiz createQuiz(QuizDto quizDto, Long classId, User teacher);

        /**
         * Finds a quiz by its ID. Performs necessary authorization checks if needed (e.g., student enrolled, teacher owns).
         * Consider adding separate methods for student/teacher views if authorization differs significantly.
         *
         * @param quizId The ID of the quiz to find.
         * @param accessingUser The user attempting to access the quiz (for authorization).
         * @return An Optional containing the Quiz if found and authorized, otherwise empty.
         */
        Optional<Quiz> findQuizByIdForUser(Long quizId, User accessingUser);

        /**
         * Finds all quizzes associated with a specific class ID.
         *
         * @param classId The ID of the class.
         * @return A List of Quiz entities for the class.
         */
        List<Quiz> findQuizzesByClassId(Long classId);


        /**
         * Updates an existing quiz.
         * Handles mapping from DTO, adding/removing/updating questions and options.
         * Ensures the user updating is the teacher who owns the class.
         *
         * @param quizId The ID of the quiz to update.
         * @param quizDto The DTO containing the updated quiz details.
         * @param teacher The teacher performing the update.
         * @return The updated Quiz entity.
         * @throws RuntimeException if the quiz is not found or the teacher is not authorized.
         */
        Quiz updateQuiz(Long quizId, QuizDto quizDto, User teacher);

        /**
         * Deletes a quiz.
         * Ensures the user deleting is the teacher who owns the class.
         * Handles cascade deletion of related entities (questions, options, attempts, answers).
         *
         * @param quizId The ID of the quiz to delete.
         * @param teacher The teacher performing the deletion.
         * @throws RuntimeException if the quiz is not found or the teacher is not authorized.
         */
        void deleteQuiz(Long quizId, User teacher);

        /**
         * Retrieves a quiz specifically for a student to take, performing necessary checks
         * (e.g., enrollment, due date, existing attempts).
         *
         * @param quizId The ID of the quiz.
         * @param student The student attempting to take the quiz.
         * @return The Quiz entity if the student is eligible to take it.
         * @throws RuntimeException if the quiz is not found, student not enrolled, already attempted, etc.
         */
        Quiz getQuizForTaking(Long quizId, User student);

    }
    