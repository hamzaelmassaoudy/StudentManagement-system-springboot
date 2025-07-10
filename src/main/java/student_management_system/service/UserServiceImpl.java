package student_management_system.service;

import student_management_system.model.FriendRequest; // Import FriendRequest
import student_management_system.model.Role;
import student_management_system.model.User;
import student_management_system.repository.FriendRequestRepository; // Import FriendRequestRepository
import student_management_system.repository.RoleRepository;
import student_management_system.repository.UserRepository;
import student_management_system.web.dto.ChangePasswordDto;
import student_management_system.web.dto.UserRegistrationDto;
import student_management_system.web.dto.UserSettingsDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the UserService interface.
 * Handles business logic for user management including updates, deletions, and friendships.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    // --- NEW: Inject FriendRequestRepository ---
    @Autowired
    private FriendRequestRepository friendRequestRepository;
    // --- END NEW ---

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${file.profile-picture-upload-dir}")
    private String profileUploadDir;

    // --- Existing Methods ---
    // ... (findUserByUsername, findUserById, createTeacher, findAllTeachers, updateTeacher, deleteTeacher, createStudent) ...
    // ... (updateUserSettings, changeUserPassword, updateProfilePicturePath, deleteProfilePictureFile) ...
    // --- Friend Management Methods (addFriend, removeFriend, getFriends, searchPotentialFriends) ---
    // --- These remain largely the same, but addFriend/removeFriend are now primarily called by accept/reject/remove actions ---

    @Override
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    @Override
    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }
    @Override
    @Transactional
    public User createTeacher(UserRegistrationDto registrationDto) {
        if (userRepository.findByUsername(registrationDto.getUsername()).isPresent()) {
            throw new RuntimeException("Email already exists: " + registrationDto.getUsername());
        }
        Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                .orElseThrow(() -> new RuntimeException("Error: ROLE_TEACHER not found."));
        User teacher = new User();
        teacher.setFirstName(registrationDto.getFirstName());
        teacher.setLastName(registrationDto.getLastName());
        teacher.setUsername(registrationDto.getUsername());
        teacher.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        teacher.setEnabled(true);
        teacher.addRole(teacherRole);
        return userRepository.save(teacher);
    }
    @Override
    public List<User> findAllTeachers() {
        return userRepository.findByRoles_Name("ROLE_TEACHER");
    }
    @Override
    @Transactional
    public User updateTeacher(Long id, UserRegistrationDto dto) {
        User existingTeacher = findUserById(id)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found with ID: " + id));
        boolean isTeacher = existingTeacher.getRoles().stream()
                                .anyMatch(role -> "ROLE_TEACHER".equals(role.getName()));
        if (!isTeacher) {
             throw new IllegalArgumentException("User with ID " + id + " is not a teacher.");
        }
        existingTeacher.setFirstName(dto.getFirstName());
        existingTeacher.setLastName(dto.getLastName());
        if (StringUtils.hasText(dto.getPassword())) {
            existingTeacher.setPassword(passwordEncoder.encode(dto.getPassword()));
            logger.info("Updating password for teacher ID: {}", id);
        } else {
             logger.info("Password not provided for update for teacher ID: {}. Keeping existing password.", id);
        }
        return userRepository.save(existingTeacher);
    }
    @Override
    @Transactional
    public void deleteTeacher(Long id) {
         User teacherToDelete = findUserById(id)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found with ID: " + id));
         boolean isTeacher = teacherToDelete.getRoles().stream()
                                .anyMatch(role -> "ROLE_TEACHER".equals(role.getName()));
        if (!isTeacher) {
             throw new IllegalArgumentException("User with ID " + id + " is not a teacher and cannot be deleted via this method.");
        }
        logger.warn("Deleting teacher ID: {}. WARNING: Associated data might be deleted due to cascade settings.", id);
        userRepository.deleteById(id);
    }
    @Override
    @Transactional
    public User createStudent(UserRegistrationDto registrationDto) {
        if (userRepository.findByUsername(registrationDto.getUsername()).isPresent()) {
            throw new RuntimeException("Email already registered: " + registrationDto.getUsername());
        }
        Role studentRole = roleRepository.findByName("ROLE_STUDENT")
                .orElseThrow(() -> new RuntimeException("Error: ROLE_STUDENT not found."));
        User student = new User();
        student.setFirstName(registrationDto.getFirstName());
        student.setLastName(registrationDto.getLastName());
        student.setStudentId(registrationDto.getStudentId());
        student.setUsername(registrationDto.getUsername());
        student.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        student.setEnabled(true);
        student.addRole(studentRole);
        try {
             return userRepository.save(student);
        } catch (DataIntegrityViolationException e) {
             if (e.getMessage() != null && e.getMessage().toLowerCase().contains("uk_user_studentid")) {
                 throw new RuntimeException("Student ID already registered: " + registrationDto.getStudentId());
             } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("uk_user_username")) {
                 throw new RuntimeException("Email already registered: " + registrationDto.getUsername());
             } else {
                 logger.error("Data integrity violation during student registration for {}", registrationDto.getUsername(), e);
                 throw new RuntimeException("Could not register student due to a data conflict.");
             }
        }
    }
    @Override
    @Transactional
    public User updateUserSettings(String username, UserSettingsDto settingsDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        user.setFirstName(settingsDto.getFirstName());
        user.setLastName(settingsDto.getLastName());
        logger.info("Attempting to save updated settings for user: {}", username);
        User savedUser = userRepository.save(user);
        logger.info("Successfully saved updated settings for user: {}", username);
        return savedUser;
    }
    @Override
    @Transactional
    public void changeUserPassword(String username, ChangePasswordDto passwordDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        if (!passwordEncoder.matches(passwordDto.getCurrentPassword(), user.getPassword())) {
            throw new AccessDeniedException("Incorrect current password provided.");
        }
        if (!Objects.equals(passwordDto.getNewPassword(), passwordDto.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match.");
        }
        if (passwordEncoder.matches(passwordDto.getNewPassword(), user.getPassword())) {
             throw new IllegalArgumentException("New password cannot be the same as the current password.");
        }
        user.setPassword(passwordEncoder.encode(passwordDto.getNewPassword()));
        logger.info("Attempting to save password change for user: {}", username);
        userRepository.save(user);
        logger.info("Password changed successfully for user: {}", username);
    }
    @Override
    @Transactional
    public void updateProfilePicturePath(String username, String newFilename) {
         User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        String oldFilename = user.getProfilePicturePath();
        user.setProfilePicturePath(newFilename);
        logger.info("Attempting to save new profile picture path '{}' for user: {}", newFilename, username);
        try {
             userRepository.save(user);
             logger.info("Successfully saved new profile picture path in DB for user: {}", username);
             deleteProfilePictureFile(oldFilename);
        } catch (Exception e) {
             logger.error("Failed to save profile picture path for user {}: {}", username, e.getMessage(), e);
             throw new RuntimeException("Failed to update profile picture information.", e);
        }
    }
    private void deleteProfilePictureFile(String filename) {
        if (!StringUtils.hasText(filename)) return;
        try {
            Path filePath = Paths.get(profileUploadDir).resolve(filename).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Deleted old profile picture file: {}", filePath.toString());
            } else {
                 logger.warn("Old profile picture file not found for deletion: {}", filePath.toString());
            }
        } catch (IOException e) {
            logger.error("Error deleting old profile picture file '{}': {}", filename, e.getMessage(), e);
        } catch (Exception e) {
             logger.error("Unexpected error deleting old profile picture file '{}': {}", filename, e.getMessage(), e);
        }
    }
    @Override
    @Transactional
    public void addFriend(String currentUsername, String friendUsername) {
        if (currentUsername.equals(friendUsername)) {
            throw new IllegalArgumentException("Cannot add yourself as a friend.");
        }
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found: " + currentUsername));
        User friendUser = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new EntityNotFoundException("User to add as friend not found: " + friendUsername));
        if (currentUser.getFriends().contains(friendUser)) {
            throw new IllegalArgumentException("You are already friends with " + friendUsername);
        }
        currentUser.addFriend(friendUser);
        userRepository.save(currentUser);
        userRepository.save(friendUser);
        logger.info("User '{}' added user '{}' as a friend.", currentUsername, friendUsername);
    }
    @Override
    @Transactional
    public void removeFriend(String currentUsername, String friendUsername) {
         User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found: " + currentUsername));
        User friendUser = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new EntityNotFoundException("Friend user not found: " + friendUsername));
        if (!currentUser.getFriends().contains(friendUser)) {
             throw new IllegalArgumentException("You are not friends with " + friendUsername);
        }
        currentUser.removeFriend(friendUser);
        userRepository.save(currentUser);
        userRepository.save(friendUser);
        logger.info("User '{}' removed user '{}' as a friend.", currentUsername, friendUsername);
    }
    @Override
    @Transactional(readOnly = true)
    public Set<User> getFriends(String username) {
         User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        return user.getFriends();
    }
    @Override
    @Transactional(readOnly = true)
    public List<User> searchPotentialFriends(String currentUsername, String searchTerm) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found: " + currentUsername));
        String term = searchTerm.trim();
        if (term.isEmpty()) return List.of();
        logger.debug("Searching potential friends for '{}' with term '{}'", currentUsername, term);
        List<User> potentialMatches = userRepository.findByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(term, term, term);
        logger.debug("Found {} potential matches initially.", potentialMatches.size());
        Set<User> currentFriends = currentUser.getFriends();
        logger.debug("Current user '{}' has {} friends.", currentUsername, currentFriends.size());
        List<User> results = potentialMatches.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> !currentFriends.contains(user))
                .collect(Collectors.toList());
        logger.debug("Filtered results count: {}", results.size());
        return results;
    }

    // --- NEW Friend Request Method Implementations ---

    @Override
    @Transactional
    public void sendFriendRequest(String senderUsername, String receiverUsername) {
        if (senderUsername.equals(receiverUsername)) {
            throw new IllegalArgumentException("Cannot send a friend request to yourself.");
        }

        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new EntityNotFoundException("Sender user not found: " + senderUsername));
        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new EntityNotFoundException("Receiver user not found: " + receiverUsername));

        // Check if already friends
        if (sender.getFriends().contains(receiver)) {
            throw new IllegalArgumentException("You are already friends with " + receiverUsername);
        }

        // Check if a pending request already exists between them (in either direction)
        if (friendRequestRepository.findPendingRequestBetweenUsers(sender, receiver).isPresent()) {
            throw new IllegalArgumentException("A friend request is already pending between you and " + receiverUsername);
        }

        // Create and save the new request
        FriendRequest newRequest = new FriendRequest(sender, receiver);
        friendRequestRepository.save(newRequest);
        logger.info("Friend request sent from '{}' to '{}'", senderUsername, receiverUsername);
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long requestId, String receiverUsername) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Friend request not found with ID: " + requestId));

        User receiver = userRepository.findByUsername(receiverUsername)
                 .orElseThrow(() -> new EntityNotFoundException("Receiver user not found: " + receiverUsername));

        // Verify the correct user is accepting and the request is pending
        if (!request.getReceiver().equals(receiver)) {
            throw new AccessDeniedException("You are not authorized to accept this friend request.");
        }
        if (request.getStatus() != FriendRequest.FriendRequestStatus.PENDING) {
            throw new IllegalArgumentException("This friend request is not pending (Status: " + request.getStatus() + ").");
        }

        // Update request status
        request.setStatus(FriendRequest.FriendRequestStatus.ACCEPTED);
        friendRequestRepository.save(request);

        // Add the friendship using the existing method
        addFriend(request.getSender().getUsername(), request.getReceiver().getUsername());

        logger.info("Friend request ID {} accepted by '{}'", requestId, receiverUsername);
    }

    @Override
    @Transactional
    public void rejectFriendRequest(Long requestId, String receiverUsername) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Friend request not found with ID: " + requestId));

        User receiver = userRepository.findByUsername(receiverUsername)
                 .orElseThrow(() -> new EntityNotFoundException("Receiver user not found: " + receiverUsername));

        // Verify the correct user is rejecting and the request is pending
        if (!request.getReceiver().equals(receiver)) {
            throw new AccessDeniedException("You are not authorized to reject this friend request.");
        }
        if (request.getStatus() != FriendRequest.FriendRequestStatus.PENDING) {
            throw new IllegalArgumentException("This friend request is not pending (Status: " + request.getStatus() + ").");
        }

        // Option 1: Set status to REJECTED (keeps a record)
        request.setStatus(FriendRequest.FriendRequestStatus.REJECTED);
        friendRequestRepository.save(request);
        logger.info("Friend request ID {} rejected by '{}'", requestId, receiverUsername);

        // Option 2: Delete the request entirely (simpler, less history)
        // friendRequestRepository.delete(request);
        // logger.info("Friend request ID {} deleted (rejected) by '{}'", requestId, receiverUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendRequest> getPendingFriendRequestsReceived(String receiverUsername) {
        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new EntityNotFoundException("Receiver user not found: " + receiverUsername));
        // Use repository method to find pending requests for this receiver
        return friendRequestRepository.findByReceiverAndStatus(receiver, FriendRequest.FriendRequestStatus.PENDING);
    }

     @Override
    @Transactional(readOnly = true)
    public List<FriendRequest> getPendingFriendRequestsSent(String senderUsername) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new EntityNotFoundException("Sender user not found: " + senderUsername));
        // Use repository method to find pending requests sent by this user
        return friendRequestRepository.findBySenderAndStatus(sender, FriendRequest.FriendRequestStatus.PENDING);
    }

    // --- END NEW Friend Request Method Implementations ---

}
