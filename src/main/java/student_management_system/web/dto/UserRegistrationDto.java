package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Import validation annotations if you add them later
// import jakarta.validation.constraints.*;

/**
 * Data Transfer Object for carrying user registration data from the web layer
 * to the service layer. Updated to include studentId for student registration.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserRegistrationDto {

    // @NotEmpty(message = "First name cannot be empty")
    private String firstName;

    // @NotEmpty(message = "Last name cannot be empty")
    private String lastName;

    // @NotEmpty(message = "Student ID cannot be empty")
    // @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Student ID must be alphanumeric") // Example validation
    private String studentId; // Added field for Student ID

    // @NotEmpty(message = "Email cannot be empty")
    // @Email(message = "Please provide a valid email address")
    private String username; // Using email as username

    // @NotEmpty(message = "Password cannot be empty")
    // @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // We don't include roles here; roles are assigned by the service logic.

    // Constructor can be useful for testing
    public UserRegistrationDto(String firstName, String lastName, String studentId, String username, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.studentId = studentId; // Added to constructor
        this.username = username;
        this.password = password;
    }
}
