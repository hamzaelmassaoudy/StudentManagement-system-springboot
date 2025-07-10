// src/main/java/student_management_system/repository/PrivateMessageRepository.java
package student_management_system.repository;

import student_management_system.model.PrivateMessage;
import student_management_system.model.User;
import org.springframework.data.domain.Pageable; // Import Pageable for limit
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PrivateMessage entities.
 * Includes methods for fetching conversation history and the latest message.
 */
@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {

    /**
     * Finds all private messages exchanged between two specific users, ordered by timestamp ascending.
     *
     * @param user1 One user in the conversation.
     * @param user2 The other user in the conversation.
     * @return A list of PrivateMessage entities representing the conversation history.
     */
    @Query("SELECT pm FROM PrivateMessage pm WHERE " +
           "(pm.sender = :user1 AND pm.receiver = :user2) OR " +
           "(pm.sender = :user2 AND pm.receiver = :user1) " +
           "ORDER BY pm.timestamp ASC")
    List<PrivateMessage> findConversationBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    /**
     * Finds the single most recent private message exchanged between two users using an explicit JPQL query.
     * This replaces the problematic derived query method name.
     *
     * @param user1 One user in the conversation.
     * @param user2 The other user in the conversation.
     * @param pageable A Pageable object configured to limit the result to 1 and sort by timestamp descending.
     * @return A List containing the latest PrivateMessage, or an empty list if no messages exist.
     * (Note: Using List<PrivateMessage> with Pageable is standard, get the Optional from the result).
     */
    @Query("SELECT pm FROM PrivateMessage pm WHERE " +
           "(pm.sender = :user1 AND pm.receiver = :user2) OR " +
           "(pm.sender = :user2 AND pm.receiver = :user1) " +
           "ORDER BY pm.timestamp DESC")
    List<PrivateMessage> findLatestConversationMessageBetweenUsers(@Param("user1") User user1, @Param("user2") User user2, Pageable pageable);

    /**
     * Convenience method to get the single latest message as an Optional.
     * Uses the findLatestConversationMessageBetweenUsers method with a Pageable limit of 1.
     *
     * @param user1 One user in the conversation.
     * @param user2 The other user in the conversation.
     * @return An Optional containing the latest PrivateMessage, or empty if no messages exist.
     */
    default Optional<PrivateMessage> findTopPrivateMessageBetweenUsers(User user1, User user2) {
        Pageable limitOne = Pageable.ofSize(1); // Request only the top 1 result
        List<PrivateMessage> latest = findLatestConversationMessageBetweenUsers(user1, user2, limitOne);
        return latest.isEmpty() ? Optional.empty() : Optional.of(latest.get(0));
    }


    // Optional: Method to find unread messages for a user
    // List<PrivateMessage> findByReceiverAndIsReadFalse(User receiver);

}
