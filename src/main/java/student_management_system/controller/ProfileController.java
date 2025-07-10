package student_management_system.controller;

import student_management_system.model.FriendRequest;
import student_management_system.model.SchoolClass; // Import SchoolClass
import student_management_system.model.User;
import student_management_system.service.ClassService; // Import ClassService
import student_management_system.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jakarta.persistence.EntityNotFoundException;
import java.util.HashSet; // Import HashSet
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Controller for displaying user profile pages.
 * Updated to include common classes calculation.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private UserService userService;

    @Autowired // Inject ClassService
    private ClassService classService;

    @GetMapping("/{username}")
    public String viewProfile(@PathVariable String username, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        String currentUsername = userDetails.getUsername();

        try {
            User profileUser = userService.findUserByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));

            User currentUser = userService.findUserByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("Current authenticated user not found in database: " + currentUsername));

            // --- Relationship Status Logic (remains the same) ---
            String relationshipStatus = "NONE";
            Long pendingRequestId = null;
            if (currentUser.getId().equals(profileUser.getId())) {
                relationshipStatus = "SELF";
            } else {
                Set<User> friends = userService.getFriends(currentUsername);
                if (friends.contains(profileUser)) {
                    relationshipStatus = "FRIEND";
                } else {
                    List<FriendRequest> sentRequests = userService.getPendingFriendRequestsSent(currentUsername);
                    List<FriendRequest> receivedRequests = userService.getPendingFriendRequestsReceived(currentUsername);
                    boolean requestSent = sentRequests.stream().anyMatch(req -> req.getReceiver().equals(profileUser));
                    Optional<FriendRequest> requestReceived = receivedRequests.stream().filter(req -> req.getSender().equals(profileUser)).findFirst();
                    if (requestSent) {
                        relationshipStatus = "REQUEST_SENT";
                    } else if (requestReceived.isPresent()) {
                        relationshipStatus = "REQUEST_RECEIVED";
                        pendingRequestId = requestReceived.get().getId();
                    }
                }
            }

            // --- Calculate Common Classes ---
            long commonClassesCount = 0;
            // Only calculate if not viewing own profile and both are students (or could adapt for teacher/student)
            boolean profileIsStudent = profileUser.getRoles().stream().anyMatch(r -> "ROLE_STUDENT".equals(r.getName()));
            boolean currentIsStudent = currentUser.getRoles().stream().anyMatch(r -> "ROLE_STUDENT".equals(r.getName()));

            // Fetch classes only if needed to avoid unnecessary queries
            if (!relationshipStatus.equals("SELF") && profileIsStudent && currentIsStudent) {
                 try {
                    // Ensure classes are fetched (might need service method adjustments if lazy loading issues persist)
                    Set<SchoolClass> profileClasses = classService.findClassesByStudent(profileUser);
                    Set<SchoolClass> currentUserClasses = classService.findClassesByStudent(currentUser);

                    Set<SchoolClass> commonClasses = new HashSet<>(currentUserClasses);
                    commonClasses.retainAll(profileClasses); // Keep only elements present in both sets
                    commonClassesCount = commonClasses.size();
                    logger.debug("Found {} common classes between {} and {}", commonClassesCount, currentUsername, username);
                 } catch (Exception e) {
                      logger.error("Error fetching or comparing classes for {} and {}: {}", currentUsername, username, e.getMessage());
                      // Handle error gracefully, maybe set count to -1 or don't display
                      commonClassesCount = -1; // Indicate an error or inability to calculate
                 }
            }
            // --- End Calculate Common Classes ---

            model.addAttribute("profileUser", profileUser);
            model.addAttribute("relationshipStatus", relationshipStatus);
            model.addAttribute("pendingRequestId", pendingRequestId);
            // Add common classes count to the model (only add if calculated successfully)
            if (commonClassesCount >= 0) {
                 model.addAttribute("commonClassesCount", commonClassesCount);
            }


            logger.info("Viewing profile for '{}'. Relationship with '{}': {}", username, currentUsername, relationshipStatus);
            return "profile/view";

        } catch (ResponseStatusException rse) {
            logger.warn("Profile view failed: {}", rse.getMessage());
            throw rse;
        } catch (Exception e) {
            logger.error("Error loading profile page for user '{}': {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "Could not load profile.");
            return "error/500"; // Or a specific error view
        }
    }
}
