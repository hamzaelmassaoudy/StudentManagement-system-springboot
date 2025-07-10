package student_management_system.model; // Make sure this matches your package structure

import jakarta.persistence.*; // For JPA annotations
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a user role within the system (e.g., ROLE_ADMIN, ROLE_TEACHER, ROLE_STUDENT).
 * This entity maps to the "roles" table in the database.
 */
@Entity // Specifies that this class is a JPA entity (maps to a database table)
@Table(name = "roles") // Specifies the name of the database table
@Getter // Lombok annotation to automatically generate getter methods for all fields
@Setter // Lombok annotation to automatically generate setter methods for all fields
@NoArgsConstructor // Lombok annotation to automatically generate a no-argument constructor
public class Role {

    /**
     * The unique identifier for the role.
     * Generated automatically by the database (identity strategy).
     */
    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Configures the ID generation strategy (auto-increment for MySQL)
    private Long id;

    /**
     * The name of the role (e.g., "ROLE_ADMIN", "ROLE_TEACHER").
     * Must be unique and cannot be null.
     * Spring Security typically expects roles to start with "ROLE_".
     */
    @Column(nullable = false, unique = true, length = 50) // Maps this field to a database column with constraints
    private String name;

    /**
     * Constructor to create a Role with a specific name.
     * Useful for initializing roles.
     * @param name The name of the role.
     */
    public Role(String name) {
        this.name = name;
    }

    /**
     * Provides a string representation of the Role object, primarily showing the name.
     * Useful for logging and debugging.
     * @return The name of the role.
     */
    @Override
    public String toString() {
        return this.name;
    }
}
