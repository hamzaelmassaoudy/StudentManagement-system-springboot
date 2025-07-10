package student_management_system.repository;

import student_management_system.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Role entities.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Finds a Role by its unique name.
     * @param name The role name (e.g., "ROLE_ADMIN").
     * @return An Optional containing the Role if found, or empty otherwise.
     */
    Optional<Role> findByName(String name);
}
