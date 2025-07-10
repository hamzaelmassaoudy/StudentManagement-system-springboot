package student_management_system.model; // Ensure this matches your package structure

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an assignment created by a teacher for a specific SchoolClass.
 * Maps to the "assignments" table in the database.
 * Includes fields for an optional attached file.
 */
@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime dueDate;

    // --- New fields for attached file ---
    /**
     * Stores the path or unique identifier for an optional file attached by the teacher.
     * The actual file will be stored on the server's file system.
     */
    @Column(length = 255)
    private String attachmentPath; // e.g., "uploads/assignments/uuid_filename.pdf"

    /**
     * Stores the original filename of the optional attached file, for display purposes.
     */
    @Column(length = 255)
    private String attachmentOriginalFilename; // e.g., "assignment_instructions.pdf"
    // --- End of new fields ---


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Submission> submissions;

    /**
     * Constructor for creating a new assignment.
     * File path details are set by the service layer after file upload.
     */
    public Assignment(String title, String description, LocalDateTime dueDate, SchoolClass schoolClass) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.schoolClass = schoolClass;
    }

    // Lombok generates getters and setters.
}
