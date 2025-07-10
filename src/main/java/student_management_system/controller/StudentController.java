package student_management_system.controller;

// --- Model Imports ---
import student_management_system.model.*; // Import all models

// --- Service Imports ---
import student_management_system.service.*; // Import all services

// --- DTO Import ---
import student_management_system.web.dto.*; // Import all DTOs

// --- Spring Framework Imports ---
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError; // Import FieldError
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// --- Java Util Imports ---
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.HashMap;
import java.util.Comparator;


// --- Logging Imports ---
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid; // Import Valid

/**
 * Controller handling requests for the Student role.
 * Includes endpoints for viewing classes, assignments, submissions, makeup requests, quizzes, grades,
 * and handling quiz submissions with detailed validation logging.
 * UPDATED: Refined makeup assignment logic to allow only one makeup attempt.
 */
@Controller
@RequestMapping("/student")
public class StudentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    // --- Service Injections ---
    @Autowired private ClassService classService;
    @Autowired private UserService userService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private SubmissionService submissionService;
    @Autowired private MakeupRequestService makeupRequestService;
    @Autowired private QuizService quizService;
    @Autowired private QuizAttemptService quizAttemptService;

    /**
     * Helper method to get a map containing submission and makeup status for each assignment.
     * Key: Assignment ID
     * Value: Map containing "submission" (Submission object or null) and "makeupStatus" (String: NONE, PENDING, APPROVED, REJECTED, MAKEUP_PASSED, MAKEUP_FAILED_FINAL)
     */
    private Map<Long, Map<String, Object>> getAssignmentStatusMapForStudent(User student, List<Assignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return Collections.emptyMap();
        }
        // Fetch all submissions by the student at once
        Map<Long, Submission> submissionMap = submissionService.findSubmissionsByStudent(student)
                .stream()
                .filter(sub -> sub.getAssignment() != null)
                .collect(Collectors.toMap(sub -> sub.getAssignment().getId(), Function.identity(), (s1, s2) -> {
                    // If multiple submissions (e.g. original and makeup), prefer the one not superseded, or the makeup one.
                    if (s1.isMakeupSubmission() && !s1.isSuperseded()) return s1;
                    if (s2.isMakeupSubmission() && !s2.isSuperseded()) return s2;
                    if (!s1.isSuperseded()) return s1;
                    return s2; // Default or if both superseded (should not happen with current logic)
                }));

        // Fetch all makeup requests by the student at once
        Map<Long, MakeupRequest> requestMap = makeupRequestService.findRequestsByStudent(student)
                .stream()
                .filter(req -> req.getSubmission() != null) // Ensure request is linked to a submission
                .collect(Collectors.toMap(req -> req.getSubmission().getId(), Function.identity(), (r1, r2) -> r1)); // Prefer first if multiple (should be one per submission)


        Map<Long, Map<String, Object>> statusMap = new HashMap<>();
        for (Assignment assignment : assignments) {
            Map<String, Object> details = new HashMap<>();
            Submission submission = submissionMap.get(assignment.getId());
            details.put("submission", submission);
            String makeupStatus = "NONE";

            if (submission != null) {
                if (submission.isMakeupSubmission()) { // This IS a makeup submission
                    if (submission.getNumericalGrade() != null) {
                        makeupStatus = submission.getNumericalGrade() >= 60.0 ? "MAKEUP_PASSED" : "MAKEUP_FAILED_FINAL";
                    } else {
                        makeupStatus = "MAKEUP_SUBMITTED_PENDING_GRADE"; // Makeup submitted, not yet graded
                    }
                } else { // This is an ORIGINAL submission
                    MakeupRequest request = requestMap.get(submission.getId());
                    if (request != null) {
                        makeupStatus = request.getStatus().name(); // PENDING, APPROVED, REJECTED
                    }
                }
            }
            details.put("makeupStatus", makeupStatus);
            statusMap.put(assignment.getId(), details);
        }
        return statusMap;
    }

    // ========================================================================
    // Class and Assignment Listing Endpoints
    // ========================================================================
    @GetMapping("/assignments")
    public String listAllAssignments(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User student = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        List<Assignment> assignments = assignmentService.findAssignmentsForStudent(student);
        Map<Long, Map<String, Object>> assignmentStatusMap = getAssignmentStatusMapForStudent(student, assignments);
        model.addAttribute("assignments", assignments);
        model.addAttribute("assignmentStatusMap", assignmentStatusMap);
        return "student/assignments";
    }

    @GetMapping("/assignments/class/{classId}")
    public String listAssignmentsForClass(@PathVariable Long classId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
         User student = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        SchoolClass schoolClass = classService.findClassById(classId).orElseThrow(() -> new EntityNotFoundException("Class not found: " + classId));
         Set<SchoolClass> enrolledClasses = classService.findClassesByStudent(student);
         if (!enrolledClasses.contains(schoolClass)) {
             logger.warn("Access Denied: Student {} attempted to view assignments for non-enrolled class {}", student.getUsername(), classId);
             throw new AccessDeniedException("Not enrolled in this class.");
         }
        List<Assignment> assignments = assignmentService.findAssignmentsByClassId(classId);
        Map<Long, Map<String, Object>> assignmentStatusMap = getAssignmentStatusMapForStudent(student, assignments);
        model.addAttribute("assignments", assignments);
        model.addAttribute("filterClassName", schoolClass.getName());
        model.addAttribute("assignmentStatusMap", assignmentStatusMap);
        return "student/assignments";
    }

    @GetMapping("/classes")
    public String listEnrolledClasses(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User student = userService.findUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated student not found in database"));
        Set<SchoolClass> enrolledClasses = classService.findClassesByStudent(student);
        model.addAttribute("enrolledClasses", enrolledClasses);
        return "student/classes";
    }
    @GetMapping("/classes/join")
    public String showJoinClassForm(Model model) {
        return "student/join-class";
    }
    @PostMapping("/classes/join")
    public String processJoinClass(@RequestParam String classCode, @RequestParam(required = false) String joinPassword, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            User student = userService.findUserByUsername(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("Authenticated student not found in database"));
            SchoolClass joinedClass = classService.enrollStudent(student, classCode.trim().toUpperCase(), joinPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Successfully joined class: " + joinedClass.getName());
            return "redirect:/student/classes";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not join class: " + e.getMessage());
            return "redirect:/student/classes/join";
        }
    }
     @GetMapping("/classes/details/{classId}")
    public String showClassDetailsForStudent(@PathVariable Long classId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        SchoolClass schoolClass = classService.findClassById(classId).orElseThrow(() -> new RuntimeException("Class not found: " + classId));
        User student = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        Set<SchoolClass> enrolledClasses = classService.findClassesByStudent(student);
         if (!enrolledClasses.contains(schoolClass)) { throw new AccessDeniedException("Not enrolled in this class."); }
        model.addAttribute("schoolClass", schoolClass);
        return "student/class-details"; // Chat view
    }

    // ========================================================================
    // Submission Endpoints
    // ========================================================================

    @GetMapping("/assignments/{assignmentId}/submit")
    public String showSubmissionForm(@PathVariable Long assignmentId, Model model, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        String username = userDetails.getUsername();
        logger.debug("Student {} viewing submission page for assignment ID {}", username, assignmentId);

        try {
            User student = userService.findUserByUsername(username).orElseThrow(() -> new EntityNotFoundException("Student not found: " + username));
            Assignment assignment = assignmentService.findAssignmentById(assignmentId)
                    .orElseThrow(() -> new EntityNotFoundException("Assignment not found with ID: " + assignmentId));

            SchoolClass assignmentClass = assignment.getSchoolClass();
            if (assignmentClass == null) {
                throw new IllegalStateException("Assignment " + assignmentId + " is not linked to a class.");
            }
            Set<SchoolClass> enrolledClasses = classService.findClassesByStudent(student);
            if (enrolledClasses == null || enrolledClasses.stream().noneMatch(c -> c.getId().equals(assignmentClass.getId()))) {
                logger.warn("Access Denied: Student {} is not enrolled in class ID {} for assignment {}", username, assignmentClass.getId(), assignmentId);
                throw new AccessDeniedException("You are not enrolled in the class for this assignment.");
            }

            Optional<Submission> existingSubmissionOpt = submissionService.findByStudentAndAssignment(student, assignment);
            boolean alreadySubmitted = existingSubmissionOpt.isPresent();
            boolean makeupResubmissionAllowed = false;
            String makeupStatus = "NONE"; // Possible values: NONE, PENDING, APPROVED, REJECTED, MAKEUP_PASSED, MAKEUP_FAILED_FINAL, MAKEUP_SUBMITTED_PENDING_GRADE
            SubmissionDto submissionDto = new SubmissionDto();

            if (alreadySubmitted) {
                Submission currentSubmission = existingSubmissionOpt.get();
                model.addAttribute("submission", currentSubmission);

                if (currentSubmission.isMakeupSubmission()) {
                    // This is a makeup submission that has been graded or is pending grade
                    makeupResubmissionAllowed = false; // No more makeups on a makeup
                    if (currentSubmission.getNumericalGrade() != null) {
                        makeupStatus = currentSubmission.getNumericalGrade() >= 60.0 ? "MAKEUP_PASSED" : "MAKEUP_FAILED_FINAL";
                    } else {
                        makeupStatus = "MAKEUP_SUBMITTED_PENDING_GRADE";
                    }
                    logger.info("Student {} has a makeup submission for assignment {}. Status: {}", username, assignmentId, makeupStatus);
                } else {
                    // This is an original submission, check for makeup request status
                    Optional<MakeupRequest> makeupReqOpt = makeupRequestService.findRequestBySubmission(currentSubmission);
                    if (makeupReqOpt.isPresent()) {
                        MakeupRequest request = makeupReqOpt.get();
                        makeupStatus = request.getStatus().name(); // PENDING, APPROVED, REJECTED
                        if (request.getStatus() == MakeupRequest.MakeupRequestStatus.APPROVED) {
                            // Allow makeup resubmission only if the original was graded (student has feedback)
                            if (currentSubmission.getGrade() != null) {
                                makeupResubmissionAllowed = true;
                                submissionDto.setContentText(currentSubmission.getContentText()); // Pre-populate with original for convenience
                                logger.info("Makeup resubmission allowed for student {} on assignment {}. Original submission was graded.", student.getUsername(), assignmentId);
                            } else {
                                logger.info("Makeup request APPROVED for student {} on assignment {}, but original not graded. Resubmission form not shown yet.", student.getUsername(), assignmentId);
                            }
                        }
                    }
                }
            }

            model.addAttribute("assignment", assignment);
            model.addAttribute("alreadySubmitted", alreadySubmitted);
            model.addAttribute("makeupResubmissionAllowed", makeupResubmissionAllowed);
            model.addAttribute("makeupStatus", makeupStatus);

            if (!alreadySubmitted || makeupResubmissionAllowed) {
                model.addAttribute("submissionDto", submissionDto);
                logger.debug("Displaying submission form for assignment {}. Already submitted: {}, Makeup allowed: {}", assignmentId, alreadySubmitted, makeupResubmissionAllowed);
            } else {
                 logger.debug("Displaying existing submission details for assignment {}. Already submitted: {}, Makeup allowed: {}", assignmentId, alreadySubmitted, makeupResubmissionAllowed);
            }

            return "student/submit-assignment";

        } catch (EntityNotFoundException | AccessDeniedException e) {
             logger.warn("Error loading submission page for assignment {}: {}", assignmentId, e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
             return "redirect:/student/assignments";
        } catch (Exception e) {
             logger.error("Unexpected error loading submission page for assignment {}: {}", assignmentId, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
             return "redirect:/student/assignments";
        }
    }


    @PostMapping("/assignments/{assignmentId}/submit")
    public String processSubmission(
            @PathVariable Long assignmentId,
            @ModelAttribute("submissionDto") SubmissionDto submissionDto,
            BindingResult result,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (userDetails == null) return "redirect:/login";
        String username = userDetails.getUsername();

        // Determine if this submission is a makeup based on approved request for an existing original submission
        boolean isMakeupSubmissionFlow = false;
        try {
            User student = userService.findUserByUsername(username).orElseThrow();
            Assignment assignment = assignmentService.findAssignmentById(assignmentId).orElseThrow();
            Optional<Submission> existingSubOpt = submissionService.findByStudentAndAssignment(student, assignment);
            if (existingSubOpt.isPresent() && !existingSubOpt.get().isMakeupSubmission()) { // Only consider original submissions for makeup flow
                Optional<MakeupRequest> makeupReqOpt = makeupRequestService.findApprovedRequestForSubmission(existingSubOpt.get());
                if (makeupReqOpt.isPresent() && existingSubOpt.get().getGrade() != null) { // Original must be graded
                    isMakeupSubmissionFlow = true;
                    submissionDto.setMakeup(true); // Set the DTO flag
                    logger.info("Server-side check: This is a makeup submission flow for student {} assignment {}", username, assignmentId);
                }
            }
        } catch (Exception e) {
            logger.error("Error during server-side makeup check for student {} assignment {}: {}", username, assignmentId, e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", "An error occurred checking submission status. Please try again.");
             return "redirect:/student/assignments/" + assignmentId + "/submit";
        }

        logger.info("Processing submission from student {} for assignment ID {}. Is Makeup Flow: {}", username, assignmentId, isMakeupSubmissionFlow);


        boolean fileProvided = submissionDto.getSubmissionFile() != null && !submissionDto.getSubmissionFile().isEmpty();
        boolean textProvided = submissionDto.getContentText() != null && !submissionDto.getContentText().isBlank();
        if (!fileProvided && !textProvided) {
             result.reject("NotEmpty.submission", "Submission cannot be empty. Please provide text or upload a file.");
             logger.warn("Validation failed for submission by {}: Both text and file are empty.", username);
        }

         if (result.hasErrors()) {
             logger.warn("Validation errors processing submission for assignment {}: {}", assignmentId, result.getAllErrors());
             try {
                 Assignment assignment = assignmentService.findAssignmentById(assignmentId)
                        .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));
                 model.addAttribute("assignment", assignment);
                 // Repopulate other necessary model attributes as in the GET handler
                 User student = userService.findUserByUsername(username).orElseThrow();
                 Optional<Submission> existingSubOpt = submissionService.findByStudentAndAssignment(student, assignment);
                 String makeupStatus = "NONE";
                 boolean currentMakeupResubmissionAllowed = false;

                 if (existingSubOpt.isPresent()) {
                     Submission currentSubmission = existingSubOpt.get();
                     model.addAttribute("submission", currentSubmission);
                     if (currentSubmission.isMakeupSubmission()) {
                         makeupStatus = currentSubmission.getNumericalGrade() != null ? (currentSubmission.getNumericalGrade() >= 60.0 ? "MAKEUP_PASSED" : "MAKEUP_FAILED_FINAL") : "MAKEUP_SUBMITTED_PENDING_GRADE";
                     } else {
                         Optional<MakeupRequest> reqOpt = makeupRequestService.findRequestBySubmission(currentSubmission);
                         if (reqOpt.isPresent()) {
                             makeupStatus = reqOpt.get().getStatus().name();
                             if (reqOpt.get().getStatus() == MakeupRequest.MakeupRequestStatus.APPROVED && currentSubmission.getGrade() != null) {
                                 currentMakeupResubmissionAllowed = true;
                             }
                         }
                     }
                 }
                 model.addAttribute("makeupResubmissionAllowed", currentMakeupResubmissionAllowed);
                 model.addAttribute("alreadySubmitted", existingSubOpt.isPresent());
                 model.addAttribute("makeupStatus", makeupStatus);
                 model.addAttribute("submissionDto", submissionDto); // Pass back DTO with errors
                 return "student/submit-assignment";
             } catch (Exception e) {
                  logger.error("Error repopulating submission form after validation failure for assignment {}: {}", assignmentId, e.getMessage());
                  redirectAttributes.addFlashAttribute("errorMessage", "An error occurred displaying the submission form.");
                  return "redirect:/student/assignments";
             }
         }

        try {
            User student = userService.findUserByUsername(username).orElseThrow();
            submissionService.createOrUpdateSubmission(submissionDto, assignmentId, student); // DTO's isMakeup flag is now correctly set
            redirectAttributes.addFlashAttribute("successMessage",
                submissionDto.isMakeup() ? "Makeup assignment submitted successfully!" : "Assignment submitted successfully!");
            logger.info("Submission successful for student {} assignment {}", username, assignmentId);
            return "redirect:/student/assignments";

        } catch (EntityNotFoundException | AccessDeniedException | IllegalStateException | IllegalArgumentException e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Submission failed: " + e.getMessage());
             logger.warn("Submission failed for student {} assignment {}: {}", username, assignmentId, e.getMessage());
             return "redirect:/student/assignments/" + assignmentId + "/submit";
        } catch (RuntimeException e) { // Catch broader runtime exceptions from service
            redirectAttributes.addFlashAttribute("errorMessage", "Submission failed: " + e.getMessage());
             logger.error("Runtime exception during submission for student {} assignment {}: {}", username, assignmentId, e.getMessage(), e);
             return "redirect:/student/assignments/" + assignmentId + "/submit";
        } catch (Exception e) { // Catch any other unexpected errors
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during submission.");
            logger.error("Unexpected error during submission for student {} assignment {}: {}", username, assignmentId, e.getMessage(), e);
            return "redirect:/student/assignments/" + assignmentId + "/submit";
        }
    }

    // ========================================================================
    // Makeup Request Endpoints
    // ========================================================================
    @GetMapping("/makeup/request/{submissionId}")
    public String showMakeupRequestForm(@PathVariable Long submissionId, Model model, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        String username = userDetails.getUsername();
        try {
            User student = userService.findUserByUsername(username).orElseThrow(() -> new RuntimeException("Authenticated student not found"));
            Submission submission = submissionService.findSubmissionById(submissionId).orElseThrow(() -> new EntityNotFoundException("Submission not found with ID: " + submissionId));

            // Authorization: Ensure the student owns the submission
            if (!submission.getStudent().getId().equals(student.getId())) {
                logger.warn("Access Denied: Student {} attempted to access makeup form for submission ID {} owned by another student.", username, submissionId);
                throw new AccessDeniedException("You can only request makeup for your own submissions.");
            }

            // Prevent requesting makeup for an already makeup submission
            if (submission.isMakeupSubmission()) {
                logger.warn("Makeup request denied for submission {}: It is already a makeup submission.", submissionId);
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot request makeup for an assignment that was already a makeup attempt.");
                return "redirect:/student/assignments/" + submission.getAssignment().getId() + "/submit";
            }

            model.addAttribute("submission", submission);
            return "student/makeup-request-form";
        } catch (EntityNotFoundException e) {
            logger.warn("Makeup request form failed for student {}: {}", username, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/dashboard"; // Or a more appropriate error page or back
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/dashboard";
        } catch (Exception e) {
            logger.error("Error showing makeup request form for submission {}: {}", submissionId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while loading the makeup request page.");
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/makeup/request/{submissionId}")
    public String applyForMakeup(@PathVariable Long submissionId, @RequestParam("reason") String reason, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        String username = userDetails.getUsername();
        logger.info("Student {} submitting makeup request for submission ID: {}", username, submissionId);

        if (reason == null || reason.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "A reason must be provided for the makeup request.");
            return "redirect:/student/makeup/request/" + submissionId;
        }

        try {
            User student = userService.findUserByUsername(username).orElseThrow(() -> new RuntimeException("Authenticated student not found: " + username));
            // The createMakeupRequest service method already contains validation logic
            makeupRequestService.createMakeupRequest(submissionId, student, reason.trim());
            redirectAttributes.addFlashAttribute("successMessage", "Makeup request submitted successfully. Your teacher will review it.");
            logger.info("Makeup request successful for submission ID {} by student {}", submissionId, username);
            // Redirect to the specific assignment submission page to see the pending status
            Submission submission = submissionService.findSubmissionById(submissionId).orElse(null);
            if (submission != null && submission.getAssignment() != null) {
                return "redirect:/student/assignments/" + submission.getAssignment().getId() + "/submit";
            }
            return "redirect:/dashboard"; // Fallback
        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException e) {
            logger.warn("Makeup request failed for submission ID {} by student {}: {}", submissionId, username, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Could not submit makeup request: " + e.getMessage());
            return "redirect:/student/makeup/request/" + submissionId;
        } catch (Exception e) {
            logger.error("Unexpected error submitting makeup request for submission ID {} by student {}: {}", submissionId, username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while submitting the request.");
            return "redirect:/student/makeup/request/" + submissionId;
        }
    }

    // ========================================================================
    // Quiz Endpoints for Students
    // ========================================================================
    @GetMapping("/quizzes")
    public String listAllQuizzes(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User student = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        Set<SchoolClass> enrolledClasses = classService.findClassesByStudent(student);
        List<Quiz> availableQuizzes = enrolledClasses.stream()
                .flatMap(sc -> quizService.findQuizzesByClassId(sc.getId()).stream())
                .collect(Collectors.toList());
        Map<Long, QuizAttempt> attemptMap = quizAttemptService.findAttemptsByStudent(student).stream()
                .filter(att -> att.getQuiz() != null)
                .collect(Collectors.toMap(att -> att.getQuiz().getId(), Function.identity(), (e1, e2) -> e1));
        model.addAttribute("quizzes", availableQuizzes);
        model.addAttribute("attemptMap", attemptMap);
        model.addAttribute("filterClassName", "All Classes");
        return "student/quiz-list";
    }

    @GetMapping("/quizzes/class/{classId}")
    public String listQuizzesForClass(@PathVariable Long classId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User student = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        SchoolClass schoolClass = classService.findClassById(classId).orElseThrow(() -> new EntityNotFoundException("Class not found"));
        if (!classService.findClassesByStudent(student).contains(schoolClass)) { throw new AccessDeniedException("Not enrolled in this class."); }
        List<Quiz> quizzes = quizService.findQuizzesByClassId(classId);
        Map<Long, QuizAttempt> attemptMap = quizAttemptService.findAttemptsByStudent(student).stream()
                .filter(att -> att.getQuiz() != null && att.getQuiz().getSchoolClass().getId().equals(classId))
                .collect(Collectors.toMap(att -> att.getQuiz().getId(), Function.identity(), (e1, e2) -> e1));
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("attemptMap", attemptMap);
        model.addAttribute("filterClassName", schoolClass.getName());
        model.addAttribute("classId", classId);
        return "student/quiz-list";
    }

    @GetMapping("/quizzes/{quizId}/take")
    public String takeQuiz(@PathVariable Long quizId, Model model, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        User student = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        try {
            Quiz quiz = quizService.getQuizForTaking(quizId, student);
            QuizAttempt attempt = quizAttemptService.startQuizAttempt(quiz, student);
            long attemptStartTimeMillis = 0;
            Long quizEndTimeMillis = null;
            if (attempt.getStartTime() != null) {
                attemptStartTimeMillis = attempt.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } else {
                 logger.warn("Quiz attempt ID {} has a null start time!", attempt.getId());
                 attemptStartTimeMillis = Instant.now().toEpochMilli();
            }
            if (quiz.getTimeLimitMinutes() != null && attempt.getStartTime() != null) {
                quizEndTimeMillis = attempt.getStartTime()
                                           .plusMinutes(quiz.getTimeLimitMinutes())
                                           .atZone(ZoneId.systemDefault())
                                           .toInstant()
                                           .toEpochMilli();
            }
            QuizSubmissionDto submissionDto = new QuizSubmissionDto();
            submissionDto.setQuizId(quizId);
            quiz.getQuestions().forEach(q -> submissionDto.getAnswers().add(new QuizAnswerDto(q.getId(), (String) null)));
            model.addAttribute("quiz", quiz);
            model.addAttribute("attempt", attempt);
            model.addAttribute("quizSubmissionDto", submissionDto);
            model.addAttribute("attemptStartTimeMillis", attemptStartTimeMillis);
            model.addAttribute("quizEndTimeMillis", quizEndTimeMillis);
            return "student/quiz-take";
        } catch (EntityNotFoundException | AccessDeniedException | IllegalStateException e) {
            logger.warn("Student {} cannot take quiz ID {}: {}", student.getUsername(), quizId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot take quiz: " + e.getMessage());
            return "redirect:/student/quizzes";
        } catch (Exception e) {
            logger.error("Error preparing quiz ID {} for student {}: {}", quizId, student.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
            return "redirect:/student/quizzes";
        }
    }

    @PostMapping("/quizzes/submit/{attemptId}")
    public String submitQuizAttempt(@PathVariable Long attemptId,
                                    @Valid @ModelAttribute("quizSubmissionDto") QuizSubmissionDto submissionDto,
                                    BindingResult result,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        User student = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        logger.info("Student {} submitting attempt ID {}", student.getUsername(), attemptId);
        if (result.hasErrors()) {
            logger.warn("Validation errors submitting attempt ID {}:", attemptId);
            for (FieldError error : result.getFieldErrors()) {
                logger.warn("Field '{}': Rejected value [{}]; Message: {}",
                        error.getField(), error.getRejectedValue(), error.getDefaultMessage());
            }
            QuizAttempt attemptForForm = quizAttemptService.findAttemptByIdForUser(attemptId, student).orElse(null);
            if (attemptForForm != null) {
                model.addAttribute("quiz", attemptForForm.getQuiz());
                model.addAttribute("attempt", attemptForForm);
                 long attemptStartTimeMillis = 0;
                 Long quizEndTimeMillis = null;
                 if (attemptForForm.getStartTime() != null) {
                    attemptStartTimeMillis = attemptForForm.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    if (attemptForForm.getQuiz().getTimeLimitMinutes() != null) {
                         quizEndTimeMillis = attemptForForm.getStartTime()
                                                    .plusMinutes(attemptForForm.getQuiz().getTimeLimitMinutes())
                                                    .atZone(ZoneId.systemDefault())
                                                    .toInstant()
                                                    .toEpochMilli();
                    }
                 }
                 model.addAttribute("attemptStartTimeMillis", attemptStartTimeMillis);
                 model.addAttribute("quizEndTimeMillis", quizEndTimeMillis);
                 model.addAttribute("quizSubmissionDto", submissionDto);
                return "student/quiz-take";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Quiz attempt not found or invalid.");
                return "redirect:/student/quizzes";
            }
        }
        try {
            QuizAttempt completedAttempt = quizAttemptService.submitQuizAttempt(attemptId, submissionDto, student);
            redirectAttributes.addFlashAttribute("successMessage", "Quiz submitted successfully!");
            return "redirect:/student/quizzes/result/" + completedAttempt.getId();
        } catch (EntityNotFoundException | AccessDeniedException | IllegalStateException e) {
            logger.warn("Failed to submit attempt ID {} for student {}: {}", attemptId, student.getUsername(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Submission failed: " + e.getMessage());
            return "redirect:/student/quizzes/" + submissionDto.getQuizId() + "/take";
        } catch (Exception e) {
            logger.error("Unexpected error submitting attempt ID {} for student {}: {}", attemptId, student.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during submission.");
            return "redirect:/student/quizzes";
        }
    }

    @GetMapping("/quizzes/result/{attemptId}")
    public String viewQuizResult(@PathVariable Long attemptId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User student = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        logger.debug("Student {} viewing result for attempt ID {}", student.getUsername(), attemptId);
        try {
            QuizAttempt attempt = quizAttemptService.findAttemptByIdForUser(attemptId, student).orElseThrow(() -> new EntityNotFoundException("Quiz attempt not found or not authorized."));
            if (attempt.getStatus() == QuizAttempt.AttemptStatus.IN_PROGRESS) { logger.warn("Student {} tried to view results for attempt ID {} which is still in progress.", student.getUsername(), attemptId); throw new IllegalStateException("Quiz attempt has not been submitted yet."); }
            QuizAttemptDto resultDto = quizAttemptService.getAttemptResultDto(attempt);
            model.addAttribute("attemptResult", resultDto);
            model.addAttribute("quiz", attempt.getQuiz());
            return "student/quiz-result";
        } catch (EntityNotFoundException | AccessDeniedException | IllegalStateException e) { logger.warn("Failed to view result for attempt ID {} by student {}: {}", attemptId, student.getUsername(), e.getMessage()); model.addAttribute("errorMessage", "Could not view result: " + e.getMessage()); return "redirect:/student/quizzes"; }
        catch (Exception e) { logger.error("Error viewing result for attempt ID {} by student {}: {}", attemptId, student.getUsername(), e.getMessage(), e); model.addAttribute("errorMessage", "An unexpected error occurred."); return "redirect:/student/quizzes"; }
    }

    // ========================================================================
    // Student Grades View Endpoint
    // ========================================================================
    @GetMapping("/classes/{classId}/grades")
    public String showStudentGradesForClass(@PathVariable Long classId, Model model, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        String username = userDetails.getUsername();
        logger.debug("Student {} viewing grades for class ID {}", username, classId);

        try {
            User student = userService.findUserByUsername(username).orElseThrow();
            SchoolClass schoolClass = classService.findClassById(classId)
                    .orElseThrow(() -> new EntityNotFoundException("Class not found: " + classId));

            Set<SchoolClass> enrolledClasses = classService.findClassesByStudent(student);
            if (!enrolledClasses.contains(schoolClass)) {
                logger.warn("Access Denied: Student {} attempted to view grades for non-enrolled class {}", username, classId);
                throw new AccessDeniedException("Not enrolled in this class.");
            }

            List<Assignment> assignments = assignmentService.findAssignmentsByClassId(classId)
                                            .stream().sorted(Comparator.comparing(Assignment::getDueDate))
                                            .collect(Collectors.toList());
            List<Quiz> quizzes = quizService.findQuizzesByClassId(classId)
                                            .stream().sorted(Comparator.comparing(Quiz::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                                            .collect(Collectors.toList());

            Map<Long, Submission> submissionMap = submissionService.findSubmissionsByStudent(student).stream()
                    .filter(s -> s.getAssignment() != null && s.getAssignment().getSchoolClass().getId().equals(classId))
                    .collect(Collectors.toMap(s -> s.getAssignment().getId(), Function.identity()));

            Map<Long, QuizAttempt> attemptMap = quizAttemptService.findAttemptsByStudent(student).stream()
                    .filter(a -> a.getQuiz() != null && a.getQuiz().getSchoolClass().getId().equals(classId))
                    .collect(Collectors.toMap(a -> a.getQuiz().getId(), Function.identity()));

            model.addAttribute("schoolClass", schoolClass);
            model.addAttribute("assignments", assignments);
            model.addAttribute("quizzes", quizzes);
            model.addAttribute("submissionMap", submissionMap);
            model.addAttribute("attemptMap", attemptMap);

            return "student/class-grades";

        } catch (EntityNotFoundException | AccessDeniedException e) {
            logger.warn("Error accessing grades for class {}: {}", classId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/classes";
        } catch (Exception e) {
            logger.error("Unexpected error loading grades for class {}: {}", classId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while loading grades.");
            return "redirect:/student/classes";
        }
    }

}
