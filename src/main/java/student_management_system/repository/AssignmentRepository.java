package student_management_system.repository;

import student_management_system.model.Assignment;
import student_management_system.model.SchoolClass; // Needed for parameters
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set; // Needed for findBySchoolClassIn

/**
 * Repository interface for Assignment entities.
 */
@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /**
     * Finds all assignments belonging to a specific SchoolClass.
     * @param schoolClass The SchoolClass entity.
     * @return A List of Assignment entities for that class.
     */
    List<Assignment> findBySchoolClass(SchoolClass schoolClass);

    /**
     * Finds all assignments belonging to a SchoolClass identified by its ID.
     * @param classId The ID of the SchoolClass.
     * @return A List of Assignment entities for that class ID.
     */
    List<Assignment> findBySchoolClass_Id(Long classId);

    /**
     * Finds all assignments belonging to any of the specified SchoolClasses.
     * @param classes A Set of SchoolClass entities.
     * @return A List of Assignment entities associated with any of the provided classes.
     */
    List<Assignment> findBySchoolClassIn(Set<SchoolClass> classes);

}
