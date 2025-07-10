package student_management_system.service;

import student_management_system.model.Assignment;
import student_management_system.model.SchoolClass; // Needed for auth checks
import student_management_system.model.Submission; // Needed for pending check
import student_management_system.model.User;
import student_management_system.repository.AssignmentRepository;
import student_management_system.repository.SchoolClassRepository; // Needed for createAssignment
import student_management_system.repository.SubmissionRepository; // Needed for pending check
import student_management_system.repository.UserRepository;
import student_management_system.web.dto.AssignmentDto;

import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException; // Use specific exception
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityNotFoundException; // Use specific exception

import java.io.IOException;
import java.nio.file.Files; // Import Files
import java.nio.file.Path; // Import Path
import java.nio.file.Paths; // Import Paths
import java.nio.file.StandardCopyOption; // Import StandardCopyOption
import java.time.LocalDateTime; // Import LocalDateTime
import java.util.Collections;
import java.util.Comparator; // Import Comparator
import java.util.List;
import java.util.Map; // Import Map
import java.util.Optional;
import java.util.Set; // Import Set
import java.util.UUID;
import java.util.stream.Collectors; // Import Collectors

/**
 * Implementation of the AssignmentService interface.
 * Includes logic for creating, finding, updating, and deleting assignments.
 * UPDATED: Corrected file path resolution in file storing logic.
 */
