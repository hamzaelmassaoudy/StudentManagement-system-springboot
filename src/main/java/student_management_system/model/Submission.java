package student_management_system.model; // Ensure this matches your package structure

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
// Add this import
import java.util.List; // Keep if you have other OneToMany/ManyToMany

/**
 * Represents a student's submission for a specific assignment.
 * Maps to the "submissions" table in the database.
 * ADDED: isMakeupSubmission and isSuperseded fields.
 * UPDATED: Added OneToOne mapping to MakeupRequest for cascade delete.
 */
@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(nullable = false)
    private LocalDateTime submissionDate = LocalDateTime.now();

    @Lob // Use Lob for potentially large text submissions
    @Column(columnDefinition = "TEXT")
    private String contentText; // Text content submitted by the student

    @Column(length = 255)
    private String filePath; // Path to the uploaded file on the server

    @Column(length = 255)
    private String originalFilename; // Original name of the uploaded file

    // --- Grading Fields ---
    @Column(length = 20, nullable = true) // e.g., "85.5", "A-", "Pass" - flexible string representation
    private String grade;

    @Column(nullable = true) // Store the numerical score for calculations (e.g., eligibility)
    private Double numericalGrade;

    @Lob // Use Lob for potentially long feedback
    @Column(columnDefinition = "TEXT")
    private String feedback; // Feedback provided by the teacher

    @Column(nullable = true) // Timestamp when the submission was graded
    private LocalDateTime gradedDate;

    // --- Makeup and Superseded Status Fields ---
    @Column(nullable = false)
    private boolean isMakeupSubmission = false;

    @Column(nullable = false)
    private boolean isSuperseded = false;

    // --- ADDED: Relationship to MakeupRequest for Cascade Delete ---
    /**
     * Represents the makeup request associated with this submission, if one exists.
     * cascade = CascadeType.REMOVE: Ensures that if this Submission is deleted,
     * the associated MakeupRequest is deleted first.
     * orphanRemoval = true: Ensures the MakeupRequest is deleted if the link
     * from Submission is broken (e.g., setMakeupRequest(null)).
     * mappedBy = "submission": Indicates the 'submission' field in MakeupRequest
     * owns the foreign key relationship.
     */
    @OneToOne(mappedBy = "submission", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private MakeupRequest makeupRequest;
    // --- END ADDED Relationship ---


    /**
     * Constructor for creating a new submission instance.
     * Initializes core fields and sets default values for status flags.
     *
     * @param student The student submitting.
     * @param assignment The assignment being submitted for.
     * @param contentText The text content of the submission (can be null).
     * @param filePath The server path to the uploaded file (can be null).
     * @param originalFilename The original name of the uploaded file (can be null).
     */
    public Submission(User student, Assignment assignment, String contentText, String filePath, String originalFilename) {
        this.student = student;
        this.assignment = assignment;
        this.contentText = contentText;
        this.filePath = filePath;
        this.originalFilename = originalFilename;
        this.isMakeupSubmission = false; // Explicitly false for new, non-makeup submissions
        this.isSuperseded = false;       // Explicitly false for new submissions
        // submissionDate is set automatically by default field initializer
    }

    // Lombok generates getters and setters for all fields, including the new boolean flags.
}
