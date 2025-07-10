// src/main/java/student_management_system/repository/ChatMessageRepository.java
package student_management_system.repository;

import student_management_system.model.ChatMessage;
import student_management_system.model.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Import Optional

/**
 * Repository interface for ChatMessage entities (class chat).
 * Includes methods for fetching message history and the latest message for a class.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Finds all chat messages belonging to a specific SchoolClass,
     * ordered by their timestamp in ascending order (oldest first).
     *
     * @param schoolClass The SchoolClass entity to fetch messages for.
     * @return A List of ChatMessage entities for the class, ordered by timestamp.
     */
    List<ChatMessage> findBySchoolClassOrderByTimestampAsc(SchoolClass schoolClass);

    /**
     * Finds the single most recent chat message for a specific SchoolClass.
     * Uses Spring Data JPA's derived query method feature. It looks for the top (first)
     * result ordered by timestamp descending for the given class.
     *
     * @param schoolClass The SchoolClass entity.
     * @return An Optional containing the latest ChatMessage, or empty if no messages exist.
     */
    Optional<ChatMessage> findTopBySchoolClassOrderByTimestampDesc(SchoolClass schoolClass);

}
