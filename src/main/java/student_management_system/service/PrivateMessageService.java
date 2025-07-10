package student_management_system.service;

import student_management_system.model.PrivateMessage;
import student_management_system.model.User;

import java.util.List;

/**
 * Interface defining operations for managing PrivateMessage entities.
 */
public interface PrivateMessageService {

    /**
     * Saves a new private message sent from one user to another.
     * Handles text content and potentially file attachment details.
     *
     * @param sender The User sending the message.
     * @param receiver The User receiving the message.
     * @param content The text content of the message (can be null if only file).
     * @param attachmentPath The unique path/filename of the attached file (null if none).
     * @param attachmentOriginalFilename The original name of the attached file (null if none).
     * @return The saved PrivateMessage entity.
     */
    PrivateMessage saveMessage(User sender, User receiver, String content, String attachmentPath, String attachmentOriginalFilename);

    /**
     * Saves a new text-only private message.
     * Convenience method calling the full saveMessage method.
     *
     * @param sender The User sending the message.
     * @param receiver The User receiving the message.
     * @param content The text content of the message.
     * @return The saved PrivateMessage entity.
     */
    default PrivateMessage saveMessage(User sender, User receiver, String content) {
        return saveMessage(sender, receiver, content, null, null);
    }

    /**
     * Retrieves the conversation history between two users.
     *
     * @param user1 One user in the conversation.
     * @param user2 The other user in the conversation.
     * @return A List of PrivateMessage entities, ordered by timestamp.
     */
    List<PrivateMessage> getConversation(User user1, User user2);

    // Optional: Add methods for marking messages as read later
    // void markMessagesAsRead(User receiver, User sender);
}
