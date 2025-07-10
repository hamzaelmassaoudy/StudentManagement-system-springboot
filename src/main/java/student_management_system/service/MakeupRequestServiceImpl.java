package student_management_system.service;

import student_management_system.model.MakeupRequest;
import student_management_system.model.Submission;
import student_management_system.model.User;
import student_management_system.repository.MakeupRequestRepository;
import student_management_system.repository.SubmissionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.List; // Import List
import java.util.Optional; // Import Optional

/**
 * Implementation of the MakeupRequestService interface.
 * Handles business logic for creating and managing makeup requests.
 */
@Service
public class MakeupRequestServiceImpl implements MakeupRequestService {

    private static final Logger logger = LoggerFactory.getLogger(MakeupRequestServiceImpl.class);

    @Autowired
    private MakeupRequestRepository makeupRequestRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    // Threshold for grade eligibility (e.g., grade must be below 60%)
    private static final double MAKEUP_ELIGIBILITY_THRESHOLD = 60.0;

    /**
     * Creates a makeup request for a given submission after validating eligibility.
     */
    @Override
    @Transactional
    public MakeupRequest createMakeupRequest(Long submissionId, User student, String reason) {
        logger.info("Attempting to create makeup request for submission ID {} by student {}", submissionId, student.getUsername());
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found with ID: " + submissionId));

        // Authorization: Ensure the student owns the submission
        if (!submission.getStudent().getId().equals(student.getId())) {
            logger.warn("Access Denied: Student {} attempted to request makeup for submission ID {} owned by {}",
                    student.getUsername(), submissionId, submission.getStudent().getUsername());
            throw new AccessDeniedException("You can only request makeup for your own submissions.");
        }

        // Validation: Check if submission is graded
        if (submission.getGrade() == null || submission.getNumericalGrade() == null) {
            logger.warn("Makeup request denied for submission {}: Submission not graded yet.", submissionId);
            throw new IllegalArgumentException("Cannot request makeup for an ungraded submission.");
        }

        // Validation: Check if grade is below the threshold
        if (submission.getNumericalGrade() >= MAKEUP_ELIGIBILITY_THRESHOLD) {
             logger.warn("Makeup request denied for submission {}: Grade ({}) is not below threshold ({}).",
                     submissionId, submission.getNumericalGrade(), MAKEUP_ELIGIBILITY_THRESHOLD);
            throw new IllegalArgumentException("Submission grade is not eligible for a makeup request (must be below " + MAKEUP_ELIGIBILITY_THRESHOLD + "%).");
        }

        // Validation: Check if a request already exists
        if (makeupRequestRepository.findBySubmission(submission).isPresent()) {
            logger.warn("Makeup request denied for submission {}: Request already exists.", submissionId);
            throw new IllegalArgumentException("A makeup request already exists for this submission.");
        }

        // Create and save the new request
        MakeupRequest newRequest = new MakeupRequest(submission, student, reason);
        // Set the originalSubmissionId explicitly (assuming it's needed due to schema)
        newRequest.setOriginalSubmissionId(submission.getId());
        MakeupRequest savedRequest = makeupRequestRepository.save(newRequest);
        logger.info("Successfully created MakeupRequest ID {} for submission ID {} by student {}",
                savedRequest.getId(), submissionId, student.getUsername());
        return savedRequest;
    }

    /**
     * Finds all pending makeup requests for a specific teacher.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MakeupRequest> findPendingRequestsForTeacher(User teacher) {
        logger.debug("Fetching pending makeup requests for teacher: {}", teacher.getUsername());
        return makeupRequestRepository.findByTeacherAndStatus(teacher, MakeupRequest.MakeupRequestStatus.PENDING);
    }

    /**
     * Approves a specific makeup request after verifying teacher authorization and request status.
     */
    @Override
    @Transactional
    public MakeupRequest approveRequest(Long requestId, User teacher, String comment) {
        logger.info("Teacher {} attempting to approve makeup request ID: {}", teacher.getUsername(), requestId);
        MakeupRequest request = getRequestAndVerifyTeacher(requestId, teacher); // Fetches and validates

        request.setStatus(MakeupRequest.MakeupRequestStatus.APPROVED);
        request.setReviewedByTeacher(teacher);
        request.setTeacherComment(comment);
        // reviewedAt is set automatically by @PreUpdate listener in the entity

        MakeupRequest updatedRequest = makeupRequestRepository.save(request);
        logger.info("Makeup request ID {} approved by teacher {}", requestId, teacher.getUsername());

        // Note: No changes are made to the original Submission entity here.
        // The student controller will check the request status before allowing resubmission.

        return updatedRequest;
    }

