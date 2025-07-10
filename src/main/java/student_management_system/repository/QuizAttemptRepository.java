// src/main/java/student_management_system/repository/QuizAttemptRepository.java
package student_management_system.repository;

import student_management_system.model.Quiz;
import student_management_system.model.QuizAttempt;
import student_management_system.model.User;
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    // Find a specific attempt by a student for a quiz (usually only one allowed)
    Optional<QuizAttempt> findByStudentAndQuiz(User student, Quiz quiz);
    // Find all attempts for a specific quiz (e.g., for teacher overview)
    List<QuizAttempt> findByQuiz(Quiz quiz);
    // Find all attempts made by a specific student
    List<QuizAttempt> findByStudent(User student);
    // Find all attempts for a specific quiz ID
    List<QuizAttempt> findByQuizId(Long quizId);

    /**
     * Finds quiz attempts for a given list of quizzes with a specific status,
     * applying pagination and sorting.
     *
     * @param quizzes The list of Quiz entities to search within.
     * @param status The desired AttemptStatus.
     * @param pageable Pageable object for limiting and sorting results.
     * @return A list of QuizAttempt entities matching the criteria.
     */
    List<QuizAttempt> findByQuizInAndStatus(List<Quiz> quizzes, QuizAttempt.AttemptStatus status, Pageable pageable);

}
