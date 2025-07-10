package student_management_system.service; // Ensure this matches your package structure

import student_management_system.model.User;
import student_management_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Important for loading lazy collections

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation for loading user-specific data for Spring Security.
 * This class bridges our User entity with Spring Security's UserDetails interface.
 */
@Service // Marks this class as a Spring service component
public class UserDetailsServiceImpl implements UserDetailsService {

    // Inject the UserRepository to fetch user data from the database
    @Autowired
    private UserRepository userRepository;

    /**
     * Locates the user based on the username provided during login.
     *
     * @param username The username identifying the user whose data is required.
     * @return A fully populated UserDetails object (username, password, authorities).
     * @throws UsernameNotFoundException if the user could not be found or the user has no GrantedAuthority.
     */
    @Override
    // @Transactional(readOnly = true) is crucial here!
    // It ensures the database session stays open long enough to load the lazy-fetched 'roles' collection
    // if you haven't set FetchType.EAGER for roles in the User entity.
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Find the user in the database by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with username: " + username));

        // 2. Convert the user's Set<Role> into a Set<GrantedAuthority> for Spring Security
        // Spring Security expects role names in the format "ROLE_ADMIN", "ROLE_TEACHER", etc.
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());

        // 3. Create and return a Spring Security UserDetails object
        // This object contains the information Spring Security needs for authentication and authorization.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),        // The username
                user.getPassword(),        // The ENCRYPTED password from the database
                user.isEnabled(),          // Is the user account enabled?
                true,                      // accountNonExpired - Hardcoded to true for now
                true,                      // credentialsNonExpired - Hardcoded to true for now
                true,                      // accountNonLocked - Hardcoded to true for now
                authorities                // The user's roles/permissions
        );
    }
}
