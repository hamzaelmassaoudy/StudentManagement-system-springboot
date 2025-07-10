package student_management_system.repository;

import student_management_system.model.MakeupRequest;
import student_management_system.model.Submission;
import student_management_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import Query
import org.springframework.data.repository.query.Param; // Import Param
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MakeupRequest entities.
 * Includes methods for finding requests based on various criteria.
 */
@Repository
public interface MakeupRequestRepository extends JpaRepository<MakeupRequest, Long> {

    /**
     * Finds if a request already exists for a specific submission.
     * Useful to prevent duplicate requests.
     *
     * @param submission The Submission entity.
     * @return An Optional containing the MakeupRequest if found, otherwise empty.
     */
    Optional<MakeupRequest> findBySubmission(Submission submission);

    /**
     * Finds all makeup requests submitted by a specific student, ordered by request date descending.
     *
     * @param student The student User entity.
     * @return A List of MakeupRequest objects submitted by the student.
     */
    List<MakeupRequest> findByStudentOrderByRequestedAtDesc(User student);

    /**
     * Finds makeup requests for assignments belonging to classes taught by the specified teacher,
     * filtered by a specific status.
     * Joins through Submission -> Assignment -> SchoolClass to filter by teacher.
     * Orders by request date ascending (oldest first).
     *
     * @param teacher The teacher User entity.
     * @param status The status to filter by (e.g., PENDING, APPROVED, REJECTED).
     * @return A list of MakeupRequest objects matching the criteria.
     */
    @Query("SELECT mr FROM MakeupRequest mr JOIN mr.submission s JOIN s.assignment a JOIN a.schoolClass sc WHERE sc.teacher = :teacher AND mr.status = :status ORDER BY mr.requestedAt ASC")
    List<MakeupRequest> findByTeacherAndStatus(@Param("teacher") User teacher, @Param("status") MakeupRequest.MakeupRequestStatus status);

    /**
     * Finds a makeup request associated with a specific submission and having a specific status.
     * This is useful for checking if an *approved* request exists before allowing resubmission.
     *
     * @param submission The Submission entity.
     * @param status The desired status (e.g., MakeupRequestStatus.APPROVED).
     * @return An Optional containing the MakeupRequest if found with the specified status, otherwise empty.
     */
    Optional<MakeupRequest> findBySubmissionAndStatus(Submission submission, MakeupRequest.MakeupRequestStatus status);

}
