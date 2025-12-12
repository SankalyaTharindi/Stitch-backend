package com.stitch.app.controller;

import com.stitch.app.dto.ChatUserDTO;
import com.stitch.app.dto.MessageDTO;
import com.stitch.app.dto.SendMessageRequest;
import com.stitch.app.entity.User;
import com.stitch.app.repository.UserRepository;
import com.stitch.app.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final MessageService messageService;
    private final UserRepository userRepository;

    // Send message via REST API
    @PostMapping("/send")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER')")
    public ResponseEntity<MessageDTO> sendMessage(
            @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal User user) {
        MessageDTO message = messageService.sendMessage(request, user);
        return ResponseEntity.ok(message);
    }

    // WebSocket endpoint for sending messages
    @MessageMapping("/chat.send")
    public void sendMessageViaWebSocket(
            @Payload SendMessageRequest request,
            Principal principal) {
        // Resolve sender from principal name (which is user's email)
        User sender = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        messageService.sendMessage(request, sender);
    }

    // Get chat history between current user and another user
    @GetMapping("/chat/{otherUserId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER')")
    public ResponseEntity<List<MessageDTO>> getChatHistory(
            @PathVariable Long otherUserId,
            @AuthenticationPrincipal User user) {
        List<MessageDTO> messages = messageService.getChatHistory(user, otherUserId);
        return ResponseEntity.ok(messages);
    }

    // For admin: Get all customers who have sent messages
    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<ChatUserDTO>> getCustomersWithMessages(@AuthenticationPrincipal User user) {
        // Diagnostic log to show which principal is calling this endpoint
        logger.info("GET /api/messages/customers called by principal={}, userId={}",
                user != null ? user.getEmail() : null,
                user != null ? user.getId() : null);

        List<ChatUserDTO> customers = messageService.getCustomersWithMessages();
        return ResponseEntity.ok(customers);
    }

    // For customer: Get admin info to chat with
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ChatUserDTO> getAdminChatUser(@AuthenticationPrincipal User user) {
        ChatUserDTO admin = messageService.getAdminChatUser(user);
        return ResponseEntity.ok(admin);
    }

    // Mark messages as read
    @PostMapping("/mark-read/{senderId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER')")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable Long senderId,
            @AuthenticationPrincipal User user) {
        User sender = User.builder().id(senderId).build();
        messageService.markMessagesAsRead(sender, user);
        return ResponseEntity.ok().build();
    }

    // Get unread message count for current user
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER')")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal User user) {
        Long count = messageService.getUnreadCount(user);
        return ResponseEntity.ok(count);
    }
}
