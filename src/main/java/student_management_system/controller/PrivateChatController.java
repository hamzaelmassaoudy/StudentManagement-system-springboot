package student_management_system.controller;

import student_management_system.model.FriendRequest;
import student_management_system.model.PrivateMessage;
import student_management_system.model.User;
import student_management_system.model.SchoolClass;
import student_management_system.service.ClassService;
import student_management_system.service.UserService;
import student_management_system.service.PrivateMessageService;
import student_management_system.web.dto.PrivateMessageDto; // Use updated DTO

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * Controller handling WebSocket messages and REST requests for private chat.
 * Includes enhanced error handling and data fetching logic.
 */
@Controller
public class PrivateChatController {

    private static final Logger logger = LoggerFactory.getLogger(PrivateChatController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private PrivateMessageService privateMessageService;

    @Autowired
    private ClassService classService;

    @Value("${file.private-attachment-upload-dir}")
    private String privateAttachmentUploadDir;

    /**
     * Displays the private chat page for a specific friend.
     * Verifies friendship and calculates common classes.
     * GET /chat/private/{friendUsername}
     */
    @GetMapping("/chat/private/{friendUsername}")
    @Transactional(readOnly = true)
    public String showPrivateChatPage(@PathVariable String friendUsername,
                                      Model model,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            logger.warn("Attempt to access private chat page without authentication.");
            return "redirect:/login";
        }
        String currentUsername = userDetails.getUsername();
        logger.debug("Loading private chat page for user {} with friend {}", currentUsername, friendUsername);

        try {
            User currentUser = userService.findUserByUsername(currentUsername)
                    .orElseThrow(() -> new EntityNotFoundException("Current user not found: " + currentUsername));
            User friendUser = userService.findUserByUsername(friendUsername)
                    .orElseThrow(() -> new EntityNotFoundException("Friend user not found: " + friendUsername));

            Set<User> friends = userService.getFriends(currentUsername);
            if (!friends.contains(friendUser)) {
                 logger.warn("Access Denied: User {} attempted to open private chat with non-friend {}", currentUsername, friendUsername);
                 redirectAttributes.addFlashAttribute("errorMessage", "You are not friends with this user.");
                 return "redirect:/friends";
            }

            long commonClassesCount = 0;
            try {
                Set<SchoolClass> currentUserClasses = classService.findClassesByStudent(currentUser);
                Set<SchoolClass> friendUserClasses = classService.findClassesByStudent(friendUser);
                Set<SchoolClass> commonClasses = new HashSet<>(currentUserClasses);
                commonClasses.retainAll(friendUserClasses);
                commonClassesCount = commonClasses.size();
                logger.debug("Found {} common classes between {} and {}", commonClassesCount, currentUsername, friendUsername);
            } catch (Exception e) {
                 logger.error("Error calculating common classes between {} and {}: {}", currentUsername, friendUsername, e.getMessage());
                 model.addAttribute("commonClassesError", "Could not load common classes info.");
                 commonClassesCount = -1;
            }

            model.addAttribute("friendUser", friendUser);
            model.addAttribute("commonClassesCount", commonClassesCount >= 0 ? commonClassesCount : null);
            model.addAttribute("currentUsername", currentUsername);

            return "chat/private";

        } catch (EntityNotFoundException e) {
            logger.error("User not found when loading private chat page: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/friends";
        } catch (Exception e) {
            logger.error("Unexpected error loading private chat page between {} and {}: {}", currentUsername, friendUsername, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while loading the chat page.");
            return "redirect:/dashboard";
        }
    }


    /**
     * Handles incoming private TEXT messages sent via WebSocket.
     * Destination: /app/private.chat
     * Saves the message and broadcasts an enhanced DTO with sender details.
     */
    @MessageMapping("/private.chat")
    @Transactional // Ensure atomicity
    public void sendPrivateMessage(@Payload PrivateMessageDto messageDto, Principal principal) {
        if (principal == null || messageDto == null) { /* ... validation ... */ return; }
        String senderUsername = principal.getName();
        String receiverUsername = messageDto.getReceiverUsername();
        if (!StringUtils.hasText(receiverUsername) || senderUsername.equals(receiverUsername)) { /* ... validation ... */ return; }
        if (!StringUtils.hasText(messageDto.getContent()) && !StringUtils.hasText(messageDto.getAttachmentUrl())) { /* ... validation ... */ return; }

        logger.debug("Processing private message from {} to {}", senderUsername, receiverUsername);

        try {
            User sender = userService.findUserByUsername(senderUsername)
                    .orElseThrow(() -> new EntityNotFoundException("Sender not found: " + senderUsername));
            User receiver = userService.findUserByUsername(receiverUsername)
                    .orElseThrow(() -> new EntityNotFoundException("Receiver not found: " + receiverUsername));

            if (!userService.getFriends(senderUsername).contains(receiver)) {
                throw new AccessDeniedException("Cannot send message: Users are not friends.");
            }

            PrivateMessage savedMessage = privateMessageService.saveMessage(
                sender, receiver, messageDto.getContent(), null, null
            );
            logger.debug("Saved private text message with ID: {}", savedMessage.getId());

            // *** UPDATED: Use constructor that populates sender details ***
            PrivateMessageDto dtoToSend = new PrivateMessageDto(
                savedMessage.getId(),
                savedMessage.getContent(),
                sender, // Pass sender User object
                receiver, // Pass receiver User object
                savedMessage.getTimestamp(),
                null, // No attachment URL for text messages
                null
            );
            // *** END UPDATED ***

            messagingTemplate.convertAndSendToUser(receiverUsername, "/queue/private", dtoToSend);
            messagingTemplate.convertAndSendToUser(senderUsername, "/queue/private", dtoToSend);
            logger.debug("Sent text message ID {} notification to receiver {} and sender {}", savedMessage.getId(), receiverUsername, senderUsername);

        } catch (EntityNotFoundException | AccessDeniedException e) {
            logger.warn("Failed to process private message from {} to {}: {}", senderUsername, receiverUsername, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing private message from {} to {}: {}", senderUsername, receiverUsername, e.getMessage(), e);
        }
    }


    /**
     * REST endpoint to handle file uploads for private chat.
     * POST /chat/private/upload/{receiverUsername}
     * Saves the file, creates a message record, and sends WebSocket notifications.
     */
    @PostMapping("/chat/private/upload/{receiverUsername}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> uploadPrivateFile(
            @PathVariable String receiverUsername,
            @RequestParam("attachmentFile") MultipartFile file,
            @RequestParam(value = "messageContent", required = false) String messageContent,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) { /* ... validation ... */ }
        String senderUsername = userDetails.getUsername();
        logger.info("Received file upload from {} for {}", senderUsername, receiverUsername);
        if (file.isEmpty() || senderUsername.equals(receiverUsername)) { /* ... validation ... */ }
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) { /* ... validation ... */ }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String uniqueFilename = null;

        try {
            User sender = userService.findUserByUsername(senderUsername)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender not found: " + senderUsername));
            User receiver = userService.findUserByUsername(receiverUsername)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver not found: " + receiverUsername));
            if (!userService.getFriends(senderUsername).contains(receiver)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot send file: Users are not friends.");
            }

            String fileExtension = "";
            int lastDot = originalFilename.lastIndexOf('.');
            if (lastDot >= 0) fileExtension = originalFilename.substring(lastDot);
            uniqueFilename = senderUsername + "_to_" + receiverUsername + "_" + UUID.randomUUID().toString() + fileExtension;
            Path uploadPath = Paths.get(privateAttachmentUploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(uniqueFilename).normalize();

            if (!Files.exists(uploadPath)) { Files.createDirectories(uploadPath); }
            if (!filePath.startsWith(uploadPath)) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path."); }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Saved private attachment '{}' for message from {} to {}", filePath, senderUsername, receiverUsername);

            PrivateMessage savedMessage = privateMessageService.saveMessage(
                sender, receiver, messageContent, uniqueFilename, originalFilename
            );
            logger.debug("Saved private message record ID {} for file upload", savedMessage.getId());

            String attachmentUrl = "/download/private-attachment/" + savedMessage.getAttachmentPath();

            // *** UPDATED: Use constructor that populates sender details ***
            PrivateMessageDto dtoToSend = new PrivateMessageDto(
                savedMessage.getId(),
                savedMessage.getContent(),
                sender, // Pass sender User object
                receiver, // Pass receiver User object
                savedMessage.getTimestamp(),
                attachmentUrl,
                savedMessage.getAttachmentOriginalFilename()
            );
            // *** END UPDATED ***

            messagingTemplate.convertAndSendToUser(receiverUsername, "/queue/private", dtoToSend);
            messagingTemplate.convertAndSendToUser(senderUsername, "/queue/private", dtoToSend);
            logger.debug("Sent WebSocket notification for file message ID {} to {} and {}", savedMessage.getId(), receiverUsername, senderUsername);

            return ResponseEntity.ok().body(Map.of(
                "message", "File uploaded successfully.",
                "messageId", savedMessage.getId(),
                "attachmentUrl", attachmentUrl,
                "originalFilename", savedMessage.getAttachmentOriginalFilename()
            ));

        } catch (IOException e) { /* ... error handling ... */
            logger.error("Could not save uploaded file '{}' from {}: {}", originalFilename, senderUsername, e.getMessage(), e);
            if (uniqueFilename != null) { try { Files.deleteIfExists(Paths.get(privateAttachmentUploadDir).resolve(uniqueFilename)); } catch (IOException ignored) {} }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to save uploaded file."));
        } catch (ResponseStatusException e) { /* ... error handling ... */
             logger.warn("File upload failed for {}: Status {}, Reason: {}", senderUsername, e.getStatusCode(), e.getReason());
             return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        } catch (Exception e) { /* ... error handling ... */
             logger.error("Unexpected error during file upload from {}: {}", senderUsername, e.getMessage(), e);
             if (uniqueFilename != null) { try { Files.deleteIfExists(Paths.get(privateAttachmentUploadDir).resolve(uniqueFilename)); } catch (IOException ignored) {} }
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred during file upload."));
        }
    }


    /**
     * REST endpoint to fetch private chat history between the logged-in user and another user.
     * GET /chat/private/history/{otherUsername}
     * Ensures users are friends before returning history.
     */
    @GetMapping("/chat/private/history/{otherUsername}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PrivateMessageDto>> getPrivateChatHistory(
            @PathVariable String otherUsername,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) { /* ... auth check ... */ }
        String currentUsername = userDetails.getUsername();
        logger.debug("Fetching private chat history between {} and {}", currentUsername, otherUsername);

        try {
            User currentUser = userService.findUserByUsername(currentUsername)
                    .orElseThrow(() -> new EntityNotFoundException("Current user not found: " + currentUsername));
            User otherUser = userService.findUserByUsername(otherUsername)
                    .orElseThrow(() -> new EntityNotFoundException("Other user not found: " + otherUsername));

            if (!userService.getFriends(currentUsername).contains(otherUser)) {
                logger.warn("Attempt to fetch chat history between non-friends: {} and {}", currentUsername, otherUsername);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<PrivateMessage> messages = privateMessageService.getConversation(currentUser, otherUser);

            // *** UPDATED: Use constructor that populates sender details ***
            List<PrivateMessageDto> messageDtos = messages.stream()
                    .map(msg -> new PrivateMessageDto(
                            msg.getId(),
                            msg.getContent(),
                            msg.getSender(), // Pass sender User
                            msg.getReceiver(), // Pass receiver User
                            msg.getTimestamp(),
                            msg.getAttachmentPath() != null ? "/download/private-attachment/" + msg.getAttachmentPath() : null,
                            msg.getAttachmentOriginalFilename()
                    ))
                    .collect(Collectors.toList());
            // *** END UPDATED ***

            logger.debug("Returning {} messages for conversation between {} and {}", messageDtos.size(), currentUsername, otherUsername);
            return ResponseEntity.ok(messageDtos);

        } catch (EntityNotFoundException e) { /* ... error handling ... */
            logger.error("User not found during history fetch: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) { /* ... error handling ... */
            logger.error("Error fetching private chat history between {} and {}: {}", currentUsername, otherUsername, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
