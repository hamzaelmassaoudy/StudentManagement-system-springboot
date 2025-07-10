// src/main/java/student_management_system/repository/QuizRepository.java
package student_management_system.repository;

import student_management_system.model.Quiz;
import student_management_system.model.SchoolClass;
import student_management_system.model.User; // Import User
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    // Finds all quizzes associated with a specific class entity
    List<Quiz> findBySchoolClass(SchoolClass schoolClass);
    // Finds all quizzes associated with a specific class ID
    List<Quiz> findBySchoolClassId(Long classId);

    /**
     * Finds all quizzes associated with classes taught by a specific teacher.
     * @param teacher The teacher User entity.
     * @return A list of quizzes taught by the teacher.
     */
    List<Quiz> findBySchoolClass_Teacher(User teacher);
}
