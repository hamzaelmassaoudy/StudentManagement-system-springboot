package student_management_system.repository;

import student_management_system.model.Role;
import student_management_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import Query
import org.springframework.data.repository.query.Param; // Import Param
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByRoles_Name(String roleName);

    /**
     * Finds users whose username, first name, or last name contains the given search term, ignoring case.
     *
     * @param usernameSearch Term to search in username.
     * @param firstNameSearch Term to search in first name.
     * @param lastNameSearch Term to search in last name.
     * @return A list of matching users.
     */
     // Using derived query method name
     List<User> findByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
         String usernameSearch, String firstNameSearch, String lastNameSearch
     );

     // --- OR using @Query for potentially more complex searches later ---
     /*
     @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(concat('%', :searchTerm, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(concat('%', :searchTerm, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(concat('%', :searchTerm, '%'))")
     List<User> searchUsers(@Param("searchTerm") String searchTerm);
     */

}
