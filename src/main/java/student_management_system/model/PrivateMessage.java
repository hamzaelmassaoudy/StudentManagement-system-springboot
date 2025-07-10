package student_management_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a private message sent between two users.
 */
@Entity
@Table(name = "private_messages")
@Getter
@Setter
@NoArgsConstructor
public class PrivateMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who sent the message.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * The user who received the message.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    /**
     * The content of the message.
     */
    @Lob // Use Lob for potentially long messages
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Timestamp when the message was sent.
     */
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Flag indicating if the receiver has read the message (optional feature).
     */
    @Column(nullable = false)
    private boolean isRead = false;

    // --- Optional: File Attachment Fields ---
    /**
     * Stores the path or unique identifier for an optional file attached to the message.
     */
    @Column(length = 255, nullable = true)
    private String attachmentPath;

    /**
     * Stores the original filename of the optional attached file.
     */
    @Column(length = 255, nullable = true)
    private String attachmentOriginalFilename;
    // --- End File Attachment Fields ---


    // Constructor for text messages
    public PrivateMessage(User sender, User receiver, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

     // Constructor including file attachment details
     public PrivateMessage(User sender, User receiver, String content, String attachmentPath, String attachmentOriginalFilename) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content; // Content can be null if only file is sent
        this.attachmentPath = attachmentPath;
        this.attachmentOriginalFilename = attachmentOriginalFilename;
    }

    // Lombok generates getters/setters
}
