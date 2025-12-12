package com.stitch.app.repository;

import com.stitch.app.entity.Message;
import com.stitch.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Get all messages between two users (for chat history)
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender = :user1 AND m.receiver = :user2) OR " +
           "(m.sender = :user2 AND m.receiver = :user1) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findMessagesBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    // Get all unique customers who have had conversations with admin
    // Using UNION to avoid complex CASE expressions that cause Hibernate issues
    @Query("SELECT DISTINCT u FROM User u WHERE u.role = 'CUSTOMER' AND " +
           "(EXISTS (SELECT m FROM Message m WHERE m.sender = u AND m.receiver.role = 'ADMIN') OR " +
           "EXISTS (SELECT m FROM Message m WHERE m.receiver = u AND m.sender.role = 'ADMIN')) " +
           "ORDER BY u.id")
    List<User> findAllCustomersWithMessages();

    // Get unread message count for a user
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = :user AND m.isRead = false")
    Long countUnreadMessages(@Param("user") User user);

    // Get unread message count between two users
    @Query("SELECT COUNT(m) FROM Message m WHERE m.sender = :sender AND m.receiver = :receiver AND m.isRead = false")
    Long countUnreadMessagesBetweenUsers(@Param("sender") User sender, @Param("receiver") User receiver);

    // Mark all messages as read
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Message m SET m.isRead = true WHERE m.sender = :sender AND m.receiver = :receiver AND m.isRead = false")
    int markMessagesAsRead(@Param("sender") User sender, @Param("receiver") User receiver);
}
