package student_management_system.controller; // Correct package

// --- Model Imports ---
import student_management_system.model.*;

// --- Service Imports ---
import student_management_system.service.*;

// --- Repository Imports ---
import student_management_system.repository.QuizRepository;
import student_management_system.repository.QuizAttemptRepository;

// --- DTO Imports ---
import student_management_system.web.dto.*;

// --- Spring Framework Imports ---
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// --- Java Util Imports ---
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.Comparator; // Import Comparator


@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private static final Logger logger = LoggerFactory.getLogger(TeacherController.class);

    // --- Service Injections ---
    @Autowired private ClassService classService;
    @Autowired private UserService userService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private SubmissionService submissionService;
    @Autowired private MakeupRequestService makeupRequestService;
    @Autowired private QuizService quizService;
    @Autowired private QuizAttemptService quizAttemptService;
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;

    // ========================================================================
    // Class Management Endpoints
    // ========================================================================
    @GetMapping("/classes")
    public String listClasses(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        List<SchoolClass> classes = classService.findClassesByTeacher(teacher);
        model.addAttribute("classes", classes);
        return "teacher/classes";
    }
    @GetMapping("/classes/new")
    public String showCreateClassForm(Model model) {
        model.addAttribute("classDto", new ClassDto());
        return "teacher/class-form";
    }
    @PostMapping("/classes/create")
    public String createClass(@ModelAttribute("classDto") ClassDto classDto,
                              BindingResult result,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("classDto", classDto);
            return "teacher/class-form";
        }
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            classService.createClass(classDto, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "Class created successfully!");
            return "redirect:/teacher/classes";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating class: " + e.getMessage());
            return "redirect:/teacher/classes/new";
        }
    }
    @GetMapping("/classes/edit/{classId}")
    public String showEditClassForm(@PathVariable Long classId,
                                    Model model,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            SchoolClass schoolClass = classService.findClassById(classId)
                    .orElseThrow(() -> new EntityNotFoundException("Class not found"));
            if (!schoolClass.getTeacher().getId().equals(teacher.getId())) {
                throw new AccessDeniedException("Not authorized.");
            }
            ClassDto classDto = new ClassDto(schoolClass.getName(), schoolClass.getDescription());
            // Pre-populate password field ONLY if you intend to show/allow editing it
            // classDto.setJoinPassword(schoolClass.getJoinPassword()); // Be careful with security implications
            model.addAttribute("classDto", classDto);
            model.addAttribute("classId", classId);
            model.addAttribute("existingImagePath", schoolClass.getClassImagePath());
            return "teacher/class-edit-form";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/classes";
        }
    }
    @PostMapping("/classes/update/{classId}")
    public String updateClass(@PathVariable Long classId,
                              @ModelAttribute("classDto") ClassDto classDto,
                              BindingResult result,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("classDto", classDto);
            model.addAttribute("classId", classId);
            classService.findClassById(classId).ifPresent(sc -> model.addAttribute("existingImagePath", sc.getClassImagePath()));
            return "teacher/class-edit-form";
        }
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            classService.updateClass(classId, classDto, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "Class updated successfully!");
            return "redirect:/teacher/classes";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/classes";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating class: " + e.getMessage());
            return "redirect:/teacher/classes/edit/" + classId;
        }
    }
    @PostMapping("/classes/delete/{classId}")
    public String deleteClass(@PathVariable Long classId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            classService.deleteClass(classId, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "Class deleted successfully!");
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting class: " + e.getMessage());
        }
        return "redirect:/teacher/classes";
    }
    @GetMapping("/classes/details/{classId}")
    public String showClassDetailsForTeacher(@PathVariable Long classId,
                                             Model model,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        SchoolClass schoolClass = classService.findClassById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Class not found"));
        User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        if (!schoolClass.getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("Not authorized.");
        }
        model.addAttribute("schoolClass", schoolClass);
        return "teacher/class-details"; // Chat view
    }

    // ========================================================================
    // Assignment Management Endpoints
    // ========================================================================
    @GetMapping("/assignments/new/{classId}")
    public String showCreateAssignmentForm(@PathVariable Long classId,
                                           Model model,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        SchoolClass schoolClass = classService.findClassById(classId).orElseThrow();
        User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        if (!schoolClass.getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("Not authorized.");
        }
        model.addAttribute("assignmentDto", new AssignmentDto());
        model.addAttribute("classId", classId);
        model.addAttribute("className", schoolClass.getName());
        return "teacher/assignment-form";
    }
    @PostMapping("/assignments/create/{classId}")
    public String createAssignment(@PathVariable Long classId,
                                   @ModelAttribute("assignmentDto") AssignmentDto assignmentDto,
                                   BindingResult result,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (result.hasErrors()) {
            classService.findClassById(classId).ifPresent(sc -> model.addAttribute("className", sc.getName()));
            model.addAttribute("classId", classId);
            return "teacher/assignment-form";
        }
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            assignmentService.createAssignment(assignmentDto, classId, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment created successfully!");
            return "redirect:/teacher/assignments/class/" + classId;
        } catch (AccessDeniedException ade) {
            redirectAttributes.addFlashAttribute("errorMessage", ade.getMessage());
            return "redirect:/teacher/classes";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating assignment: " + e.getMessage());
            return "redirect:/teacher/assignments/new/" + classId;
        }
    }
    @GetMapping("/assignments/class/{classId}")
    public String listAssignmentsForClass(@PathVariable Long classId,
                                          Model model,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        SchoolClass schoolClass = classService.findClassById(classId).orElseThrow();
        User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        if (!schoolClass.getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("Not authorized.");
        }
        List<Assignment> assignments = assignmentService.findAssignmentsByClassId(classId);
        model.addAttribute("assignments", assignments);
        model.addAttribute("schoolClass", schoolClass);
        return "teacher/assignments";
    }
    @GetMapping("/assignments/edit/{assignmentId}")
    public String showEditAssignmentForm(@PathVariable Long assignmentId,
                                         Model model,
                                         @AuthenticationPrincipal UserDetails userDetails,
                                         RedirectAttributes redirectAttributes) {
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            Assignment assignment = assignmentService.findAssignmentById(assignmentId)
                    .orElseThrow(() -> new EntityNotFoundException("Assignment not found with ID: " + assignmentId));
            if (!assignment.getSchoolClass().getTeacher().getId().equals(teacher.getId())) {
                throw new AccessDeniedException("You are not authorized to edit this assignment.");
            }
            AssignmentDto assignmentDto = new AssignmentDto(assignment.getTitle(), assignment.getDescription(), assignment.getDueDate());
            model.addAttribute("assignmentDto", assignmentDto);
            model.addAttribute("assignmentId", assignmentId);
            model.addAttribute("existingAttachmentFilename", assignment.getAttachmentOriginalFilename());
            model.addAttribute("classId", assignment.getSchoolClass().getId());
            return "teacher/assignment-edit-form";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/classes";
        }
    }
    @PostMapping("/assignments/update/{assignmentId}")
    public String updateAssignment(@PathVariable Long assignmentId,
                                   @ModelAttribute("assignmentDto") AssignmentDto assignmentDto,
                                   BindingResult result,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        Long classId = null;
        Assignment existingAssignment = assignmentService.findAssignmentById(assignmentId).orElse(null);
        if (existingAssignment != null) {
            classId = existingAssignment.getSchoolClass().getId();
        }
        if (result.hasErrors()) {
            model.addAttribute("assignmentDto", assignmentDto);
            model.addAttribute("assignmentId", assignmentId);
            if (existingAssignment != null) {
                model.addAttribute("existingAttachmentFilename", existingAssignment.getAttachmentOriginalFilename());
                model.addAttribute("classId", classId);
            }
            return "teacher/assignment-edit-form";
        }
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            Assignment updatedAssignment = assignmentService.updateAssignment(assignmentId, assignmentDto, teacher);
            classId = updatedAssignment.getSchoolClass().getId();
            redirectAttributes.addFlashAttribute("successMessage", "Assignment updated successfully!");
            return "redirect:/teacher/assignments/class/" + classId;
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/classes";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating assignment: " + e.getMessage());
            return "redirect:/teacher/assignments/edit/" + assignmentId;
        }
    }
    @PostMapping("/assignments/delete/{assignmentId}")
    public String deleteAssignment(@PathVariable Long assignmentId,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        Long classId = null;
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            Assignment assignment = assignmentService.findAssignmentById(assignmentId)
                    .orElseThrow(() -> new EntityNotFoundException("Assignment not found with ID: " + assignmentId));
            classId = assignment.getSchoolClass().getId();
            assignmentService.deleteAssignment(assignmentId, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment deleted successfully!");
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (classId != null) {
                return "redirect:/teacher/assignments/class/" + classId;
            } else {
                return "redirect:/teacher/classes";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting assignment: " + e.getMessage());
            logger.error("Error deleting assignment ID {}: {}", assignmentId, e.getMessage(), e);
            if (classId != null) {
                return "redirect:/teacher/assignments/class/" + classId;
            } else {
                return "redirect:/teacher/classes";
            }
        }
        return "redirect:/teacher/assignments/class/" + classId;
    }

    // ========================================================================
    // Submission Grading Endpoints
    // ========================================================================
    @GetMapping("/assignments/{assignmentId}/submissions")
    public String viewSubmissions(@PathVariable Long assignmentId,
                                  Model model,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        Assignment assignment = assignmentService.findAssignmentById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment not found"));
        User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        if (!assignment.getSchoolClass().getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("Not authorized.");
        }
        List<Submission> submissions = submissionService.findSubmissionsByAssignment(assignment);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);
        return "teacher/submissions";
    }
    @GetMapping("/submissions/{submissionId}/grade")
    public String showGradeForm(@PathVariable Long submissionId,
                                Model model,
                                @AuthenticationPrincipal UserDetails userDetails) {
        Submission submission = submissionService.findSubmissionById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found"));
        User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
        if (!submission.getAssignment().getSchoolClass().getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("Not authorized.");
        }
        GradeDto gradeDto = new GradeDto(submission.getNumericalGrade(), submission.getFeedback());
        model.addAttribute("submission", submission);
        model.addAttribute("gradeDto", gradeDto);
        return "teacher/grade-submission";
    }
    @PostMapping("/submissions/{submissionId}/grade")
    public String processGradeSubmission(@PathVariable Long submissionId,
                                         @Valid @ModelAttribute("gradeDto") GradeDto gradeDto,
                                         BindingResult result,
                                         @AuthenticationPrincipal UserDetails userDetails,
                                         RedirectAttributes redirectAttributes,
                                         Model model) {
        Submission submission = submissionService.findSubmissionById(submissionId).orElse(null);
        if (submission == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Submission not found.");
            return "redirect:/teacher/classes";
        }
        if (result.hasErrors()) {
            model.addAttribute("submission", submission);
            return "teacher/grade-submission";
        }
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            Submission gradedSubmission = submissionService.gradeSubmission(submissionId, gradeDto, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "Submission graded successfully!");
            return "redirect:/teacher/assignments/" + gradedSubmission.getAssignment().getId() + "/submissions";
        } catch (AccessDeniedException ade) {
            redirectAttributes.addFlashAttribute("errorMessage", ade.getMessage());
            return "redirect:/teacher/assignments/" + submission.getAssignment().getId() + "/submissions";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error grading submission: " + e.getMessage());
            return "redirect:/teacher/submissions/" + submissionId + "/grade";
        }
    }

    // ========================================================================
    // Makeup Request Endpoints
    // ========================================================================
    @GetMapping("/makeup-requests")
    public String viewMakeupRequests(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            List<MakeupRequest> pendingRequests = makeupRequestService.findPendingRequestsForTeacher(teacher);
            model.addAttribute("pendingRequests", pendingRequests);
            logger.debug("Found {} pending makeup requests for teacher {}", pendingRequests.size(), teacher.getUsername());
        } catch (Exception e) {
            logger.error("Error fetching makeup requests for teacher {}: {}", userDetails.getUsername(), e.getMessage(), e);
            model.addAttribute("errorMessage", "Could not load makeup requests.");
            model.addAttribute("pendingRequests", Collections.emptyList());
        }
        return "teacher/makeup-requests";
    }
    @PostMapping("/makeup-requests/approve/{requestId}")
    public String approveMakeupRequest(@PathVariable Long requestId,
                                       @RequestParam(value = "comment", required = false) String comment,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            makeupRequestService.approveRequest(requestId, teacher, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Makeup request approved.");
        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException e) {
            logger.warn("Failed to approve makeup request ID {} for teacher {}: {}", requestId, userDetails.getUsername(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Could not approve request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error approving makeup request ID {} for teacher {}: {}", requestId, userDetails.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while approving the request.");
        }
        return "redirect:/teacher/makeup-requests";
    }
    @PostMapping("/makeup-requests/reject/{requestId}")
    public String rejectMakeupRequest(@PathVariable Long requestId,
                                      @RequestParam(value = "comment", required = false) String comment,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            makeupRequestService.rejectRequest(requestId, teacher, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Makeup request rejected.");
        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException e) {
            logger.warn("Failed to reject makeup request ID {} for teacher {}: {}", requestId, userDetails.getUsername(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Could not reject request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error rejecting makeup request ID {} for teacher {}: {}", requestId, userDetails.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while rejecting the request.");
        }
        return "redirect:/teacher/makeup-requests";
    }

    // ========================================================================
    // Quiz Management Endpoints
    // ========================================================================
    @GetMapping("/classes/{classId}/quizzes")
    public String listQuizzesForClass(@PathVariable Long classId,
                                      Model model,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("Listing quizzes for class ID {} by teacher {}", classId, userDetails.getUsername());
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            SchoolClass schoolClass = classService.findClassById(classId)
                    .orElseThrow(() -> new EntityNotFoundException("Class not found with ID: " + classId));
            if (!schoolClass.getTeacher().getId().equals(teacher.getId())) {
                throw new AccessDeniedException("You are not authorized to view quizzes for this class.");
            }
            List<Quiz> quizzes = quizService.findQuizzesByClassId(classId);
            model.addAttribute("quizzes", quizzes);
            model.addAttribute("schoolClass", schoolClass);
            return "teacher/quiz-list";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            logger.warn("Error listing quizzes for class {}: {}", classId, e.getMessage());
            model.addAttribute("errorMessage", "Could not load quizzes: " + e.getMessage());
            return "redirect:/teacher/classes";
        }
    }
    @GetMapping("/quizzes/new/{classId}")
    public String showCreateQuizForm(@PathVariable Long classId,
                                     Model model,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("Showing create quiz form for class ID {} by teacher {}", classId, userDetails.getUsername());
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            SchoolClass schoolClass = classService.findClassById(classId)
                    .orElseThrow(() -> new EntityNotFoundException("Class not found with ID: " + classId));
            if (!schoolClass.getTeacher().getId().equals(teacher.getId())) {
                throw new AccessDeniedException("You are not authorized to create quizzes for this class.");
            }
            QuizDto quizDto = new QuizDto();
            quizDto.addEmptyQuestion();
            model.addAttribute("quizDto", quizDto);
            model.addAttribute("classId", classId);
            model.addAttribute("className", schoolClass.getName());
            return "teacher/quiz-form";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            logger.warn("Error showing create quiz form for class {}: {}", classId, e.getMessage());
            return "redirect:/teacher/classes";
        }
    }
    @PostMapping("/quizzes/create/{classId}")
    public String createQuiz(@PathVariable Long classId,
                             @Valid @ModelAttribute("quizDto") QuizDto quizDto,
                             BindingResult result,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        logger.info("Attempting to create quiz for class ID {} by teacher {}", classId, userDetails.getUsername());
        if (result.hasErrors()) {
            logger.warn("Validation errors found when creating quiz for class ID {}: {}", classId, result.getAllErrors());
            classService.findClassById(classId).ifPresent(sc -> model.addAttribute("className", sc.getName()));
            model.addAttribute("classId", classId);
            return "teacher/quiz-form";
        }
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Authenticated teacher not found"));
            quizService.createQuiz(quizDto, classId, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "Quiz created successfully!");
            logger.info("Quiz created successfully for class ID {}", classId);
            return "redirect:/teacher/classes/" + classId + "/quizzes";
        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException e) {
            logger.error("Error creating quiz for class ID {}: {}", classId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating quiz: " + e.getMessage());
            return "redirect:/teacher/quizzes/new/" + classId;
        } catch (Exception e) {
             logger.error("Unexpected error creating quiz for class ID {}: {}", classId, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while creating the quiz.");
             return "redirect:/teacher/quizzes/new/" + classId;
        }
    }
    @GetMapping("/quizzes/{quizId}/view")
    public String viewQuizDetails(@PathVariable Long quizId,
                                  Model model,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("Viewing details for quiz ID {} by user {}", quizId, userDetails.getUsername());
        Long classId = null;
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            Quiz quiz = quizService.findQuizByIdForUser(quizId, teacher)
                 .orElseThrow(() -> new EntityNotFoundException("Quiz not found or not authorized."));
            classId = quiz.getSchoolClass().getId();
            model.addAttribute("quiz", quiz);
            return "teacher/quiz-details";
        } catch (EntityNotFoundException | AccessDeniedException e) {
             logger.warn("Error viewing quiz ID {}: {}", quizId, e.getMessage());
             if (classId == null) {
                 classId = quizRepository.findById(quizId).map(q -> q.getSchoolClass().getId()).orElse(null);
             }
             return "redirect:/teacher/classes" + (classId != null ? "/" + classId + "/quizzes" : "");
        }
    }
    @GetMapping("/quizzes/{quizId}/edit")
    public String showEditQuizForm(@PathVariable Long quizId,
                                   Model model,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        logger.debug("Showing edit quiz form for quiz ID {} by teacher {}", quizId, userDetails.getUsername());
        Long classId = null;
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            Quiz quiz = quizService.findQuizByIdForUser(quizId, teacher)
                 .orElseThrow(() -> new EntityNotFoundException("Quiz not found or not authorized."));
            classId = quiz.getSchoolClass().getId();
            QuizDto quizDto = new QuizDto();
            quizDto.setId(quiz.getId());
            quizDto.setTitle(quiz.getTitle());
            quizDto.setDescription(quiz.getDescription());
            quizDto.setDueDate(quiz.getDueDate());
            quizDto.setTimeLimitMinutes(quiz.getTimeLimitMinutes());
            if (quiz.getQuestions() != null) {
                quizDto.setQuestions(quiz.getQuestions().stream().map(q -> {
                    QuizQuestionDto qDto = new QuizQuestionDto();
                    qDto.setId(q.getId());
                    qDto.setQuestionText(q.getQuestionText());
                    qDto.setPoints(q.getPoints());
                    qDto.setQuestionOrder(q.getQuestionOrder());
                    return qDto;
                }).collect(Collectors.toList()));
            }
            model.addAttribute("quizDto", quizDto);
            model.addAttribute("classId", classId);
            model.addAttribute("className", quiz.getSchoolClass().getName());
            return "teacher/quiz-form";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            logger.warn("Error showing edit quiz form for quiz ID {}: {}", quizId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
             if (classId == null) { classId = quizRepository.findById(quizId).map(q -> q.getSchoolClass().getId()).orElse(null); }
            return "redirect:/teacher/classes" + (classId != null ? "/" + classId + "/quizzes" : "");
        } catch (Exception e) {
             logger.error("Unexpected error showing edit quiz form for quiz ID {}: {}", quizId, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
             return "redirect:/teacher/classes";
        }
    }
    @PostMapping("/quizzes/update/{quizId}")
    public String updateQuiz(@PathVariable Long quizId,
                             @Valid @ModelAttribute("quizDto") QuizDto quizDto,
                             BindingResult result,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        logger.info("Attempting to update quiz ID {} by teacher {}", quizId, userDetails.getUsername());
        Long classId = null;
        try {
            Quiz existingQuiz = quizRepository.findById(quizId).orElse(null);
            if (existingQuiz != null) {
                classId = existingQuiz.getSchoolClass().getId();
            }
        } catch (Exception e) {
            logger.error("Could not fetch quiz {} to get classId before update/validation.", quizId);
        }
        if (result.hasErrors()) {
            logger.warn("Validation errors found when updating quiz ID {}", quizId);
            model.addAttribute("classId", classId);
            if(classId != null) {
                classService.findClassById(classId).ifPresent(sc -> model.addAttribute("className", sc.getName()));
            }
            return "teacher/quiz-form";
        }
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            Quiz updatedQuiz = quizService.updateQuiz(quizId, quizDto, teacher);
            classId = updatedQuiz.getSchoolClass().getId();
            redirectAttributes.addFlashAttribute("successMessage", "Quiz updated successfully!");
            logger.info("Quiz ID {} updated successfully", quizId);
            return "redirect:/teacher/classes/" + classId + "/quizzes";
        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException e) {
            logger.error("Error updating quiz ID {}: {}", quizId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating quiz: " + e.getMessage());
            return "redirect:/teacher/quizzes/" + quizId + "/edit";
        } catch (Exception e) {
             logger.error("Unexpected error updating quiz ID {}: {}", quizId, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while updating the quiz.");
             return "redirect:/teacher/quizzes/" + quizId + "/edit";
        }
    }
    @PostMapping("/quizzes/delete/{quizId}")
    public String deleteQuiz(@PathVariable Long quizId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        logger.warn("Attempting to delete quiz ID {} by teacher {}", quizId, userDetails.getUsername());
        Long classId = null;
         try {
             User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
             Quiz quizToDelete = quizRepository.findById(quizId)
                     .orElseThrow(() -> new EntityNotFoundException("Quiz not found with ID: " + quizId));
             classId = quizToDelete.getSchoolClass().getId();
             quizService.deleteQuiz(quizId, teacher);
             redirectAttributes.addFlashAttribute("successMessage", "Quiz deleted successfully!");
             logger.warn("Quiz ID {} deleted successfully by teacher {}", quizId, userDetails.getUsername());
         } catch (EntityNotFoundException | AccessDeniedException e) {
             logger.error("Failed to delete quiz ID {}: {}", quizId, e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
         } catch (Exception e) {
              logger.error("Unexpected error deleting quiz ID {}: {}", quizId, e.getMessage(), e);
              redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while deleting the quiz.");
         }
         return "redirect:/teacher/classes" + (classId != null ? "/" + classId + "/quizzes" : "");
    }
    @GetMapping("/quizzes/{quizId}/attempts")
    public String viewQuizAttempts(@PathVariable Long quizId,
                                   Model model,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        logger.debug("Teacher {} viewing attempts for quiz ID {}", userDetails.getUsername(), quizId);
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
             Quiz quiz = quizService.findQuizByIdForUser(quizId, teacher)
                 .orElseThrow(() -> new EntityNotFoundException("Quiz not found or not authorized."));
            List<QuizAttempt> attempts = quizAttemptService.findAttemptsByQuizForTeacher(quizId, teacher);
            model.addAttribute("quiz", quiz);
            model.addAttribute("attempts", attempts);
            return "teacher/quiz-attempts"; // Ensure this template exists
        } catch (EntityNotFoundException | AccessDeniedException e) {
            logger.warn("Error viewing attempts for quiz ID {}: {}", quizId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            Long classId = quizRepository.findById(quizId).map(q -> q.getSchoolClass().getId()).orElse(null);
            return "redirect:/teacher/classes" + (classId != null ? "/" + classId + "/quizzes" : "");
        } catch (Exception e) {
             logger.error("Unexpected error viewing attempts for quiz ID {}: {}", quizId, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
             return "redirect:/teacher/classes";
        }
    }

    // ========================================================================
    // Quiz Attempt Grading Endpoints
    // ========================================================================

    @GetMapping("/attempts/{attemptId}/grade")
    public String showGradeAttemptForm(@PathVariable Long attemptId, Model model, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        logger.debug("Teacher {} showing grade form for attempt ID {}", userDetails.getUsername(), attemptId);
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            QuizAttempt attempt = quizAttemptService.findAttemptByIdForUser(attemptId, teacher)
                 .orElseThrow(() -> new EntityNotFoundException("Attempt not found or not authorized."));

            if (attempt.getStatus() != QuizAttempt.AttemptStatus.SUBMITTED) {
                 redirectAttributes.addFlashAttribute("errorMessage", "This attempt does not require manual grading or has already been graded.");
                 return "redirect:/teacher/quizzes/" + attempt.getQuiz().getId() + "/attempts";
            }

            QuizAttemptDto attemptDto = quizAttemptService.getAttemptResultDto(attempt);
            GradeAttemptDto gradeAttemptDto = new GradeAttemptDto(attemptId, attemptDto.getAnswerResults());

            model.addAttribute("attemptDto", attemptDto);
            model.addAttribute("gradeAttemptDto", gradeAttemptDto);
            model.addAttribute("quiz", attempt.getQuiz());

            return "teacher/grade-attempt";

        } catch (EntityNotFoundException | AccessDeniedException e) {
             logger.warn("Error showing grade form for attempt {}: {}", attemptId, e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
             Long quizId = quizAttemptRepository.findById(attemptId).map(a -> a.getQuiz().getId()).orElse(null);
             return "redirect:/teacher/classes" + (quizId != null ? "/quizzes/" + quizId + "/attempts" : "");
        } catch (Exception e) {
             logger.error("Error showing grade form for attempt {}: {}", attemptId, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
             return "redirect:/teacher/classes";
        }
    }

    @PostMapping("/attempts/{attemptId}/grade")
    public String processGradeAttempt(@PathVariable Long attemptId,
                                      @Valid @ModelAttribute("gradeAttemptDto") GradeAttemptDto gradeAttemptDto,
                                      BindingResult result,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {

        logger.info("Teacher {} processing grades for attempt ID {}", userDetails.getUsername(), attemptId);

        if (result.hasErrors()) {
             logger.warn("Validation errors grading attempt ID {}: {}", attemptId, result.getAllErrors());
             try {
                 User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
                 QuizAttempt attempt = quizAttemptService.findAttemptByIdForUser(attemptId, teacher)
                      .orElseThrow(() -> new EntityNotFoundException("Attempt not found or not authorized."));
                 QuizAttemptDto attemptDto = quizAttemptService.getAttemptResultDto(attempt);
                 model.addAttribute("attemptDto", attemptDto);
                 model.addAttribute("quiz", attempt.getQuiz());
                 return "teacher/grade-attempt"; // Return to form with errors
             } catch (Exception e) {
                  logger.error("Error repopulating grade form for attempt {} after validation failure: {}", attemptId, e.getMessage());
                  redirectAttributes.addFlashAttribute("errorMessage", "An error occurred. Please try again.");
                  Long quizId = quizAttemptRepository.findById(attemptId).map(a -> a.getQuiz().getId()).orElse(null);
                  return "redirect:/teacher/classes" + (quizId != null ? "/quizzes/" + quizId + "/attempts" : "");
             }
        }

        Long quizId = null;
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            QuizAttempt gradedAttempt = quizAttemptService.gradeQuizAttempt(attemptId, gradeAttemptDto, teacher);
            quizId = gradedAttempt.getQuiz().getId();

            redirectAttributes.addFlashAttribute("successMessage", "Quiz attempt graded successfully!");
            logger.info("Attempt ID {} graded successfully by teacher {}. Final Score: {}/{}. Status set to GRADED.",
                    gradedAttempt.getId(), teacher.getUsername(), gradedAttempt.getScore(), gradedAttempt.getMaxScore());

            return "redirect:/teacher/quizzes/" + quizId + "/attempts"; // Redirect back to attempts list

        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException e) {
             logger.warn("Error grading attempt ID {}: {}", attemptId, e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", "Grading failed: " + e.getMessage());
             return "redirect:/teacher/attempts/" + attemptId + "/grade";
        } catch (Exception e) {
             logger.error("Unexpected error grading attempt ID {}: {}", attemptId, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while grading.");
             return "redirect:/teacher/attempts/" + attemptId + "/grade";
        }
    }

    // ========================================================================
    // Gradebook Endpoint (Added Previously)
    // ========================================================================
    @GetMapping("/classes/{classId}/gradebook")
    public String showClassGradebook(@PathVariable Long classId, Model model, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        logger.debug("Teacher {} viewing gradebook for class ID {}", userDetails.getUsername(), classId);
        try {
            User teacher = userService.findUserByUsername(userDetails.getUsername()).orElseThrow();
            SchoolClass schoolClass = classService.findClassById(classId)
                    .orElseThrow(() -> new EntityNotFoundException("Class not found with ID: " + classId));

            // Authorization check
            if (!schoolClass.getTeacher().getId().equals(teacher.getId())) {
                throw new AccessDeniedException("You are not authorized to view the gradebook for this class.");
            }

            // Fetch required data
            List<User> students = schoolClass.getStudents().stream()
                                            .sorted(Comparator.comparing(User::getLastName, String.CASE_INSENSITIVE_ORDER)
                                                              .thenComparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER))
                                            .collect(Collectors.toList());
            List<Assignment> assignments = assignmentService.findAssignmentsByClassId(classId)
                                            .stream().sorted(Comparator.comparing(Assignment::getDueDate))
                                            .collect(Collectors.toList());
            List<Quiz> quizzes = quizService.findQuizzesByClassId(classId)
                                            .stream().sorted(Comparator.comparing(Quiz::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                                            .collect(Collectors.toList());

            // Fetch all submissions and attempts for the class efficiently
            List<Submission> allSubmissions = assignments.stream()
                                                .flatMap(a -> submissionService.findSubmissionsByAssignment(a).stream())
                                                .collect(Collectors.toList());
            List<QuizAttempt> allAttempts = quizzes.stream()
                                                .flatMap(q -> quizAttemptService.findAttemptsByQuizForTeacher(q.getId(), teacher).stream())
                                                .collect(Collectors.toList());

            // Group data by student ID for easier access in the template
            Map<Long, Map<Long, Submission>> submissionsByStudent = allSubmissions.stream()
                    .filter(s -> s.getStudent() != null && s.getAssignment() != null)
                    .collect(Collectors.groupingBy(s -> s.getStudent().getId(),
                             Collectors.toMap(s -> s.getAssignment().getId(), Function.identity())));

            Map<Long, Map<Long, QuizAttempt>> attemptsByStudent = allAttempts.stream()
                    .filter(a -> a.getStudent() != null && a.getQuiz() != null)
                    .collect(Collectors.groupingBy(a -> a.getStudent().getId(),
                             Collectors.toMap(a -> a.getQuiz().getId(), Function.identity())));


            model.addAttribute("schoolClass", schoolClass);
            model.addAttribute("students", students);
            model.addAttribute("assignments", assignments);
            model.addAttribute("quizzes", quizzes);
            model.addAttribute("submissionsByStudent", submissionsByStudent);
            model.addAttribute("attemptsByStudent", attemptsByStudent);

            return "teacher/class-gradebook"; // Path to the new template

        } catch (EntityNotFoundException | AccessDeniedException e) {
            logger.warn("Error accessing gradebook for class {}: {}", classId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/classes";
        } catch (Exception e) {
            logger.error("Unexpected error loading gradebook for class {}: {}", classId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while loading the gradebook.");
            return "redirect:/teacher/classes";
        }
    }


}