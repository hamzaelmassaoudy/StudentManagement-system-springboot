package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import student_management_system.model.User; // Import User

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // For formatting

/**
 * Data Transfer Object for private chat messages.
 * Enhanced to include sender details for display.
 */
@Getter
@Setter
@NoArgsConstructor
public class PrivateMessageDto {

    private Long id;
    private String content;
    private String senderUsername; // Keep for internal logic/reference
    private String receiverUsername;
    private String timestamp; // String representation for client display

    // --- Added Sender Details ---
    private String senderFirstName;
    private String senderLastName;
    private String senderProfilePicPath;
    private String senderRole;
    private String senderStudentId;
    // --- End Added Sender Details ---

    // Optional fields for file attachments
    private String attachmentUrl;
    private String attachmentOriginalFilename;

    // Constructor for sending basic text messages (used internally before broadcasting)
    public PrivateMessageDto(String content, String senderUsername, String receiverUsername) {
        this.content = content;
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        // Timestamp will be set when populated from the saved entity
    }

     // Constructor for broadcasting/fetching saved messages (including sender details)
     public PrivateMessageDto(Long id, String content, User sender, User receiver, LocalDateTime timestamp, String attachmentUrl, String attachmentOriginalFilename) {
        this.id = id;
        this.content = content;
        this.senderUsername = sender.getUsername();
        this.receiverUsername = receiver.getUsername();
        // Format timestamp nicely for display
        this.timestamp = timestamp != null ? timestamp.format(DateTimeFormatter.ISO_DATE_TIME) : null; // Example format
        this.attachmentUrl = attachmentUrl;
        this.attachmentOriginalFilename = attachmentOriginalFilename;
        // Populate sender details
        populateSenderDetails(sender);
    }

    /**
     * Populates sender details from a User object.
     * Similar to ChatMessageDto.
     * @param user The User object representing the sender.
     */
    public void populateSenderDetails(User user) {
        if (user != null) {
            this.senderFirstName = user.getFirstName();
            this.senderLastName = user.getLastName();
            this.senderProfilePicPath = user.getProfilePicturePath();
            // Determine role
            if (user.getRoles().stream().anyMatch(r -> "ROLE_TEACHER".equals(r.getName()))) {
                this.senderRole = "Teacher";
            } else if (user.getRoles().stream().anyMatch(r -> "ROLE_STUDENT".equals(r.getName()))) {
                this.senderRole = "Student";
                this.senderStudentId = user.getStudentId();
            } else {
                this.senderRole = "User";
            }
        } else {
            this.senderFirstName = "System"; // Fallback
            this.senderLastName = "";
            this.senderRole = "System";
        }
    }

    // Note: We don't include MultipartFile here.
}
