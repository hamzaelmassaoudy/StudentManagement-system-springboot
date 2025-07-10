package student_management_system.config; // Ensure this matches your package structure

import student_management_system.model.Role;
import student_management_system.model.User;
import student_management_system.repository.RoleRepository;
import student_management_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Optional but good practice

import java.util.Optional;
// Import Set if needed for checking roles later, though not strictly necessary for this basic setup
// import java.util.Set; 

/**
 * Initializes essential data on application startup.
 * Creates default roles and an admin user if they don't exist.
 * Implements CommandLineRunner so its run() method executes after the application context is loaded.
 */
@Component // Marks this as a Spring component to be managed by the container
public class DataInitializer implements CommandLineRunner {

    @Autowired // Inject the RoleRepository
    private RoleRepository roleRepository;

    @Autowired // Inject the UserRepository
    private UserRepository userRepository;

    @Autowired // Inject the PasswordEncoder bean defined in SecurityConfig
    private PasswordEncoder passwordEncoder;

    /**
     * This method will be executed automatically upon application startup.
     * It checks for and creates necessary roles and the default admin user.
     * @param args Incoming command line arguments (not used here).
     * @throws Exception if any error occurs during initialization.
     */
    @Override
    @Transactional // Wrap in a transaction to ensure atomicity, especially if creating multiple entities
    public void run(String... args) throws Exception {
        System.out.println("Starting Data Initialization...");

        // --- Create Roles if they don't exist ---
        createRoleIfNotFound("ROLE_ADMIN");
        createRoleIfNotFound("ROLE_TEACHER");
        createRoleIfNotFound("ROLE_STUDENT");

        // --- Create Admin User if not exists ---
        // Check if a user with the NEW username "admin@dpu.com" already exists
        String adminUsername = "admin@dpu.com";
        String adminPassword = "admin123123";

        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            System.out.println("Creating default admin user: " + adminUsername);
            // Create a new User object with the new credentials
            User adminUser = new User(
                adminUsername,                             // New Username
                passwordEncoder.encode(adminPassword),    // New Encoded Password
                "Admin",                                  // First Name (you can change this too if needed)
                "DPU"                                     // Last Name (e.g., "DPU" or "System")
            );

            // Find the ROLE_ADMIN role (should exist after createRoleIfNotFound)
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Error: ROLE_ADMIN not found. Initialization failed."));

            // Add the admin role to the user
            adminUser.addRole(adminRole);
            // Ensure the user account is enabled
            adminUser.setEnabled(true);

            // Save the new admin user to the database
            userRepository.save(adminUser);
            System.out.println("Created default admin user '" + adminUsername + "' with password '" + adminPassword + "'");
        } else {
            System.out.println("Admin user '" + adminUsername + "' already exists.");
        }

        System.out.println("Data Initialization finished.");
    }

    /**
     * Helper method to check if a role exists and create it if it doesn't.
     * @param roleName The name of the role to check/create (e.g., "ROLE_ADMIN").
     */
    private void createRoleIfNotFound(String roleName) {
        Optional<Role> role = roleRepository.findByName(roleName);
        if (role.isEmpty()) {
            roleRepository.save(new Role(roleName));
            System.out.println("Created role: " + roleName);
        } else {
             System.out.println("Role already exists: " + roleName);
        }
    }
}
