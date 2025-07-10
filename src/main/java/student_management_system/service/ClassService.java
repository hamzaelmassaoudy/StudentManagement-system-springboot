package student_management_system.service;

import student_management_system.model.SchoolClass;
import student_management_system.model.User;
import student_management_system.web.dto.ClassDto;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Interface defining operations for managing SchoolClass entities.
 */
public interface ClassService {

    SchoolClass createClass(ClassDto classDto, User teacher);

    List<SchoolClass> findClassesByTeacher(User teacher);

    Optional<SchoolClass> findClassById(Long classId);

    Optional<SchoolClass> findClassByCode(String classCode);

    SchoolClass enrollStudent(User student, String classCode, String joinPassword);

    Set<SchoolClass> findClassesByStudent(User student);

    /**
     * Updates an existing class.
     * Ensures the user performing the update is the teacher who owns the class.
     * @param classId The ID of the class to update.
     * @param classDto DTO containing updated class details (name, description).
     * @param teacher The User entity representing the teacher attempting the update.
     * @return The updated SchoolClass entity.
     * @throws RuntimeException if class not found or teacher not authorized.
     */
    SchoolClass updateClass(Long classId, ClassDto classDto, User teacher); // Added update method

    /**
     * Deletes a class owned by the specified teacher.
     * Note: Consider cascade implications (assignments, submissions, student enrollments).
     * @param classId The ID of the class to delete.
     * @param teacher The User entity representing the teacher attempting the deletion.
     * @throws RuntimeException if class not found or teacher not authorized.
     */
    void deleteClass(Long classId, User teacher); // Added delete method

}
