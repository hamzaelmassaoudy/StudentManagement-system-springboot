package student_management_system.service;

import student_management_system.model.MakeupRequest;
import student_management_system.model.Submission;
import student_management_system.model.User;

import java.util.List;
import java.util.Optional; // Import Optional

/**
 * Interface defining operations for managing MakeupRequest entities.
 */
public interface MakeupRequestService {

    /**
     * Creates a new makeup request for a given submission.
     * Performs validation checks (e.g., submission exists, owned by student, grade eligible).
     *
     * @param submissionId The ID of the Submission to request makeup for.
     * @param student The User submitting the request.
     * @param reason The reason provided by the student for the request.
     * @return The newly created MakeupRequest object.
     * @throws RuntimeException if the submission is not found, not owned by the student,
     * already has a request, or is not eligible based on grade.
     */
    MakeupRequest createMakeupRequest(Long submissionId, User student, String reason);

    /**
     * Finds all pending makeup requests for assignments belonging to classes taught by a specific teacher.
     *
     * @param teacher The teacher User entity.
     * @return A List of pending MakeupRequest objects.
     */
    List<MakeupRequest> findPendingRequestsForTeacher(User teacher);

    /**
     * Approves a specific makeup request.
     * Updates the request status to APPROVED and sets the reviewer.
     *
     * @param requestId The ID of the MakeupRequest to approve.
     * @param teacher The teacher approving the request (for authorization).
     * @param comment Optional comment from the teacher.
     * @return The updated MakeupRequest object.
     * @throws RuntimeException if request not found, teacher not authorized, or request not pending.
     */
    MakeupRequest approveRequest(Long requestId, User teacher, String comment);

    /**
     * Rejects a specific makeup request.
     * Updates the request status to REJECTED and sets the reviewer.
     *
     * @param requestId The ID of the MakeupRequest to reject.
     * @param teacher The teacher rejecting the request (for authorization).
     * @param comment Optional comment from the teacher.
     * @return The updated MakeupRequest object.
     * @throws RuntimeException if request not found, teacher not authorized, or request not pending.
     */
    MakeupRequest rejectRequest(Long requestId, User teacher, String comment);

    /**
     * Finds an approved makeup request associated with a specific submission.
     * This is used by the student submission controller to check if resubmission is allowed.
     *
     * @param submission The Submission entity.
     * @return An Optional containing the approved MakeupRequest if found, otherwise empty.
     */
    Optional<MakeupRequest> findApprovedRequestForSubmission(Submission submission);

    /**
     * Finds *any* makeup request (regardless of status) associated with a specific submission.
     * Useful for checking if *any* request exists.
     *
     * @param submission The Submission entity.
     * @return An Optional containing the MakeupRequest if found, otherwise empty.
     */
    Optional<MakeupRequest> findRequestBySubmission(Submission submission); // Added method signature

    /**
     * Finds all makeup requests submitted by a specific student.
     *
     * @param student The student User entity.
     * @return A List of MakeupRequest objects submitted by the student, ordered by request date descending.
     */
    List<MakeupRequest> findRequestsByStudent(User student); // Added method signature

}
