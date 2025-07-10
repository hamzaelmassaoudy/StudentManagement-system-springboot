package student_management_system.controller;

import student_management_system.model.User;
import student_management_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller Advice to add common model attributes to all controllers.
 * Specifically, adds the currently logged-in User object if authenticated.
 */
@ControllerAdvice // Apply to all controllers
public class GlobalControllerAdvice {

    @Autowired
    private UserService userService; // Inject UserService to fetch the full User object

    /**
     * Adds the 'loggedInUser' attribute to the model for all requests
     * if the user is authenticated.
     *
     * @return The full User object corresponding to the logged-in principal, or null if not authenticated.
     */
    @ModelAttribute("loggedInUser") // The name used in Thymeleaf templates (#vars.loggedInUser or ${loggedInUser})
    public User addLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if user is authenticated and not anonymous
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            Object principal = authentication.getPrincipal();
            String username = null;

            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                // This case might happen depending on security config, but usually UserDetails is used
                username = (String) principal;
            }

            if (username != null) {
                // Fetch the full User object from the database using the username
                // Return null if not found (shouldn't happen for authenticated user)
                return userService.findUserByUsername(username).orElse(null);
            }
        }
        // Return null if not authenticated or principal is not recognized
        return null;
    }
}
