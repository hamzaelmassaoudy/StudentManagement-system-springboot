package student_management_system.service;

import student_management_system.model.FriendRequest; // Import FriendRequest
import student_management_system.model.User;
import student_management_system.web.dto.ChangePasswordDto;
import student_management_system.web.dto.UserRegistrationDto;
import student_management_system.web.dto.UserSettingsDto;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Interface defining the operations for user management.
 */
public interface UserService {

    // --- Existing User Methods ---
    Optional<User> findUserByUsername(String username);
    Optional<User> findUserById(Long id);
    User createTeacher(UserRegistrationDto registrationDto);
    List<User> findAllTeachers();
    User updateTeacher(Long id, UserRegistrationDto dto);
    void deleteTeacher(Long id);
    User createStudent(UserRegistrationDto registrationDto);

    // --- Settings Methods ---
    User updateUserSettings(String username, UserSettingsDto settingsDto);
    void changeUserPassword(String username, ChangePasswordDto passwordDto);
    void updateProfilePicturePath(String username, String filename);

    // --- Friend Management Methods ---
    void addFriend(String currentUsername, String friendUsername); // Keep for internal use
    void removeFriend(String currentUsername, String friendUsername);
    Set<User> getFriends(String username);
    List<User> searchPotentialFriends(String currentUsername, String searchTerm);

    // --- NEW Friend Request Methods ---

    /**
     * Sends a friend request from one user to another.
     * @param senderUsername The username of the user sending the request.
     * @param receiverUsername The username of the user receiving the request.
     * @throws RuntimeException if users not found, already friends, request already pending, or self-request.
     */
    void sendFriendRequest(String senderUsername, String receiverUsername);

    /**
     * Accepts a pending friend request.
     * @param requestId The ID of the FriendRequest to accept.
     * @param receiverUsername The username of the user accepting the request (for verification).
     * @throws RuntimeException if request not found, user is not the receiver, or request is not pending.
     */
    void acceptFriendRequest(Long requestId, String receiverUsername);

    /**
     * Rejects a pending friend request.
     * @param requestId The ID of the FriendRequest to reject.
     * @param receiverUsername The username of the user rejecting the request (for verification).
     * @throws RuntimeException if request not found, user is not the receiver, or request is not pending.
     */
    void rejectFriendRequest(Long requestId, String receiverUsername);

    /**
     * Retrieves all pending friend requests received by a user.
     * @param receiverUsername The username of the user.
     * @return A List of pending FriendRequest objects.
     * @throws RuntimeException if the user is not found.
     */
    List<FriendRequest> getPendingFriendRequestsReceived(String receiverUsername);

    /**
     * Retrieves all pending friend requests sent by a user.
     * @param senderUsername The username of the user.
     * @return A List of pending FriendRequest objects.
     * @throws RuntimeException if the user is not found.
     */
    List<FriendRequest> getPendingFriendRequestsSent(String senderUsername);

    // --- END NEW Friend Request Methods ---

}