@Service
public class AssignmentServiceImpl implements AssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentServiceImpl.class);

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Value("${file.assignment-upload-dir}")
    private String uploadDir;

    @Override
    @Transactional
    public Assignment createAssignment(AssignmentDto assignmentDto, Long classId, User teacher) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found with ID: " + classId));
        if (!schoolClass.getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("User is not authorized to create assignments for this class.");
        }

        String uniqueFilename = null;
        String originalFilename = null;
        String storedFilePath = null;
        MultipartFile file = assignmentDto.getAttachmentFile();

        if (file != null && !file.isEmpty()) {
            try {
                originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
                uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;

                // **FIX APPLIED HERE**
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                Path filePath = uploadPath.resolve(uniqueFilename).normalize();

                if (!Files.exists(uploadPath)) { Files.createDirectories(uploadPath); }

                // This check should now work correctly
                if (!filePath.startsWith(uploadPath)) {
                     throw new RuntimeException("Cannot store file with relative path outside current directory " + originalFilename);
                }
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                storedFilePath = uniqueFilename;
                logger.info("Stored assignment attachment: {}", filePath);
            } catch (IOException e) {
                logger.error("Could not store assignment attachment file {}: {}", originalFilename, e.getMessage(), e);
                throw new RuntimeException("Could not store file " + (originalFilename != null ? originalFilename : "") + ". Please try again!", e);
            }
        }

        Assignment assignment = new Assignment();
        assignment.setTitle(assignmentDto.getTitle());
        assignment.setDescription(assignmentDto.getDescription());
        assignment.setDueDate(assignmentDto.getDueDate());
        assignment.setSchoolClass(schoolClass);
        if (storedFilePath != null) {
            assignment.setAttachmentPath(storedFilePath);
            assignment.setAttachmentOriginalFilename(originalFilename);
        }
        logger.info("Creating assignment '{}' for class ID {}", assignment.getTitle(), classId);
        return assignmentRepository.save(assignment);
    }

    @Override
    public List<Assignment> findAssignmentsByClass(SchoolClass schoolClass) {
        return assignmentRepository.findBySchoolClass(schoolClass);
    }

     @Override
    public List<Assignment> findAssignmentsByClassId(Long classId) {
        return assignmentRepository.findBySchoolClass_Id(classId);
    }

    @Override
    public Optional<Assignment> findAssignmentById(Long assignmentId) {
        return assignmentRepository.findById(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> findAssignmentsForStudent(User student) {
        User studentWithClasses = userRepository.findById(student.getId())
                                        .orElseThrow(() -> new RuntimeException("Student not found"));
        Set<SchoolClass> enrolledClasses = studentWithClasses.getEnrolledClasses();
        if (enrolledClasses == null || enrolledClasses.isEmpty()) {
            return Collections.emptyList();
        }
        return assignmentRepository.findBySchoolClassIn(enrolledClasses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> findPendingAssignmentsForStudent(User student, int limit) {
        logger.debug("Finding pending assignments for student: {}", student.getUsername());
        List<Assignment> allAssignments = findAssignmentsForStudent(student);
        if (allAssignments.isEmpty()) {
            logger.debug("No assignments found for student {}", student.getUsername());
            return Collections.emptyList();
        }
        logger.trace("Total assignments found for student {}: {}", student.getUsername(), allAssignments.size());
        List<Submission> submissions = submissionRepository.findByStudent(student);
        Set<Long> submittedAssignmentIds = submissions.stream()
                .map(sub -> sub.getAssignment().getId())
                .collect(Collectors.toSet());
        logger.trace("Student {} has submissions for assignment IDs: {}", student.getUsername(), submittedAssignmentIds);
        LocalDateTime now = LocalDateTime.now();
        List<Assignment> pendingAssignments = allAssignments.stream()
                .filter(assignment -> !submittedAssignmentIds.contains(assignment.getId()))
                .filter(assignment -> assignment.getDueDate() != null && assignment.getDueDate().isAfter(now))
                .sorted(Comparator.comparing(Assignment::getDueDate))
                .limit(limit)
                .collect(Collectors.toList());
        logger.debug("Found {} pending assignments for student {} after filtering and limiting to {}",
                     pendingAssignments.size(), student.getUsername(), limit);
        return pendingAssignments;
    }

     @Override
    @Transactional
    public Assignment updateAssignment(Long assignmentId, AssignmentDto dto, User teacher) {
        Assignment existingAssignment = findAssignmentById(assignmentId)
                 .orElseThrow(() -> new EntityNotFoundException("Assignment not found with ID: " + assignmentId));
        if (!existingAssignment.getSchoolClass().getTeacher().getId().equals(teacher.getId())) {
             throw new AccessDeniedException("You are not authorized to update this assignment.");
        }
        existingAssignment.setTitle(dto.getTitle());
        existingAssignment.setDescription(dto.getDescription());
        existingAssignment.setDueDate(dto.getDueDate());
        MultipartFile newFile = dto.getAttachmentFile();
        String oldFilePath = existingAssignment.getAttachmentPath();
        if (newFile != null && !newFile.isEmpty()) {
            if (oldFilePath != null && !oldFilePath.isBlank()) {
                deleteAttachmentFile(oldFilePath);
            }
            String originalFilename = StringUtils.cleanPath(newFile.getOriginalFilename());
            String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;

            // **FIX APPLIED HERE**
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(uniqueFilename).normalize();

            try {
                 if (!Files.exists(uploadPath)) { Files.createDirectories(uploadPath); }
                 // This check should now work correctly
                 if (!filePath.startsWith(uploadPath)) {
                    throw new RuntimeException("Cannot store file with relative path outside current directory " + originalFilename);
                 }
                 Files.copy(newFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                 existingAssignment.setAttachmentPath(uniqueFilename);
                 existingAssignment.setAttachmentOriginalFilename(originalFilename);
                 logger.info("Updated attachment for assignment ID {} to {}", assignmentId, uniqueFilename);
            } catch (IOException e) {
                 logger.error("Could not store new attachment file {} for assignment ID {}: {}", originalFilename, assignmentId, e.getMessage(), e);
                 throw new RuntimeException("Could not store new attachment file " + originalFilename + ". Please try again!", e);
            }
        }
        logger.info("Updating assignment ID {} by teacher '{}'", assignmentId, teacher.getUsername());
        return assignmentRepository.save(existingAssignment);
    }

    @Override
    @Transactional
    public void deleteAssignment(Long assignmentId, User teacher) {
        Assignment assignmentToDelete = findAssignmentById(assignmentId)
                 .orElseThrow(() -> new EntityNotFoundException("Assignment not found with ID: " + assignmentId));
        if (!assignmentToDelete.getSchoolClass().getTeacher().getId().equals(teacher.getId())) {
             throw new AccessDeniedException("You are not authorized to delete this assignment.");
        }
        String filePath = assignmentToDelete.getAttachmentPath();
        if (filePath != null && !filePath.isBlank()) {
             deleteAttachmentFile(filePath);
        }
        logger.warn("Deleting assignment ID: {}. WARNING: Associated submissions will also be deleted due to cascade settings.", assignmentId);
        assignmentRepository.deleteById(assignmentId);
    }

    private void deleteAttachmentFile(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        try {
            // **Ensure consistent path resolution for deletion too**
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename).normalize();

            if(Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Deleted attachment file: {}", filePath.toString());
            } else {
                 logger.warn("Attachment file not found for deletion: {}", filePath.toString());
            }
        } catch (IOException e) {
            logger.error("Error deleting attachment file: {}. Error: {}", filename, e.getMessage());
        }
    }
}
