package student_management_system.service;

// Model Imports
import student_management_system.model.Assignment;
import student_management_system.model.MakeupRequest; // Import MakeupRequest
import student_management_system.model.SchoolClass;
import student_management_system.model.Submission;
import student_management_system.model.User;

// Repository Imports
import student_management_system.repository.AssignmentRepository;
import student_management_system.repository.MakeupRequestRepository; // Import MakeupRequestRepository
import student_management_system.repository.SubmissionRepository;
import student_management_system.repository.UserRepository; // Import UserRepository

// DTO Imports
import student_management_system.web.dto.GradeDto;
import student_management_system.web.dto.SubmissionDto;

// Logging Imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Spring Imports
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

// JPA/Util Imports
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat; // Import DecimalFormat
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the SubmissionService interface.
 * Handles logic for creating, finding, grading, and managing makeup submissions.
 * UPDATED: Corrected logic for handling makeup submissions in createOrUpdateSubmission.
 * UPDATED: Corrected file path resolution in storeSubmissionFile.
 */
@Service
public class SubmissionServiceImpl implements SubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionServiceImpl.class);

    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private MakeupRequestRepository makeupRequestRepository;
    @Autowired private UserRepository userRepository; // Added UserRepository

    @Value("${file.submission-upload-dir}")
    private String uploadDir;

    /**
     * Creates a new submission or updates an existing one for makeup purposes.
     * Handles file uploads and authorization checks.
     */
    @Override
    @Transactional
    public Submission createOrUpdateSubmission(SubmissionDto submissionDto, Long assignmentId, User student) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment not found with ID: " + assignmentId));

        // Fetch student with enrolled classes eagerly to avoid lazy loading issues
        User studentWithClasses = userRepository.findById(student.getId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found: " + student.getUsername()));

        // Check enrollment
         boolean isEnrolled = studentWithClasses.getEnrolledClasses() != null &&
                             studentWithClasses.getEnrolledClasses().stream()
                                .anyMatch(c -> c.getId().equals(assignment.getSchoolClass().getId()));

        if (!isEnrolled) {
             logger.warn("Submission blocked: Student {} is not enrolled in class ID {}", student.getUsername(), assignment.getSchoolClass().getId());
             throw new AccessDeniedException("Student not enrolled in the class for this assignment.");
         }

        Optional<Submission> existingSubmissionOpt = submissionRepository.findByStudentAndAssignment(student, assignment);
        Submission submissionToSave;
        String action; // For logging

        // --- START REVISED LOGIC ---
        if (submissionDto.isMakeup()) {
            action = "Processing makeup";
            // Makeup Scenario: Must have an existing submission
            submissionToSave = existingSubmissionOpt
                .orElseThrow(() -> {
                    logger.error("Makeup submission requested for student {} assignment {}, but no original submission found.",
                                student.getUsername(), assignmentId);
                    return new IllegalStateException("Cannot submit makeup for an assignment that was not previously submitted.");
                });

            logger.info("Found existing submission ID {} for makeup by student {} for assignment {}",
                        submissionToSave.getId(), student.getUsername(), assignmentId);

            // Verify an approved makeup request exists for this specific submission
            Optional<MakeupRequest> makeupReqOpt = makeupRequestRepository.findBySubmissionAndStatus(
                    submissionToSave, MakeupRequest.MakeupRequestStatus.APPROVED);
            if (makeupReqOpt.isEmpty()) {
                 logger.warn("Makeup submission blocked for student {} assignment {}: No approved makeup request found.",
                             student.getUsername(), assignmentId);
                 throw new AccessDeniedException("No approved makeup request found for this submission.");
             }

             // Reset grading fields, update submission date, mark as makeup
             submissionToSave.setGrade(null);
             submissionToSave.setNumericalGrade(null);
             submissionToSave.setFeedback(null);
             submissionToSave.setGradedDate(null);
             submissionToSave.setSubmissionDate(LocalDateTime.now()); // Update submission date to now
             submissionToSave.setMakeupSubmission(true); // Mark as makeup
             submissionToSave.setSuperseded(false); // This is now the active submission

             // Handle potential file change - delete old file if new one is uploaded
             String oldFilePath = submissionToSave.getFilePath();
             MultipartFile newFile = submissionDto.getSubmissionFile();
             if (newFile != null && !newFile.isEmpty()) {
                 if (StringUtils.hasText(oldFilePath)) {
                     deleteSubmissionFile(oldFilePath); // Delete the old file
                 }
                 // Save the new file
                 String[] fileDetails = storeSubmissionFile(newFile, student, assignment);
                 submissionToSave.setFilePath(fileDetails[0]); // uniqueFilename
                 submissionToSave.setOriginalFilename(fileDetails[1]); // originalFilename
             } else {
                // If no new file is uploaded during makeup, clear the old file path if it exists
                 if (StringUtils.hasText(oldFilePath)) {
                    deleteSubmissionFile(oldFilePath);
                    submissionToSave.setFilePath(null);
                    submissionToSave.setOriginalFilename(null);
                 }
             }
             // Update text content
             submissionToSave.setContentText(submissionDto.getContentText());

        } else {
            // Normal Submission Scenario: Check if already submitted
            action = "Creating new";
             if (existingSubmissionOpt.isPresent()) {
                 // Prevent normal submission if one (even a makeup one) already exists
                 logger.warn("Submission blocked for student {} assignment {}: Already submitted.", student.getUsername(), assignmentId);
                 throw new IllegalStateException("You have already submitted for this assignment.");
             }
            // Create a brand new submission
            submissionToSave = new Submission();
            submissionToSave.setStudent(student);
            submissionToSave.setAssignment(assignment);
            submissionToSave.setMakeupSubmission(false); // Not a makeup submission
            submissionToSave.setSuperseded(false);      // Not superseded

            // Store file if present
             MultipartFile file = submissionDto.getSubmissionFile();
             if (file != null && !file.isEmpty()) {
                 String[] fileDetails = storeSubmissionFile(file, student, assignment);
                 submissionToSave.setFilePath(fileDetails[0]); // uniqueFilename
                 submissionToSave.setOriginalFilename(fileDetails[1]); // originalFilename
             }
             // Set text content
             submissionToSave.setContentText(submissionDto.getContentText());
             // submissionDate is set automatically by default field initializer
        }
        // --- END REVISED LOGIC ---


        // Validate that either text or file is present before saving
         if ((submissionToSave.getContentText() == null || submissionToSave.getContentText().isBlank()) &&
             !StringUtils.hasText(submissionToSave.getFilePath())) {
             throw new IllegalArgumentException("Submission cannot be empty. Please provide text or upload a file.");
         }


        logger.info("{} submission for student {} on assignment ID {}", action, student.getUsername(), assignmentId);
        return submissionRepository.save(submissionToSave);
    }


    /**
     * Grades a submission, updating grade fields and timestamp.
     */
    @Override
    @Transactional
    public Submission gradeSubmission(Long submissionId, GradeDto gradeDto, User teacher) {
        Submission submission = findSubmissionById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found with ID: " + submissionId));
        Assignment assignment = submission.getAssignment();

        // Authorization Check
        if (assignment == null || assignment.getSchoolClass() == null || assignment.getSchoolClass().getTeacher() == null) {
             logger.error("Data integrity issue: Cannot verify teacher for submission ID {}", submissionId);
             throw new RuntimeException("Could not verify teacher for submission ID: " + submissionId);
        }
        if (!assignment.getSchoolClass().getTeacher().getId().equals(teacher.getId())) {
            logger.warn("Authorization Denied: Teacher {} attempted to grade submission ID {} for class owned by another teacher.",
                    teacher.getUsername(), submissionId);
            throw new AccessDeniedException("You are not authorized to grade submissions for this assignment.");
        }

        // Set the numerical grade directly from the DTO (which is Double)
        submission.setNumericalGrade(gradeDto.getGrade());

        // Set the string grade representation (e.g., "85.0", "100")
        DecimalFormat df = new DecimalFormat("#.##"); // Format to max 2 decimal places
        submission.setGrade(gradeDto.getGrade() != null ? df.format(gradeDto.getGrade()) : null);

        // Update feedback and graded date
        submission.setFeedback(gradeDto.getFeedback());
        submission.setGradedDate(LocalDateTime.now());

        logger.info("Grading submission ID {} by teacher {}. Grade: {}, NumericalGrade: {}",
                    submissionId, teacher.getUsername(), submission.getGrade(), submission.getNumericalGrade());
        return submissionRepository.save(submission);
    }

    // --- Find Methods ---
    @Override
    public Optional<Submission> findByStudentAndAssignment(User student, Assignment assignment) {
        return submissionRepository.findByStudentAndAssignment(student, assignment);
    }

    @Override
    public List<Submission> findSubmissionsByAssignment(Assignment assignment) {
        return submissionRepository.findByAssignment(assignment);
    }

    @Override
    public List<Submission> findSubmissionsByStudent(User student) {
        return submissionRepository.findByStudent(student);
    }

    @Override
    public Optional<Submission> findSubmissionById(Long submissionId) {
        return submissionRepository.findById(submissionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Assignment, List<Submission>> findAssignmentsWithUngradedSubmissions(User teacher, int limit) {
        logger.debug("Finding assignments with ungraded submissions for teacher: {}", teacher.getUsername());
        List<Submission> ungradedSubmissions = submissionRepository.findUngradedSubmissionsForTeacherAssignments(teacher.getId());
        logger.trace("Found {} total ungraded submissions for teacher {}", ungradedSubmissions.size(), teacher.getUsername());
        Map<Assignment, List<Submission>> groupedByAssignment = ungradedSubmissions.stream()
                .collect(Collectors.groupingBy(Submission::getAssignment));
        Map<Assignment, List<Submission>> limitedMap = groupedByAssignment.entrySet().stream()
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        logger.debug("Returning {} assignments with ungraded submissions for teacher {} (limit {})",
                     limitedMap.size(), teacher.getUsername(), limit);
        return limitedMap;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Submission> findLatestGradedSubmissions(User student, int limit) {
        logger.debug("Finding latest {} graded submissions for student {}", limit, student.getUsername());
        Pageable pageable = PageRequest.of(0, limit, Sort.by("gradedDate").descending());
        List<Submission> latestSubmissions = submissionRepository.findLatestGradedSubmissionsByStudent(student, pageable);
        logger.debug("Found {} latest graded submissions for student {}", latestSubmissions.size(), student.getUsername());
        return latestSubmissions;
    }

    // --- Helper Methods for File Handling ---

    private String[] storeSubmissionFile(MultipartFile file, User student, Assignment assignment) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot >= 0) {
            fileExtension = originalFilename.substring(lastDot);
        }
        String uniqueFilename = student.getId() + "_" + assignment.getId() + "_" + UUID.randomUUID().toString() + fileExtension;

        // **FIX APPLIED HERE**
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(uniqueFilename).normalize();

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created submission upload directory: {}", uploadPath);
            }
            // This check should now work correctly
            if (!filePath.startsWith(uploadPath)) {
                 throw new RuntimeException("Cannot store file with relative path outside current directory " + originalFilename);
            }
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Stored submission file '{}' for student {} assignment ID {}", filePath, student.getUsername(), assignment.getId());
            return new String[]{uniqueFilename, originalFilename};
        } catch (IOException e) {
            logger.error("Could not store submission file '{}' for student {} assignment {}: {}", originalFilename, student.getUsername(), assignment.getId(), e.getMessage(), e);
            throw new RuntimeException("Could not store submission file " + originalFilename + ". Please try again!", e);
        }
    }

    private void deleteSubmissionFile(String filename) {
        if (!StringUtils.hasText(filename)) {
            return; // No file to delete
        }
        try {
            // **Ensure consistent path resolution for deletion too**
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename).normalize();

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Deleted old submission file: {}", filePath.toString());
            } else {
                logger.warn("Old submission file not found for deletion: {}", filePath.toString());
            }
        } catch (IOException e) {
            logger.error("Error deleting old submission file: {}. Error: {}", filename, e.getMessage());
        } catch (Exception e) {
             logger.error("Unexpected error deleting old submission file '{}': {}", filename, e.getMessage(), e);
        }
    }
}
