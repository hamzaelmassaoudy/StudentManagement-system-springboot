package student_management_system.service;

import student_management_system.model.PrivateMessage;
import student_management_system.model.User;
import student_management_system.repository.PrivateMessageRepository; // Import repository

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Use transactions

import java.util.List;

/**
 * Implementation of the PrivateMessageService interface.
 */
@Service
public class PrivateMessageServiceImpl implements PrivateMessageService {

    private static final Logger logger = LoggerFactory.getLogger(PrivateMessageServiceImpl.class);

    @Autowired
    private PrivateMessageRepository privateMessageRepository;

    @Autowired
    private UserService userService; // Might need user service for validation if not done in controller

    /**
     * Saves a new private message to the database.
     */
    @Override
    @Transactional // Ensure saving is transactional
    public PrivateMessage saveMessage(User sender, User receiver, String content, String attachmentPath, String attachmentOriginalFilename) {
        // Basic validation (can add more)
        if (sender == null || receiver == null) {
            throw new IllegalArgumentException("Sender and Receiver cannot be null.");
        }
        if (content == null && attachmentPath == null) {
             throw new IllegalArgumentException("Message content or attachment must be provided.");
        }

        // Optional: Double-check if users are friends before saving?
        // This check is already done in the controller, but could be added here for service-level integrity.
        // Set<User> senderFriends = userService.getFriends(sender.getUsername());
        // if (!senderFriends.contains(receiver)) {
        //     throw new AccessDeniedException("Cannot save message: Users are not friends.");
        // }

        PrivateMessage message = new PrivateMessage(sender, receiver, content, attachmentPath, attachmentOriginalFilename);
        // Timestamp is set automatically in the entity

        logger.debug("Saving private message from {} to {}", sender.getUsername(), receiver.getUsername());
        return privateMessageRepository.save(message);
    }

    /**
     * Retrieves the conversation history between two users.
     */
    @Override
    @Transactional(readOnly = true) // Read-only transaction is sufficient for fetching
    public List<PrivateMessage> getConversation(User user1, User user2) {
        if (user1 == null || user2 == null) {
            throw new IllegalArgumentException("Users cannot be null for fetching conversation.");
        }
        logger.debug("Fetching conversation between {} and {}", user1.getUsername(), user2.getUsername());
        // Use the custom query method defined in the repository
        return privateMessageRepository.findConversationBetweenUsers(user1, user2);
    }
}
