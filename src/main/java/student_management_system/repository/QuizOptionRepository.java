
// src/main/java/student_management_system/repository/QuizOptionRepository.java
package student_management_system.repository;

import student_management_system.model.QuizOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizOptionRepository extends JpaRepository<QuizOption, Long> {
    // No custom methods needed yet, JpaRepository provides basic CRUD
}