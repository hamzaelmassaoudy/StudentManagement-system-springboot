package student_management_system.controller;

import student_management_system.model.FriendRequest;
import student_management_system.model.User;
import student_management_system.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// --- Correct Import for AccessDeniedException ---
import org.springframework.security.access.AccessDeniedException;
// -----------------------------------------------

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for handling friend-related actions (viewing, searching, requests).
 */
@Controller
@RequestMapping("/friends")
public class FriendController {

    private static final Logger logger = LoggerFactory.getLogger(FriendController.class);

    @Autowired
    private UserService userService;

    /**
     * Displays the list of friends and pending incoming friend requests.
     */
    @GetMapping
    public String viewFriendsAndRequests(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        String username = userDetails.getUsername();
        logger.debug("Fetching friends and requests for user: {}", username);

        try {
            Set<User> friends = userService.getFriends(username);
            List<FriendRequest> pendingRequests = userService.getPendingFriendRequestsReceived(username);

            model.addAttribute("friends", friends);
            model.addAttribute("pendingRequests", pendingRequests);

            logger.debug("Found {} friends and {} pending requests for user: {}", friends.size(), pendingRequests.size(), username);

        } catch (EntityNotFoundException e) {
            logger.error("User not found when trying to fetch friends/requests: {}", username, e);
            model.addAttribute("errorMessage", "Could not load friend list: User not found.");
            model.addAttribute("friends", Set.of());
            model.addAttribute("pendingRequests", List.of());
        } catch (Exception e) {
             logger.error("Error fetching friends/requests for user {}: {}", username, e.getMessage(), e);
             model.addAttribute("errorMessage", "An error occurred while loading friends or requests.");
             model.addAttribute("friends", Set.of());
             model.addAttribute("pendingRequests", List.of());
        }

        return "friends/list";
    }

    /**
     * Displays the search page and handles search results.
     * Also indicates if a request is pending with found users.
     */
    @GetMapping("/search")
    public String searchFriends(@RequestParam(value = "searchTerm", required = false) String searchTerm,
                                Model model,
                                @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        String currentUsername = userDetails.getUsername();
        List<User> searchResults = new ArrayList<>();
        Map<Long, String> pendingRequestStatus = Map.of();

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            logger.debug("Searching for potential friends for user '{}' with term: {}", currentUsername, searchTerm);
            try {
                User currentUser = userService.findUserByUsername(currentUsername).orElseThrow();
                searchResults = userService.searchPotentialFriends(currentUsername, searchTerm.trim());

                List<FriendRequest> sentRequests = userService.getPendingFriendRequestsSent(currentUsername);
                List<FriendRequest> receivedRequests = userService.getPendingFriendRequestsReceived(currentUsername);

                pendingRequestStatus = searchResults.stream()
                    .collect(Collectors.toMap(
                        User::getId,
                        user -> {
                            boolean sent = sentRequests.stream().anyMatch(req -> req.getReceiver().equals(user));
                            boolean received = receivedRequests.stream().anyMatch(req -> req.getSender().equals(user));
                            if (sent) return "SENT";
                            if (received) return "RECEIVED";
                            return "NONE";
                        }
                    ));

                logger.debug("Found {} potential friends for term '{}'", searchResults.size(), searchTerm);

            } catch (EntityNotFoundException e) {
                 logger.error("Current user '{}' not found during friend search.", currentUsername, e);
                 model.addAttribute("errorMessage", "Could not perform search: Current user not found.");
            } catch (Exception e) {
                 logger.error("Error searching for potential friends for user {}: {}", currentUsername, e.getMessage(), e);
                 model.addAttribute("errorMessage", "An error occurred during the search.");
            }
        }

        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("searchResults", searchResults);
        model.addAttribute("pendingRequestStatus", pendingRequestStatus);

