package student_management_system.controller;

// --- Model Imports ---
import student_management_system.model.ChatMessage;
import student_management_system.model.SchoolClass;
import student_management_system.model.User;

// --- Service Imports ---
import student_management_system.service.ClassService;
import student_management_system.service.UserService;

// --- Repository Import ---
import student_management_system.repository.ChatMessageRepository;

// --- DTO Import ---
import student_management_system.web.dto.ChatMessageDto; // Use the enhanced DTO

// --- Spring Framework Imports ---
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// --- Java Util Imports ---
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// --- Logging Imports ---
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller handling WebSocket messages and chat history retrieval.
 * Updated to use enhanced ChatMessageDto with more sender details.
 */
@Controller
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class); // Use logger

    @Autowired private UserService userService;
    @Autowired private ClassService classService;
    @Autowired private ChatMessageRepository chatMessageRepository;

    /**
     * Handles incoming chat messages sent to "/app/chat.sendMessage/{classId}".
     * Saves the message and broadcasts an enhanced DTO with sender details.
     */
    @MessageMapping("/chat.sendMessage/{classId}")
    @SendTo("/topic/class/{classId}")
    @Transactional // Wrap in transaction
    public ChatMessageDto sendMessage(
            @DestinationVariable Long classId,
            @Payload ChatMessageDto chatMessageDto, // Receives basic DTO from client
            Principal principal) {

        if (principal == null || chatMessageDto == null || chatMessageDto.getContent() == null || chatMessageDto.getContent().isBlank()) {
            logger.warn("Invalid message received (null principal, DTO, or empty content).");
            return null; // Ignore invalid request
        }

        String username = principal.getName();
        logger.debug("Processing message from {} for class ID {}", username, classId);

        try {
            // 1. Fetch sender User and target SchoolClass
            User senderUser = userService.findUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Sender user not found: " + username));
            SchoolClass targetClass = classService.findClassById(classId)
                    .orElseThrow(() -> new RuntimeException("Target class not found: " + classId));

            // 2. Authorization Check
            boolean isTeacher = targetClass.getTeacher().getId().equals(senderUser.getId());
            boolean isEnrolled = !isTeacher && classService.findClassesByStudent(senderUser).contains(targetClass); // Check enrollment if not teacher

            if (!isTeacher && !isEnrolled) {
                throw new AccessDeniedException("User '" + username + "' is not authorized for class ID: " + classId);
            }

            // 3. Create and save the ChatMessage entity
            ChatMessage messageToSave = new ChatMessage(
                chatMessageDto.getContent(),
                senderUser,
                targetClass
            );
            chatMessageRepository.save(messageToSave);
            logger.debug("Saved chat message from {} to class {}", username, classId);

            // 4. Prepare the ENHANCED DTO to broadcast
            ChatMessageDto dtoToSend = new ChatMessageDto();
            dtoToSend.setContent(messageToSave.getContent());
            dtoToSend.setClassId(messageToSave.getSchoolClass().getId());
            dtoToSend.populateSenderDetails(senderUser); // Populate all sender details

            return dtoToSend; // Broadcast the enhanced DTO

        } catch (AccessDeniedException ade) {
            logger.warn("Authorization error sending chat message: {}", ade.getMessage());
            return null; // Don't broadcast
        } catch (Exception e) {
            logger.error("Error processing/saving chat message from {}: {}", username, e.getMessage(), e);
            return null; // Don't broadcast on error
        }
    }


    /**
     * Handles users joining the chat (sends a system-like message).
     * Updated to populate sender details in the broadcasted DTO.
     */
    @MessageMapping("/chat.addUser/{classId}")
    @SendTo("/topic/class/{classId}")
    @Transactional(readOnly = true) // Read-only as we only fetch user data
    public ChatMessageDto addUser(
            @DestinationVariable Long classId,
            @Payload ChatMessageDto chatMessageDto, // Basic DTO from client (mostly ignored)
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {

        if (principal == null) {
            logger.warn("addUser called without authenticated principal for class ID {}", classId);
            return null;
        }
        String username = principal.getName();
        logger.debug("User {} joining chat for class ID {}", username, classId);

        try {
            User senderUser = userService.findUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Joining user not found: " + username));

            // Optional: Add authorization check here too

            // Store username in session attributes (optional)
            headerAccessor.getSessionAttributes().put("username", username);
            headerAccessor.getSessionAttributes().put("classId", classId);

            // Prepare the DTO to broadcast
            ChatMessageDto dtoToSend = new ChatMessageDto();
            dtoToSend.setContent(senderUser.getFirstName() + " joined the chat!"); // Use first name
            dtoToSend.setClassId(classId);
            dtoToSend.populateSenderDetails(senderUser); // Populate sender details

            return dtoToSend;

        } catch (Exception e) {
            logger.error("Error processing addUser for user {} in class {}: {}", username, classId, e.getMessage(), e);
            return null;
        }
    }


    /**
     * REST endpoint to fetch chat history for a specific class.
     * Updated to return the enhanced ChatMessageDto.
     */
    @GetMapping("/chat/history/{classId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(@PathVariable Long classId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
             return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        logger.debug("Fetching chat history for class ID {} by user {}", classId, username);

        User currentUser = userService.findUserByUsername(username).orElse(null);
        if (currentUser == null) {
             logger.warn("User {} not found in DB during history fetch.", username);
             return ResponseEntity.status(401).build();
        }

        SchoolClass targetClass = classService.findClassById(classId).orElse(null);
        if (targetClass == null) {
            logger.warn("Class ID {} not found during history fetch.", classId);
            return ResponseEntity.notFound().build();
        }

        // Authorization Check
        boolean isTeacher = targetClass.getTeacher().getId().equals(currentUser.getId());
        boolean isEnrolled = !isTeacher && classService.findClassesByStudent(currentUser).contains(targetClass);

        if (!isTeacher && !isEnrolled) {
            logger.warn("User {} forbidden from accessing history for class ID {}", username, classId);
            return ResponseEntity.status(403).build();
        }

        // Fetch messages
        List<ChatMessage> messages = chatMessageRepository.findBySchoolClassOrderByTimestampAsc(targetClass);
        logger.debug("Found {} messages in history for class ID {}", messages.size(), classId);

        // Convert entities to ENHANCED DTOs
        List<ChatMessageDto> messageDtos = messages.stream()
                .map(msg -> {
                    ChatMessageDto dto = new ChatMessageDto();
                    dto.setContent(msg.getContent());
                    dto.setClassId(msg.getSchoolClass().getId());
                    // Populate sender details from the User entity associated with the message
                    if (msg.getSender() != null) {
                        dto.populateSenderDetails(msg.getSender());
                    } else {
                        // Handle cases where sender might be null (e.g., old data, system messages)
                        dto.setSenderFirstName("Unknown");
                        dto.setSenderLastName("");
                        dto.setSenderRole("System");
                    }
                    // Add timestamp if needed: dto.setTimestamp(msg.getTimestamp().toString());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(messageDtos);
    }
}
