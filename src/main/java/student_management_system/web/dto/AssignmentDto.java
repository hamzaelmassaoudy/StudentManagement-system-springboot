package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile; // Import MultipartFile

import java.time.LocalDateTime;

// Add validation annotations later (@NotEmpty, @NotNull, @FutureOrPresent)
// import jakarta.validation.constraints.*;

/**
 * Data Transfer Object for creating or updating an Assignment.
 * Includes a field for an optional file attachment.
 */
@Getter
@Setter
@NoArgsConstructor
public class AssignmentDto {

    // @NotEmpty(message = "Assignment title cannot be empty")
    private String title;

    private String description;

    // @NotNull(message = "Due date cannot be empty")
    // @FutureOrPresent(message = "Due date must be in the present or future") // Optional validation
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) // Helps Spring bind String from form input to LocalDateTime
    private LocalDateTime dueDate;

    // --- New field for file upload ---
    /**
     * Holds the optional file uploaded by the teacher when creating/updating the assignment.
     * Spring MVC will automatically bind the uploaded file from the form to this field.
     */
    private MultipartFile attachmentFile;
    // --- End of new field ---


    // Constructor without file - might still be useful
    public AssignmentDto(String title, String description, LocalDateTime dueDate) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
    }

    // Lombok generates getters/setters for attachmentFile as well.
}
