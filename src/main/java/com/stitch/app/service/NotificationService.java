package com.stitch.app.service;

import com.stitch.app.entity.Appointment;
import com.stitch.app.entity.Notification;
import com.stitch.app.entity.User;
import com.stitch.app.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Transactional
    public Notification createNotification(User user, Appointment appointment,
                                           String title, String message,
                                           Notification.NotificationType type) {
        Notification notification = Notification.builder()
                .user(user)
                .appointment(appointment)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);
        sendEmailNotification(user.getEmail(), title, message);

        return notification;
    }

    @Async
    public void sendEmailNotification(String to, String subject, String text) {
        if (mailSender == null) {
            log.info("MailSender not configured - skipping email to {}", to);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        message.setFrom("noreply@stitch.com");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Log error
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }
}