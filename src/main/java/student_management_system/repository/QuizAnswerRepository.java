
// src/main/java/student_management_system/repository/QuizAnswerRepository.java
package student_management_system.repository;

import student_management_system.model.QuizAnswer;
import student_management_system.model.QuizAttempt; // Import QuizAttempt
import student_management_system.model.QuizQuestion; // Import QuizQuestion
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Import List
import java.util.Optional; // Import Optional

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    /**
     * Finds all answers associated with a specific quiz attempt.
     * Useful for retrieving all of a student's answers for grading or display.
     *
     * @param quizAttempt The QuizAttempt entity.
     * @return A List of QuizAnswer entities belonging to the attempt.
     */
    List<QuizAnswer> findByQuizAttempt(QuizAttempt quizAttempt);

    /**
     * Finds a specific answer within an attempt for a given question.
     * Useful if updating or retrieving a single answer.
     *
     * @param quizAttempt The QuizAttempt entity.
     * @param question The QuizQuestion entity.
     * @return An Optional containing the QuizAnswer if found.
     */
    Optional<QuizAnswer> findByQuizAttemptAndQuestion(QuizAttempt quizAttempt, QuizQuestion question);
}
