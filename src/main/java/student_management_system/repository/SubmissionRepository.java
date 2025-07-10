package student_management_system.repository;

import student_management_system.model.Submission;
import student_management_system.model.Assignment;
import student_management_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import Query
import org.springframework.data.repository.query.Param; // Import Param
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable; // Import Pageable

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Submission entities.
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssignment(Assignment assignment);

    List<Submission> findByStudent(User student);

    List<Submission> findByAssignment_Id(Long assignmentId);

    Optional<Submission> findByStudentAndAssignment(User student, Assignment assignment);

    /**
     * Finds all submissions for assignments taught by a specific teacher
     * where the submission has not yet been graded (grade is null).
     * Orders by submission date descending (most recent first).
     *
     * @param teacherId The ID of the teacher.
     * @return A list of ungraded submissions for the teacher's assignments.
     */
    @Query("SELECT s FROM Submission s JOIN FETCH s.assignment a JOIN FETCH a.schoolClass sc WHERE sc.teacher.id = :teacherId AND s.grade IS NULL ORDER BY s.submissionDate DESC")
    List<Submission> findUngradedSubmissionsForTeacherAssignments(@Param("teacherId") Long teacherId);

    /**
     * Finds the latest graded submissions for a specific student.
     * Fetches submissions where grade is not null, ordered by gradedDate descending.
     * Uses Pageable to limit the results.
     *
     * @param student The student user.
     * @param pageable Pageable object to limit and sort (e.g., PageRequest.of(0, 5, Sort.by("gradedDate").descending())).
     * @return A List of the latest graded submissions.
     */
    @Query("SELECT s FROM Submission s JOIN FETCH s.assignment a WHERE s.student = :student AND s.grade IS NOT NULL ORDER BY s.gradedDate DESC")
    List<Submission> findLatestGradedSubmissionsByStudent(@Param("student") User student, Pageable pageable);

}
