package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Import validation annotations if needed later
// import jakarta.validation.constraints.*;

/**
 * Data Transfer Object for updating basic user profile settings.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserSettingsDto {

    // @NotEmpty(message = "First name cannot be empty")
    private String firstName;

    // @NotEmpty(message = "Last name cannot be empty")
    private String lastName;

    // Include username to potentially pre-populate the form (read-only)
    private String username;

    // Constructor for easy initialization in the controller
    public UserSettingsDto(String firstName, String lastName, String username) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
    }
}
