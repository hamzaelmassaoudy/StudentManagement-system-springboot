package student_management_system.service;

import student_management_system.model.Assignment;
import student_management_system.model.SchoolClass;
import student_management_system.model.User; // Needed for permission checks
import student_management_system.web.dto.AssignmentDto;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Interface defining operations for managing Assignment entities.
 */
public interface AssignmentService {

    Assignment createAssignment(AssignmentDto assignmentDto, Long classId, User teacher);

    List<Assignment> findAssignmentsByClass(SchoolClass schoolClass);

    List<Assignment> findAssignmentsByClassId(Long classId);

    Optional<Assignment> findAssignmentById(Long assignmentId);

    List<Assignment> findAssignmentsForStudent(User student);

    /**
     * Finds assignments for a student that are upcoming or recently past due
     * and have not yet been submitted.
     *
     * @param student The student user.
     * @param limit The maximum number of assignments to return.
     * @return A list of pending assignments, ordered by due date.
     */
    // *** Ensure this method signature is present ***
    List<Assignment> findPendingAssignmentsForStudent(User student, int limit);

    Assignment updateAssignment(Long assignmentId, AssignmentDto dto, User teacher);

    void deleteAssignment(Long assignmentId, User teacher);

}