    /**
     * Rejects a specific makeup request after verifying teacher authorization and request status.
     */
    @Override
    @Transactional
    public MakeupRequest rejectRequest(Long requestId, User teacher, String comment) {
        logger.info("Teacher {} attempting to reject makeup request ID: {}", teacher.getUsername(), requestId);
        MakeupRequest request = getRequestAndVerifyTeacher(requestId, teacher); // Fetches and validates

        request.setStatus(MakeupRequest.MakeupRequestStatus.REJECTED);
        request.setReviewedByTeacher(teacher);
        request.setTeacherComment(comment);
        // reviewedAt is set automatically by @PreUpdate listener in the entity

        MakeupRequest updatedRequest = makeupRequestRepository.save(request);
        logger.info("Makeup request ID {} rejected by teacher {}", requestId, teacher.getUsername());
        return updatedRequest;
    }

    /**
     * Finds an approved makeup request associated with a specific submission.
     * Delegates the call to the repository.
     */
    @Override
    @Transactional(readOnly = true) // Read-only operation
    public Optional<MakeupRequest> findApprovedRequestForSubmission(Submission submission) {
        logger.debug("Checking for approved makeup request for submission ID: {}", submission.getId());
        return makeupRequestRepository.findBySubmissionAndStatus(submission, MakeupRequest.MakeupRequestStatus.APPROVED);
    }

    /**
     * Finds *any* makeup request (regardless of status) associated with a specific submission.
     * Delegates the call to the repository.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<MakeupRequest> findRequestBySubmission(Submission submission) {
        logger.debug("Checking for any makeup request for submission ID: {}", submission.getId());
        return makeupRequestRepository.findBySubmission(submission);
    }

    /**
     * Finds all makeup requests submitted by a specific student.
     * Delegates the call to the repository.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MakeupRequest> findRequestsByStudent(User student) {
        logger.debug("Fetching all makeup requests for student: {}", student.getUsername());
        return makeupRequestRepository.findByStudentOrderByRequestedAtDesc(student);
    }


    /**
     * Helper method to fetch a request by ID and verify the teacher's authorization
     * to review it based on class ownership and request status.
     *
     * @param requestId The ID of the MakeupRequest.
     * @param teacher The teacher attempting the action.
     * @return The validated MakeupRequest entity.
     * @throws EntityNotFoundException if the request is not found.
     * @throws AccessDeniedException if the teacher is not authorized.
     * @throws IllegalArgumentException if the request is not in PENDING status.
     */
    private MakeupRequest getRequestAndVerifyTeacher(Long requestId, User teacher) {
        MakeupRequest request = makeupRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Makeup request not found with ID: " + requestId));

        // Verify the teacher owns the class associated with the request's submission
        Submission submission = request.getSubmission();
        if (submission == null ||
            submission.getAssignment() == null ||
            submission.getAssignment().getSchoolClass() == null ||
            submission.getAssignment().getSchoolClass().getTeacher() == null ||
            !submission.getAssignment().getSchoolClass().getTeacher().getId().equals(teacher.getId()))
        {
            logger.warn("Authorization Denied: Teacher {} attempted to review makeup request ID {} for a class they do not own.",
                    teacher.getUsername(), requestId);
            throw new AccessDeniedException("You are not authorized to review this makeup request.");
        }

        // Verify the request is actually pending
        if (request.getStatus() != MakeupRequest.MakeupRequestStatus.PENDING) {
             logger.warn("Action Denied: Attempt to review non-pending makeup request ID {} (Status: {}) by teacher {}",
                     requestId, request.getStatus(), teacher.getUsername());
            throw new IllegalArgumentException("This makeup request has already been reviewed (Status: " + request.getStatus() + ").");
        }

        return request;
    }
}
