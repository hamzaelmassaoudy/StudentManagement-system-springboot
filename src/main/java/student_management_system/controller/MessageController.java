// src/main/java/student_management_system/controller/MessageController.java
package student_management_system.controller;

import student_management_system.model.ChatMessage;
import student_management_system.model.PrivateMessage;
import student_management_system.model.SchoolClass;
import student_management_system.model.User;
import student_management_system.repository.ChatMessageRepository; // Import repository
import student_management_system.repository.PrivateMessageRepository; // Import repository
import student_management_system.service.ClassService;
import student_management_system.service.UserService;
import student_management_system.web.dto.ConversationPreviewDto; // Import new DTO

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional; // Add for read-only transaction
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList; // Import ArrayList
import java.util.Collections;
import java.util.Comparator; // Import Comparator
import java.util.List; // Import List
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for displaying an aggregated view of messages (private and class chats),
 * including previews of the latest messages.
 */
@Controller
@RequestMapping("/messages")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    // Inject necessary services and repositories
    @Autowired private UserService userService;
    @Autowired private ClassService classService;
    @Autowired private PrivateMessageRepository privateMessageRepository; // Inject repo
    @Autowired private ChatMessageRepository chatMessageRepository; // Inject repo

    /**
     * Handles GET requests to /messages.
     * Fetches friends and classes, gets the latest message for each,
     * creates preview DTOs, sorts them, and adds them to the model.
     *
     * @param model       The Spring Model object.
     * @param userDetails Details of the authenticated user.
     * @return The view name "messages".
     */
    @GetMapping
    @Transactional(readOnly = true) // Use transaction for potentially lazy-loaded data and multiple reads
    public String showMessagesPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        String username = userDetails.getUsername();
        logger.debug("Loading messages overview page for user: {}", username);

        // Initialize lists to hold the preview DTOs
        List<ConversationPreviewDto> privatePreviews = new ArrayList<>();
        List<ConversationPreviewDto> classPreviews = new ArrayList<>();

        try {
            // Fetch the current user
            User currentUser = userService.findUserByUsername(username)
                    .orElseThrow(() -> new EntityNotFoundException("Current user not found: " + username));

            // --- Process Private Chats ---
            Set<User> friends = userService.getFriends(username); // Get the user's friends
            logger.debug("Found {} friends for user {}", friends.size(), username);
            for (User friend : friends) {
                // For each friend, find the latest message exchanged with the current user
                // *** THIS IS THE CORRECTED LINE ***
                PrivateMessage latestPrivate = privateMessageRepository
                        .findTopPrivateMessageBetweenUsers(currentUser, friend) // Use the correct helper method
                        .orElse(null); // Returns the message or null if none exist
                // Create a preview DTO using the factory method in ConversationPreviewDto
                privatePreviews.add(ConversationPreviewDto.fromPrivateChat(currentUser, friend, latestPrivate));
            }

            // --- Process Class Chats ---
            Set<SchoolClass> userClasses;
            String userRoleForClassLink = null; // Hint for generating correct links in the DTO

            // Determine user role to fetch appropriate classes
            boolean isTeacher = currentUser.getRoles().stream().anyMatch(r -> "ROLE_TEACHER".equals(r.getName()));
            boolean isStudent = currentUser.getRoles().stream().anyMatch(r -> "ROLE_STUDENT".equals(r.getName()));

            if (isTeacher) {
                userClasses = Set.copyOf(classService.findClassesByTeacher(currentUser)); // Fetch classes taught
                userRoleForClassLink = "teacher";
            } else if (isStudent) {
                userClasses = classService.findClassesByStudent(currentUser); // Fetch enrolled classes
                userRoleForClassLink = "student";
            } else {
                userClasses = Collections.emptySet(); // No classes for other roles (e.g., Admin)
            }
            logger.debug("Found {} classes for user {} (Role hint: {})", userClasses.size(), username, userRoleForClassLink);

            for (SchoolClass sClass : userClasses) {
                // For each class, find the latest message posted in that class
                ChatMessage latestClassMsg = chatMessageRepository
                        .findTopBySchoolClassOrderByTimestampDesc(sClass)
                        .orElse(null); // Returns the message or null if none exist
                // Create a preview DTO using the factory method, passing the role hint
                classPreviews.add(ConversationPreviewDto.fromClassChat(currentUser, sClass, latestClassMsg, userRoleForClassLink));
            }

            // --- Sort Previews (Optional but recommended for better UX) ---
            // Sort both lists by the timestamp of the last message, descending (most recent first).
            // Handle null timestamps (conversations with no messages) by placing them last.
            Comparator<ConversationPreviewDto> byLastMessageDesc = Comparator
                .comparing(ConversationPreviewDto::getLastMessageTimestamp, Comparator.nullsLast(Comparator.reverseOrder()));

            privatePreviews.sort(byLastMessageDesc);
            classPreviews.sort(byLastMessageDesc);

            // Add the lists of DTOs to the model for the template to use
            model.addAttribute("privateChatPreviews", privatePreviews);
            model.addAttribute("classChatPreviews", classPreviews);

        } catch (EntityNotFoundException e) {
            logger.error("User not found when loading messages page: {}", username, e);
            model.addAttribute("errorMessage", "Could not load message overview: User not found.");
            // Provide empty lists to prevent potential template errors
            model.addAttribute("privateChatPreviews", Collections.emptyList());
            model.addAttribute("classChatPreviews", Collections.emptyList());
        } catch (Exception e) {
            logger.error("Error loading messages overview for user {}: {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the messages overview.");
            // Provide empty lists to prevent potential template errors
            model.addAttribute("privateChatPreviews", Collections.emptyList());
            model.addAttribute("classChatPreviews", Collections.emptyList());
        }

        // Render the messages template
        return "messages"; // Renders src/main/resources/templates/messages.html
    }
}
