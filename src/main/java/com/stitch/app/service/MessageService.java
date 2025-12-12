package com.stitch.app.service;

import com.stitch.app.dto.ChatUserDTO;
import com.stitch.app.dto.MessageDTO;
import com.stitch.app.dto.SendMessageRequest;
import com.stitch.app.entity.Message;
import com.stitch.app.entity.User;
import com.stitch.app.repository.MessageRepository;
import com.stitch.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageDTO sendMessage(SendMessageRequest request, User sender) {
        // Resolve sender if only id/email provided (WebSocket Principal may pass only username/email)
        User resolvedSender = resolveUserIfNeeded(sender);

        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Message message = Message.builder()
                .sender(resolvedSender)
                .receiver(receiver)
                .content(request.getContent())
                .isRead(false)
                .build();

        message = messageRepository.save(message);

        MessageDTO messageDTO = convertToDTO(message);

        // Send real-time notification to receiver via WebSocket
        // Use receiver id as user destination so client subscribing to /user/{id}/queue/messages will receive it
        String destinationUser = receiver.getId().toString();
        messagingTemplate.convertAndSendToUser(
                destinationUser,
                "/queue/messages",
                messageDTO
        );

        return messageDTO;
    }

    private User resolveUserIfNeeded(User sender) {
        if (sender == null) throw new RuntimeException("Sender must not be null");

        // If sender has email populated, prefer lookup by email
        if (sender.getEmail() != null) {
            return userRepository.findByEmail(sender.getEmail())
                    .orElseThrow(() -> new RuntimeException("Sender not found by email"));
        }

        // Else if id is present, lookup by id
        if (sender.getId() != null) {
            return userRepository.findById(sender.getId())
                    .orElseThrow(() -> new RuntimeException("Sender not found by id"));
        }

        // Already a managed full user
        return sender;
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getMessagesBetweenUsers(Long userId1, Long userId2) {
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Message> messages = messageRepository.findMessagesBetweenUsers(user1, user2);
        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getChatHistory(User currentUser, Long otherUserId) {
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Message> messages = messageRepository.findMessagesBetweenUsers(currentUser, otherUser);
        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatUserDTO> getCustomersWithMessages() {
        // Get all customers who have sent/received messages with admin
        List<User> customers = messageRepository.findAllCustomersWithMessages();

        // Get admin user
        User admin = userRepository.findByRole(User.Role.ADMIN).stream()
                .findFirst()
                .orElse(null);

        if (admin == null) {
            return new ArrayList<>();
        }

        return customers.stream()
                .map(customer -> {
                    Long unreadCount = messageRepository.countUnreadMessagesBetweenUsers(customer, admin);
                    return ChatUserDTO.builder()
                            .id(customer.getId())
                            .fullName(customer.getFullName())
                            .email(customer.getEmail())
                            .unreadCount(unreadCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatUserDTO getAdminChatUser(User customer) {
        // Get admin user
        User admin = userRepository.findByRole(User.Role.ADMIN).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        Long unreadCount = messageRepository.countUnreadMessagesBetweenUsers(admin, customer);

        return ChatUserDTO.builder()
                .id(admin.getId())
                .fullName("Stitch Admin")
                .email(admin.getEmail())
                .unreadCount(unreadCount)
                .build();
    }

    @Transactional
    public void markMessagesAsRead(User sender, User receiver) {
        // Ensure both are managed entities
        User resolvedSender = resolveUserIfNeeded(sender);
        User resolvedReceiver = resolveUserIfNeeded(receiver);
        messageRepository.markMessagesAsRead(resolvedSender, resolvedReceiver);
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(User user) {
        return messageRepository.countUnreadMessages(user);
    }

    private MessageDTO convertToDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .receiverId(message.getReceiver().getId())
                .receiverName(message.getReceiver().getFullName())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
