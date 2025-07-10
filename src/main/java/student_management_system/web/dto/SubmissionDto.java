package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * Data Transfer Object for creating or updating a Submission.
 * Includes fields for text content, an optional file upload,
 * and a flag to indicate if it's a makeup submission.
 */
@Getter
@Setter
@NoArgsConstructor
public class SubmissionDto {

    private String contentText; // Text submitted by the student

    private MultipartFile submissionFile; // Optional file uploaded by the student

    /**
     * Flag indicating whether this submission is a makeup attempt.
     * This will be set by a hidden field in the form when resubmitting
     * after a makeup request has been approved. Defaults to false.
     */
    private boolean isMakeup = false; // Added field

    // Assignment ID and Student ID will be handled by the controller/service context

    // Constructor for basic text submission (can be used for testing or specific scenarios)
    public SubmissionDto(String contentText) {
        this.contentText = contentText;
        this.isMakeup = false; // Ensure default is set
    }
}
