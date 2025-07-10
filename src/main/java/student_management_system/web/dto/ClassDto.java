package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

// Add validation annotations later (@NotEmpty, @Size)
// import jakarta.validation.constraints.*;

/**
 * Data Transfer Object for creating or updating a SchoolClass.
 * Includes field for optional class image upload and join password.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClassDto {

    // @NotEmpty(message = "Class name cannot be empty")
    private String name;

    private String description;

    // Field for class image upload
    private MultipartFile classImageFile;

    // *** ADDED: Field for optional join password ***
    // @Size(min = 4, max = 20, message = "Password must be between 4 and 20 characters if provided") // Example validation
    private String joinPassword; // Optional password for students to join
    // *** END ADDED ***


    public ClassDto(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Lombok generates getters/setters including for joinPassword
}
