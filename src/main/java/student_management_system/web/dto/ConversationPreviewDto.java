// src/main/java/student_management_system/web/dto/ConversationPreviewDto.java
package student_management_system.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import student_management_system.model.ChatMessage;
import student_management_system.model.PrivateMessage;
import student_management_system.model.SchoolClass;
import student_management_system.model.User;

import java.time.LocalDateTime;

/**
 * DTO to represent a preview of a conversation (either private or class)
 * for display on the messages overview page. Includes details about the
 * conversation target and the latest message snippet.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // Convenient for controller creation
public class ConversationPreviewDto {

    // Type of conversation ("PRIVATE" or "CLASS")
    private String type;
    // ID of the target (Friend User ID or Class ID)
    private Long targetId;
    // Display name of the target (Friend Name or Class Name)
    private String targetName;
    // Relative URL for the avatar or class image (e.g., "/download/profile/...")
    private String targetImageUrl;
    // Link to the full chat page for this conversation
    private String targetLink;

    // --- Latest Message Details (can be null if no messages exist) ---
    // Display name of the sender ("You" or Friend's/Sender's first name)
    private String lastMessageSender;
    // Snippet of the message content or attachment filename
    private String lastMessageContent;
    // Timestamp of the last message
    private LocalDateTime lastMessageTimestamp;
    // Flag indicating if the last message was primarily an attachment (for private chats)
    private boolean lastMessageIsAttachment;

    // --- Helper Static Factory Methods ---

    /**
     * Creates a preview DTO for a private conversation between the currentUser and a friend.
     * It fetches details from the friend User object and the latest PrivateMessage.
     *
     * @param currentUser The user viewing the messages page.
     * @param friend The friend user associated with this conversation.
     * @param latestMessage The latest PrivateMessage exchanged (can be null).
     * @return A populated ConversationPreviewDto for the private chat.
     */
    public static ConversationPreviewDto fromPrivateChat(User currentUser, User friend, PrivateMessage latestMessage) {
        ConversationPreviewDto dto = new ConversationPreviewDto();
        dto.setType("PRIVATE");
        dto.setTargetId(friend.getId());
        dto.setTargetName(friend.getFirstName() + " " + friend.getLastName());
        // Construct the relative URL for the profile picture
        dto.setTargetImageUrl(friend.getProfilePicturePath() != null ? "/download/profile/" + friend.getProfilePicturePath() : null);
        // Construct the link to the specific private chat page
        dto.setTargetLink("/chat/private/" + friend.getUsername());

        // Populate last message details if a message exists
        if (latestMessage != null) {
            // Determine sender display name ("You" or the other person's first name)
            dto.setLastMessageSender(latestMessage.getSender().equals(currentUser) ? "You" : latestMessage.getSender().getFirstName());
            // Determine content preview: Show text content if available, otherwise show attachment info
            dto.setLastMessageContent(latestMessage.getContent() != null && !latestMessage.getContent().isBlank()
                    ? latestMessage.getContent() // Show text content
                    : (latestMessage.getAttachmentOriginalFilename() != null ? "Attachment: " + latestMessage.getAttachmentOriginalFilename() : "[Empty Message]")); // Show attachment filename or placeholder
            dto.setLastMessageTimestamp(latestMessage.getTimestamp());
            // Mark if the message was primarily an attachment (no text content)
            dto.setLastMessageIsAttachment(latestMessage.getAttachmentPath() != null && (latestMessage.getContent() == null || latestMessage.getContent().isBlank()));
        } else {
             // Set defaults if no message exists yet
             dto.setLastMessageContent("No messages yet.");
             dto.setLastMessageTimestamp(null); // No timestamp if no message
             dto.setLastMessageIsAttachment(false);
        }

        return dto;
    }

    /**
     * Creates a preview DTO for a class chat.
     * It fetches details from the SchoolClass object and the latest ChatMessage.
     *
     * @param currentUser The user viewing the messages page.
     * @param schoolClass The class associated with this chat.
     * @param latestMessage The latest ChatMessage in the class (can be null).
     * @param userRoleForLink Role hint ("teacher" or "student") used to generate the correct link.
     * @return A populated ConversationPreviewDto for the class chat.
     */
    public static ConversationPreviewDto fromClassChat(User currentUser, SchoolClass schoolClass, ChatMessage latestMessage, String userRoleForLink) {
        ConversationPreviewDto dto = new ConversationPreviewDto();
        dto.setType("CLASS");
        dto.setTargetId(schoolClass.getId());
        dto.setTargetName(schoolClass.getName());
        // Construct the relative URL for the class image
        dto.setTargetImageUrl(schoolClass.getClassImagePath() != null ? "/download/class-image/" + schoolClass.getClassImagePath() : null);

        // Construct the correct link to the class details page based on the user's role
        // Default to student link if role hint is missing or unexpected
        String linkPrefix = "teacher".equals(userRoleForLink) ? "/teacher" : "/student";
        dto.setTargetLink(linkPrefix + "/classes/details/" + schoolClass.getId());

        // Populate last message details if a message exists
        if (latestMessage != null) {
            // Determine sender display name ("You" or the sender's first name)
            dto.setLastMessageSender(latestMessage.getSender().equals(currentUser) ? "You" : latestMessage.getSender().getFirstName());
            dto.setLastMessageContent(latestMessage.getContent());
            dto.setLastMessageTimestamp(latestMessage.getTimestamp());
            dto.setLastMessageIsAttachment(false); // Class chat doesn't support attachments in this implementation
        } else {
             // Set defaults if no message exists yet
             dto.setLastMessageContent("No messages yet.");
             dto.setLastMessageTimestamp(null); // No timestamp if no message
             dto.setLastMessageIsAttachment(false);
        }

        return dto;
    }
}
