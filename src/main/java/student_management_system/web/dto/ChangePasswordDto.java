package student_management_system.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Import validation annotations if needed later
// import jakarta.validation.constraints.*;

/**
 * Data Transfer Object for changing a user's password.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordDto {

    // @NotEmpty(message = "Current password cannot be empty")
    private String currentPassword;

    // @NotEmpty(message = "New password cannot be empty")
    // @Size(min = 8, message = "New password must be at least 8 characters") // Example validation
    private String newPassword;

    // @NotEmpty(message = "Password confirmation cannot be empty")
    private String confirmNewPassword;

    // Note: Add validation later to ensure newPassword and confirmNewPassword match
}

