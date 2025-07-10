package student_management_system.repository;

import student_management_system.model.SchoolClass;
import student_management_system.model.User; // Needed for teacher and student parameters
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for SchoolClass entities.
 */
@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    /**
     * Finds all classes taught by a specific teacher (User).
     * @param teacher The User entity representing the teacher.
     * @return A List of SchoolClass entities taught by the teacher.
     */
    List<SchoolClass> findByTeacher(User teacher);

    /**
     * Finds all classes a specific student (User) is enrolled in.
     * @param student The User entity representing the student.
     * @return A Set of SchoolClass entities the student is enrolled in.
     */
    Set<SchoolClass> findByStudentsContains(User student);

    /**
     * Finds a SchoolClass by its unique class code.
     * @param classCode The class code to search for.
     * @return An Optional containing the SchoolClass if found, or empty otherwise.
     */
    Optional<SchoolClass> findByClassCode(String classCode);

}
