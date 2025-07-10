// src/main/java/student_management_system/service/QuizAttemptService.java
package student_management_system.service;

import student_management_system.model.Quiz;
import student_management_system.model.QuizAttempt;
import student_management_system.model.User;
import student_management_system.web.dto.QuizSubmissionDto;
import student_management_system.web.dto.QuizAttemptDto; // For returning results
import student_management_system.web.dto.GradeAttemptDto; // Import for grading DTO

import java.util.List;
import java.util.Optional;


/**
 * Interface for managing student attempts at taking quizzes.
 */
public interface QuizAttemptService {

    /**
     * Starts or resumes an IN_PROGRESS quiz attempt for a student.
     * Records the start time if new, and sets the status to IN_PROGRESS.
     * Checks for existing completed attempts if only one is allowed.
     *
     * @param quiz The Quiz the student is attempting.
     * @param student The student taking the quiz.
     * @return The QuizAttempt entity (new or existing IN_PROGRESS).
     * @throws RuntimeException if an attempt already exists and retakes are not allowed, or due date passed.
     */
    QuizAttempt startQuizAttempt(Quiz quiz, User student);

    /**
     * Submits a student's answers for a quiz attempt.
     * Records the end time, saves answers, and updates the attempt status to SUBMITTED.
     * For short-answer quizzes, score is set to null initially.
     *
     * @param attemptId The ID of the QuizAttempt being submitted.
     * @param submissionDto The DTO containing the student's answers.
     * @param student The student submitting the quiz.
     * @return The completed QuizAttempt entity with status SUBMITTED.
     * @throws RuntimeException if the attempt is not found, not owned by the student, or not in progress.
     */
    QuizAttempt submitQuizAttempt(Long attemptId, QuizSubmissionDto submissionDto, User student);

    /**
     * Retrieves a specific quiz attempt by its ID, potentially for viewing results or grading.
     * Includes authorization check (student owns attempt or teacher owns class).
     *
     * @param attemptId The ID of the attempt.
     * @param accessingUser The user trying to view the attempt.
     * @return An Optional containing the QuizAttempt if found and authorized.
     */
    Optional<QuizAttempt> findAttemptByIdForUser(Long attemptId, User accessingUser);

     /**
     * Retrieves a student's attempt for a specific quiz.
     *
     * @param student The student user.
     * @param quiz The quiz entity.
     * @return An Optional containing the QuizAttempt if it exists.
     */
    Optional<QuizAttempt> findAttemptByStudentAndQuiz(User student, Quiz quiz);

    /**
     * Retrieves all attempts for a specific quiz (e.g., for teacher view).
     *
     * @param quizId The ID of the quiz.
     * @param teacher The teacher user (for authorization).
     * @return A List of QuizAttempt entities.
     * @throws RuntimeException if quiz not found or teacher not authorized.
     */
    List<QuizAttempt> findAttemptsByQuizForTeacher(Long quizId, User teacher);

    /**
     * Retrieves all attempts made by a specific student.
     *
     * @param student The student user.
     * @return A List of QuizAttempt entities.
     */
    List<QuizAttempt> findAttemptsByStudent(User student);

    /**
     * Converts a QuizAttempt entity into a DTO suitable for displaying results.
     * Includes calculated scores (if graded) and detailed answer results.
     *
     * @param attempt The QuizAttempt entity.
     * @return A QuizAttemptDto containing formatted results.
     */
    QuizAttemptDto getAttemptResultDto(QuizAttempt attempt);

    /**
     * Grades a submitted quiz attempt, typically involving manual grading of short answers.
     * Updates the points awarded for each answer, calculates the total score,
     * and sets the attempt status to GRADED.
     *
     * @param attemptId The ID of the QuizAttempt to grade.
     * @param gradeAttemptDto The DTO containing the grades for each answer.
     * @param teacher The teacher performing the grading (for authorization).
     * @return The updated and graded QuizAttempt entity.
     * @throws EntityNotFoundException if the attempt or related answers/questions are not found.
     * @throws AccessDeniedException if the teacher is not authorized to grade this attempt.
     * @throws IllegalStateException if the attempt is not in the SUBMITTED state.
     * @throws IllegalArgumentException if the grading data is invalid (e.g., points out of range).
     */
    QuizAttempt gradeQuizAttempt(Long attemptId, GradeAttemptDto gradeAttemptDto, User teacher);

    /**
     * Finds quiz attempts with a specific status for quizzes taught by a given teacher.
     * Used for the teacher dashboard to show quizzes needing grading.
     *
     * @param teacher The teacher user.
     * @param status The status to filter by (e.g., SUBMITTED).
     * @param limit The maximum number of attempts to return.
     * @return A list of QuizAttempt entities matching the criteria.
     */
    List<QuizAttempt> findAttemptsByTeacherAndStatus(User teacher, QuizAttempt.AttemptStatus status, int limit);

} // End of interface
