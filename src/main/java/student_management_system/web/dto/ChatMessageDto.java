package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import student_management_system.model.User; // Import User model

import java.time.LocalDateTime; // Keep if you add timestamp later

/**
 * Data Transfer Object for WebSocket chat messages.
 * Used for sending messages between client and server.
 * Enhanced to include more sender details.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatMessageDto {

    private String content; // The message text
    private Long classId; // ID of the class this message belongs to

    // --- Enhanced Sender Information ---
    private String senderUsername; // Keep username (e.g., for internal use or fallback)
    private String senderFirstName;
    private String senderLastName;
    private String senderProfilePicPath; // Relative path for profile picture
    private String senderRole; // e.g., "Student", "Teacher"
    private String senderStudentId; // Only populated if sender is a student

    // Optional: Add timestamp if needed on client side immediately
    // private String timestamp;

    /**
     * Populates the DTO with sender details from a User object.
     * @param user The User object representing the sender.
     */
    public void populateSenderDetails(User user) {
        if (user != null) {
            this.senderUsername = user.getUsername();
            this.senderFirstName = user.getFirstName();
            this.senderLastName = user.getLastName();
            this.senderProfilePicPath = user.getProfilePicturePath(); // Get the path
            // Determine role (simplified)
            if (user.getRoles().stream().anyMatch(r -> "ROLE_TEACHER".equals(r.getName()))) {
                this.senderRole = "Teacher";
            } else if (user.getRoles().stream().anyMatch(r -> "ROLE_STUDENT".equals(r.getName()))) {
                this.senderRole = "Student";
                this.senderStudentId = user.getStudentId(); // Get student ID only for students
            } else {
                this.senderRole = "User"; // Fallback
            }
        } else {
            // Handle case where user might be null (e.g., system message)
            this.senderFirstName = "System";
            this.senderLastName = "";
            this.senderRole = "System";
        }
    }

     // Basic constructor (can be removed if populateSenderDetails is always used)
     public ChatMessageDto(String content, String senderUsername, Long classId) {
         this.content = content;
         this.senderUsername = senderUsername; // Keep this for basic construction if needed
         this.classId = classId;
     }
}
