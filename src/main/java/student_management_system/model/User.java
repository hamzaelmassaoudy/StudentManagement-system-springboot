package student_management_system.model; // Correct package based on your structure

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode; // Import for equals/hashCode

import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * Represents a user in the system (can be Admin, Teacher, or Student).
 * Maps to the "users" table in the database.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "username", name = "uk_user_username"),
    @UniqueConstraint(columnNames = "studentId", name = "uk_user_studentid")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Important for ManyToMany self-reference
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include // Include ID in equals/hashCode
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username; // Email

    @Column(unique = true, length = 50, nullable = true)
    private String studentId;

    @Column(nullable = false)
    private String password; // Store ENCRYPTED password

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false)
    private boolean enabled = true;

    // --- MODIFIED LINE ---
    // Explicitly initialize to null. Although @Column(nullable=true) should suffice,
    // this makes the default Java value explicit.
    @Column(length = 255, nullable = true)
    private String profilePicturePath = null;
    // --- END MODIFIED LINE ---

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // --- Relationships specific to roles ---

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<SchoolClass> taughtClasses = new HashSet<>();

    @ManyToMany(mappedBy = "students", fetch = FetchType.LAZY)
    private Set<SchoolClass> enrolledClasses = new HashSet<>();

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Submission> submissions;

    // --- Friends Relationship (Self-Referencing ManyToMany) ---
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_friends", // Name of the join table
        joinColumns = @JoinColumn(name = "user_id"), // Column linking to this user
        inverseJoinColumns = @JoinColumn(name = "friend_id") // Column linking to the friend user
    )
    private Set<User> friends = new HashSet<>();

    @ManyToMany(mappedBy = "friends", fetch = FetchType.LAZY)
    private Set<User> friendOf = new HashSet<>();
    // --- END Friends Relationship ---


    // Constructors
    public User(String username, String password, String firstName, String lastName) {
        this.username = username;
        this.password = password; // Raw password, MUST be encoded by the service layer before saving
        this.firstName = firstName;
        this.lastName = lastName;
        // profilePicturePath will default to null due to the field initialization above
    }

     public User(String username, String studentId, String password, String firstName, String lastName) {
        this.username = username;
        this.studentId = studentId;
        this.password = password; // Raw password, MUST be encoded
        this.firstName = firstName;
        this.lastName = lastName;
        // profilePicturePath will default to null due to the field initialization above
    }

    // Helper methods
    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void addFriend(User friend) {
        this.friends.add(friend);
        friend.getFriends().add(this); // Manage both sides
    }

    public void removeFriend(User friend) {
        this.friends.remove(friend);
        friend.getFriends().remove(this); // Manage both sides
    }

    // Note: Lombok generates getters and setters.
    // Equals and HashCode based on ID is important for collections.
}
