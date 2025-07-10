package student_management_system.controller;

import student_management_system.model.*; // Import all models
import student_management_system.service.*; // Import all services

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional; // Import Transactional
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime; // Import LocalDateTime
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator; // Import Comparator
import java.util.HashMap; // Import HashMap
import java.util.List;
import java.util.Map;
import java.util.Optional; // Import Optional
import java.util.Set;
import java.util.function.Function; // Import Function
import java.util.stream.Collectors; // Import Collectors

/**
 * Controller for handling general pages like the dashboard, home page,
 * and access denied page.
 * UPDATED: Refined dashboard logic for student's "Request Makeup" button visibility.
 */
@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired private UserService userService;
    @Autowired private ClassService classService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private SubmissionService submissionService;
    @Autowired private MakeupRequestService makeupRequestService;
    @Autowired private QuizService quizService;
    @Autowired private QuizAttemptService quizAttemptService;


    @GetMapping({"/", "/home"})
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true) // Use read-only transaction for data fetching
    public String dashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        logger.debug("Loading dashboard for user: {}", username);

        User currentUser = userService.findUserByUsername(username).orElse(null);

        if (currentUser == null) {
            logger.error("Authenticated user '{}' not found in database for dashboard.", username);
            return "redirect:/login?error=user_not_found";
        }

        model.addAttribute("currentUser", currentUser);

        boolean isStudent = authorities.stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_STUDENT"));
        boolean isTeacher = authorities.stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_TEACHER"));
        boolean isAdmin = authorities.stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));


        int dashboardItemLimit = 5; // Limit items shown on dashboard

        if (isStudent) {
            try {
                // --- Pending Assignments ---
                List<Assignment> pendingAssignments = assignmentService.findPendingAssignmentsForStudent(currentUser, dashboardItemLimit);
                model.addAttribute("pendingAssignments", pendingAssignments);
                logger.debug("Added {} pending assignments to model for student {}", pendingAssignments.size(), username);

                // --- Latest Grades ---
                List<Submission> latestGrades = submissionService.findLatestGradedSubmissions(currentUser, dashboardItemLimit);
                Map<Long, Boolean> canRequestMakeupMap = new HashMap<>();
                if (latestGrades != null) {
                    for (Submission sub : latestGrades) {
                        boolean canRequest = false;
                        // Condition: Original submission, failed, and no makeup request initiated yet for this original submission.
                        if (!sub.isMakeupSubmission() && sub.getNumericalGrade() != null && sub.getNumericalGrade() < 60) {
                            Optional<MakeupRequest> reqOpt = makeupRequestService.findRequestBySubmission(sub);
                            if (reqOpt.isEmpty()) { // Only if NO request has ever been made
                                canRequest = true;
                            }
                        }
                        canRequestMakeupMap.put(sub.getId(), canRequest);
                    }
                }
                model.addAttribute("latestGrades", latestGrades);
                model.addAttribute("canRequestMakeupMap", canRequestMakeupMap); // Pass this map to the template
                logger.debug("Added {} latest grades to model for student {}", latestGrades.size(), username);


                // --- Pending Quizzes ---
                Set<SchoolClass> enrolledClasses = classService.findClassesByStudent(currentUser);
                List<Quiz> allQuizzes = enrolledClasses.stream()
                        .flatMap(sc -> quizService.findQuizzesByClassId(sc.getId()).stream())
                        .collect(Collectors.toList());
                Map<Long, QuizAttempt> studentAttemptMap = quizAttemptService.findAttemptsByStudent(currentUser).stream()
                        .filter(att -> att.getQuiz() != null)
                        .collect(Collectors.toMap(att -> att.getQuiz().getId(), Function.identity(), (e1, e2) -> e1));

                LocalDateTime now = LocalDateTime.now();
                List<Quiz> pendingQuizzes = allQuizzes.stream()
                        .filter(quiz -> {
                            QuizAttempt attempt = studentAttemptMap.get(quiz.getId());
                            boolean pastDue = quiz.getDueDate() != null && now.isAfter(quiz.getDueDate());
                            return !pastDue && (attempt == null || attempt.getStatus() == QuizAttempt.AttemptStatus.IN_PROGRESS);
                        })
                        .sorted(Comparator.comparing(Quiz::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                        .limit(dashboardItemLimit)
                        .collect(Collectors.toList());
                model.addAttribute("pendingQuizzes", pendingQuizzes);
                model.addAttribute("studentAttemptMap", studentAttemptMap);
                logger.debug("Added {} pending quizzes to model for student {}", pendingQuizzes.size(), username);

                // --- Reviewed Makeup Requests ---
                List<MakeupRequest> reviewedMakeupRequests = makeupRequestService.findRequestsByStudent(currentUser).stream()
                        .filter(req -> req.getStatus() == MakeupRequest.MakeupRequestStatus.APPROVED || req.getStatus() == MakeupRequest.MakeupRequestStatus.REJECTED)
                        .sorted(Comparator.comparing(MakeupRequest::getReviewedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(dashboardItemLimit)
                        .collect(Collectors.toList());
                model.addAttribute("reviewedMakeupRequests", reviewedMakeupRequests);
                logger.debug("Added {} reviewed makeup requests to model for student {}", reviewedMakeupRequests.size(), username);


            } catch (Exception e) {
                logger.error("Error fetching student dashboard data for {}: {}", username, e.getMessage(), e);
                model.addAttribute("dashboardError", "Could not load all dashboard data.");
                model.addAttribute("pendingAssignments", Collections.emptyList());
                model.addAttribute("latestGrades", Collections.emptyList());
                model.addAttribute("canRequestMakeupMap", Collections.emptyMap());
                model.addAttribute("pendingQuizzes", Collections.emptyList());
                model.addAttribute("reviewedMakeupRequests", Collections.emptyList());
                model.addAttribute("studentAttemptMap", Collections.emptyMap());
            }
        } else if (isTeacher) {
            try {
                Map<Assignment, List<Submission>> assignmentsToGrade = submissionService.findAssignmentsWithUngradedSubmissions(currentUser, dashboardItemLimit);
                model.addAttribute("assignmentsToGrade", assignmentsToGrade);
                logger.debug("Added {} assignments with ungraded submissions to model for teacher {}", assignmentsToGrade.size(), username);

                List<MakeupRequest> pendingMakeupRequests = makeupRequestService.findPendingRequestsForTeacher(currentUser);
                model.addAttribute("pendingMakeupRequestCount", pendingMakeupRequests.size());
                logger.debug("Added {} pending makeup requests count to model for teacher {}", pendingMakeupRequests.size(), username);

                 List<QuizAttempt> pendingQuizGrading = quizAttemptService.findAttemptsByTeacherAndStatus(currentUser, QuizAttempt.AttemptStatus.SUBMITTED, dashboardItemLimit);
                 model.addAttribute("pendingQuizGrading", pendingQuizGrading);
                 logger.debug("Added {} quizzes needing grading to model for teacher {}", pendingQuizGrading.size(), username);


            } catch (Exception e) {
                logger.error("Error fetching teacher dashboard data for {}: {}", username, e.getMessage(), e);
                model.addAttribute("dashboardError", "Could not load all dashboard data.");
                 model.addAttribute("assignmentsToGrade", Collections.emptyMap());
                 model.addAttribute("pendingMakeupRequestCount", 0);
                 model.addAttribute("pendingQuizGrading", Collections.emptyList());
            }
        } else if (isAdmin) {
            logger.debug("Loading admin dashboard view for user {}", username);
        }

        return "dashboard";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
