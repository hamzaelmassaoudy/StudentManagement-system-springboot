package student_management_system.repository;

import student_management_system.model.FriendRequest;
import student_management_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for FriendRequest entities.
 */
@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    /**
     * Finds pending friend requests received by a specific user.
     * @param receiver The user who received the requests.
     * @return A list of pending friend requests.
     */
    List<FriendRequest> findByReceiverAndStatus(User receiver, FriendRequest.FriendRequestStatus status);

    /**
     * Finds pending friend requests sent by a specific user.
     * @param sender The user who sent the requests.
     * @return A list of pending friend requests sent by the user.
     */
    List<FriendRequest> findBySenderAndStatus(User sender, FriendRequest.FriendRequestStatus status);

    /**
     * Checks if a pending friend request exists between two users (in either direction).
     * Useful before allowing a new request to be sent.
     * @param user1 One user.
     * @param user2 The other user.
     * @param status The status to check for (typically PENDING).
     * @return An Optional containing the FriendRequest if a pending one exists, empty otherwise.
     */
    Optional<FriendRequest> findBySenderAndReceiverAndStatus(User user1, User user2, FriendRequest.FriendRequestStatus status);

    // Combined check for pending request in either direction
    default Optional<FriendRequest> findPendingRequestBetweenUsers(User user1, User user2) {
        Optional<FriendRequest> requestSent = findBySenderAndReceiverAndStatus(user1, user2, FriendRequest.FriendRequestStatus.PENDING);
        if (requestSent.isPresent()) {
            return requestSent;
        }
        return findBySenderAndReceiverAndStatus(user2, user1, FriendRequest.FriendRequestStatus.PENDING);
    }

}
