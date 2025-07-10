package student_management_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "makeup_requests")
@Getter
@Setter
@NoArgsConstructor
public class MakeupRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false, unique = true) // Existing submission link
    private Submission submission;

    // --- ADDED FIELD TO MATCH DATABASE ---
    /**
     * Explicitly mapping the ID of the original submission.
     * This seems redundant given the 'submission' field above,
     * but is added to match the database schema error.
     * Assumes the DB column is named 'original_submission_id' and is NOT NULL.
     */
    @Column(name = "original_submission_id", nullable = false) // Map to the column name from the error
    private Long originalSubmissionId;
    // --- END ADDED FIELD ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MakeupRequestStatus status = MakeupRequestStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(nullable = true)
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_teacher_id", nullable = true)
    private User reviewedByTeacher; // Teacher who approved/rejected

    @Lob
    @Column(columnDefinition = "TEXT", nullable = true)
    private String teacherComment; // Optional comment from teacher

    public enum MakeupRequestStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    // Updated Constructor
    public MakeupRequest(Submission submission, User student, String reason) {
        this.submission = submission;
        this.student = student;
        this.reason = reason;
        this.status = MakeupRequestStatus.PENDING;
        // --- SET THE NEW FIELD ---
        if (submission != null) {
            this.originalSubmissionId = submission.getId(); // Set the ID from the submission object
        } else {
             // Handle case where submission might be null, though it shouldn't be based on logic
             throw new IllegalArgumentException("Submission cannot be null when creating a MakeupRequest.");
        }
        // --- END SET THE NEW FIELD ---
    }

    @PreUpdate
    protected void onUpdate() {
        if (status == MakeupRequestStatus.APPROVED || status == MakeupRequestStatus.REJECTED) {
            reviewedAt = LocalDateTime.now();
        }
    }
}
