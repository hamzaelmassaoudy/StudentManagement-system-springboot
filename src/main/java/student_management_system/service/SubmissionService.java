package student_management_system.service;

import student_management_system.model.Assignment;
import student_management_system.model.Submission;
import student_management_system.model.User;
import student_management_system.web.dto.GradeDto; // Import GradeDto
import student_management_system.web.dto.SubmissionDto;

import java.util.List;
import java.util.Map; // Import Map
import java.util.Optional;

/**
 * Interface defining operations for managing Submission entities.
 */
public interface SubmissionService {

    /**
     * Creates a new submission or updates an existing one (for makeup).
     * If the submissionDto indicates it's a makeup submission, it finds the existing
     * submission, resets its grade/feedback, updates content/file, and marks it as makeup.
     * If it's not a makeup submission, it creates a new submission record.
     * Performs authorization checks (student enrollment).
     * Handles file storage for new/updated submissions.
     *
     * @param submissionDto DTO containing submission content, file, and makeup flag.
     * @param assignmentId The ID of the assignment being submitted for.
     * @param student The student making the submission.
     * @return The created or updated Submission entity.
     * @throws RuntimeException if assignment not found, student not enrolled,
     * or if attempting a normal submission when one already exists,
     * or if attempting makeup without an approved request or existing submission.
     */
    Submission createOrUpdateSubmission(SubmissionDto submissionDto, Long assignmentId, User student); // Renamed and updated signature

    /**
     * Finds a submission made by a specific student for a specific assignment.
     *
     * @param student The student User entity.
     * @param assignment The Assignment entity.
     * @return An Optional containing the Submission if found, otherwise empty.
     */
    Optional<Submission> findByStudentAndAssignment(User student, Assignment assignment);

    /**
     * Finds all submissions for a specific assignment.
     *
     * @param assignment The Assignment entity.
     * @return A List of Submission entities for that assignment.
     */
    List<Submission> findSubmissionsByAssignment(Assignment assignment);

    /**
     * Finds all submissions made by a specific student.
     *
     * @param student The student User entity.
     * @return A List of Submission entities made by the student.
     */
    List<Submission> findSubmissionsByStudent(User student);

    /**
     * Finds a specific submission by its unique ID.
     *
     * @param submissionId The ID of the submission.
     * @return An Optional containing the Submission if found, otherwise empty.
     */
    Optional<Submission> findSubmissionById(Long submissionId);

    /**
     * Grades a specific submission.
     * Updates the grade, numericalGrade, feedback, and gradedDate fields.
     * Performs authorization check to ensure the grader is the teacher of the class.
     *
     * @param submissionId The ID of the submission to grade.
     * @param gradeDto DTO containing the grade score and feedback.
     * @param teacher The teacher User performing the grading.
     * @return The updated Submission entity.
     * @throws RuntimeException if submission not found or teacher not authorized.
     */
    Submission gradeSubmission(Long submissionId, GradeDto gradeDto, User teacher);

    /**
     * Finds assignments taught by a specific teacher that have ungraded submissions.
     * Returns a map where the key is the Assignment and the value is a list of ungraded Submissions.
     *
     * @param teacher The teacher User entity.
     * @param limit The maximum number of assignments to return in the map.
     * @return A Map grouping ungraded submissions by assignment, limited by the specified count.
     */
    Map<Assignment, List<Submission>> findAssignmentsWithUngradedSubmissions(User teacher, int limit);

    /**
     * Finds the latest graded submissions for a specific student.
     *
     * @param student The student user.
     * @param limit The maximum number of submissions to return.
     * @return A List of the latest graded submissions, ordered by graded date descending.
     */
    List<Submission> findLatestGradedSubmissions(User student, int limit);
}
