package student_management_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a friend request sent from one user (sender) to another (receiver).
 */
@Entity
@Table(name = "friend_requests", uniqueConstraints = {
    // Ensure a user cannot send multiple pending requests to the same receiver
    @UniqueConstraint(columnNames = {"sender_id", "receiver_id"}, name = "uk_friend_request_sender_receiver")
})
@Getter
@Setter
@NoArgsConstructor
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who sent the friend request.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * The user who received the friend request.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    /**
     * The status of the friend request (PENDING, ACCEPTED, REJECTED).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendRequestStatus status = FriendRequestStatus.PENDING; // Default to PENDING

    /**
     * Timestamp when the request was created.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Timestamp when the request was last updated (e.g., accepted/rejected).
     */
    @Column
    private LocalDateTime updatedAt;

    // Enum for request status
    public enum FriendRequestStatus {
        PENDING,
        ACCEPTED,
        REJECTED
        // Could add BLOCKED later if needed
    }

    // Constructor
    public FriendRequest(User sender, User receiver) {
        this.sender = sender;
        this.receiver = receiver;
        this.status = FriendRequestStatus.PENDING; // Explicitly set default
    }

    // --- Lifecycle Callbacks ---
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