        return "friends/search";
    }

    /**
     * Handles the request to SEND a friend request.
     */
    @PostMapping("/request/send/{receiverUsername}")
    public String sendFriendRequest(@PathVariable String receiverUsername,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        String senderUsername = userDetails.getUsername();
        logger.info("User '{}' attempting to send friend request to '{}'", senderUsername, receiverUsername);

        try {
            userService.sendFriendRequest(senderUsername, receiverUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request sent to " + receiverUsername + "!");
            logger.info("User '{}' successfully sent friend request to '{}'", senderUsername, receiverUsername);
        } catch (EntityNotFoundException | IllegalArgumentException e) { // Catch specific exceptions
            logger.warn("Failed to send friend request from '{}' to '{}': {}", senderUsername, receiverUsername, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Could not send request: " + e.getMessage());
        } catch (Exception e) { // Catch broader exceptions
             logger.error("Unexpected error sending friend request from '{}' to '{}': {}", senderUsername, receiverUsername, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while sending the request.");
        }

        return "redirect:/friends/search";
    }

     /**
     * Handles the request to ACCEPT a friend request.
     */
    @PostMapping("/request/accept/{requestId}")
    public String acceptFriendRequest(@PathVariable Long requestId,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        String receiverUsername = userDetails.getUsername();
        logger.info("User '{}' attempting to accept friend request ID {}", receiverUsername, requestId);

        try {
            userService.acceptFriendRequest(requestId, receiverUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request accepted!");
            logger.info("User '{}' successfully accepted friend request ID {}", receiverUsername, requestId);
        } catch (EntityNotFoundException | IllegalArgumentException | AccessDeniedException e) { // Catch specific exceptions
             logger.warn("Failed to accept friend request ID {} for user '{}': {}", requestId, receiverUsername, e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", "Could not accept request: " + e.getMessage());
        } catch (Exception e) { // Catch broader exceptions
             logger.error("Unexpected error accepting friend request ID {} for user '{}': {}", requestId, receiverUsername, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
        }

        return "redirect:/friends";
    }

     /**
     * Handles the request to REJECT a friend request.
     */
    @PostMapping("/request/reject/{requestId}")
    public String rejectFriendRequest(@PathVariable Long requestId,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        String receiverUsername = userDetails.getUsername();
         logger.info("User '{}' attempting to reject friend request ID {}", receiverUsername, requestId);

        try {
            userService.rejectFriendRequest(requestId, receiverUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request rejected.");
             logger.info("User '{}' successfully rejected friend request ID {}", receiverUsername, requestId);
        } catch (EntityNotFoundException | IllegalArgumentException | AccessDeniedException e) { // Catch specific exceptions
             logger.warn("Failed to reject friend request ID {} for user '{}': {}", requestId, receiverUsername, e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", "Could not reject request: " + e.getMessage());
        } catch (Exception e) { // Catch broader exceptions
             logger.error("Unexpected error rejecting friend request ID {} for user '{}': {}", requestId, receiverUsername, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
        }

        return "redirect:/friends";
    }


    /**
     * Handles the request to REMOVE an existing friend.
     */
    @PostMapping("/remove/{friendUsername}")
    public String removeFriend(@PathVariable String friendUsername,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        String currentUsername = userDetails.getUsername();
         logger.info("User '{}' attempting to remove friend '{}'", currentUsername, friendUsername);

        try {
            userService.removeFriend(currentUsername, friendUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Successfully removed " + friendUsername + " from your friends list.");
             logger.info("User '{}' successfully removed friend '{}'", currentUsername, friendUsername);
        } catch (EntityNotFoundException | IllegalArgumentException e) { // Catch specific exceptions
             logger.warn("Failed to remove friend '{}' for user '{}': {}", friendUsername, currentUsername, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Could not remove friend: " + e.getMessage());
        } catch (Exception e) { // Catch broader exceptions
             logger.error("Unexpected error removing friend '{}' for user '{}': {}", friendUsername, currentUsername, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while removing friend.");
        }

        return "redirect:/friends";
    }
}
