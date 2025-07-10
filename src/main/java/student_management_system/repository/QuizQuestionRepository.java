
// src/main/java/student_management_system/repository/QuizQuestionRepository.java
package student_management_system.repository;

import student_management_system.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    // No custom methods needed yet, JpaRepository provides basic CRUD
    // Add custom query methods if needed later (e.g., find by quiz ordered by questionOrder)
    // List<QuizQuestion> findByQuizOrderByQuestionOrderAsc(Quiz quiz);
}