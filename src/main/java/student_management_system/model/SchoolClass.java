package student_management_system.model; // Ensure this matches your package structure

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a class taught by a teacher, which students can enroll in.
 * Maps to the "classes" table in the database.
 * Note: Named SchoolClass to avoid conflict with java.lang.Class.
 */
@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, unique = true, length = 20)
    private String classCode;

    @Column(length = 60)
    private String joinPassword;

    // *** ADDED: Field for class image path ***
    @Column(length = 255, nullable = true)
    private String classImagePath; // Stores filename like "uuid_math_icon.png"
    // *** END ADDED ***

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
            name = "classes_students",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<User> students = new HashSet<>();

    @OneToMany(mappedBy = "schoolClass", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Assignment> assignments;

    // Constructors remain the same

    public SchoolClass(String name, String description, User teacher, String classCode) {
        this.name = name;
        this.description = description;
        this.teacher = teacher;
        this.classCode = classCode;
    }

    // Helper methods remain the same
    public void addStudent(User student) {
        this.students.add(student);
        student.getEnrolledClasses().add(this);
    }

    public void removeStudent(User student) {
        this.students.remove(student);
        student.getEnrolledClasses().remove(this);
    }

    // Lombok generates getters and setters.
}
