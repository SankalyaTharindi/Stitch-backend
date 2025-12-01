package com.stitch.app.service;

import com.stitch.app.dto.AppointmentDTO;
import com.stitch.app.entity.Appointment;
import com.stitch.app.entity.Notification;
import com.stitch.app.entity.User;
import com.stitch.app.repository.AppointmentRepository;
import com.stitch.app.repository.NotificationRepository;
import com.stitch.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    @Transactional
    public Appointment createAppointment(AppointmentDTO dto, MultipartFile image, User customer) {
        String imageUrl = null;

        // Upload image if provided
        if (image != null && !image.isEmpty()) {
            imageUrl = fileStorageService.storeFile(image);
        }

        Appointment appointment = Appointment.builder()
                .customer(customer)
                .customerName(dto.getCustomerName())
                .age(dto.getAge())
                .phoneNumber(dto.getPhoneNumber())
                .deadline(dto.getDeadline())
                .inspoImageUrl(imageUrl)
                .status(Appointment.Status.PENDING)
                .notes(dto.getNotes())
                .build();

        appointment = appointmentRepository.save(appointment);

        // Notify all admins
        notifyAdmins(appointment);

        return appointment;
    }

    public List<Appointment> getAppointmentsByCustomer(Long customerId) {
        return appointmentRepository.findByCustomer_Id(customerId);
    }

    public Appointment getAppointmentByIdAndCustomer(Long appointmentId, Long customerId) {
        return appointmentRepository.findByIdAndCustomer_Id(appointmentId, customerId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
    }

    @Transactional
    public Appointment updateAppointmentByCustomer(Long appointmentId, AppointmentDTO dto, MultipartFile image, Long customerId) {
        // Verify the appointment belongs to the customer
        Appointment appointment = getAppointmentByIdAndCustomer(appointmentId, customerId);

        // Only allow updates if appointment is PENDING
        if (appointment.getStatus() != Appointment.Status.PENDING) {
            throw new RuntimeException("Cannot update appointment that is not in PENDING status");
        }

        // Update fields
        if (dto.getCustomerName() != null) {
            appointment.setCustomerName(dto.getCustomerName());
        }
        if (dto.getAge() != null) {
            appointment.setAge(dto.getAge());
        }
        if (dto.getPhoneNumber() != null) {
            appointment.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getDeadline() != null) {
            appointment.setDeadline(dto.getDeadline());
        }
        if (dto.getNotes() != null) {
            appointment.setNotes(dto.getNotes());
        }

        // Update image if provided
        if (image != null && !image.isEmpty()) {
            // Delete old image if exists
            if (appointment.getInspoImageUrl() != null) {
                fileStorageService.deleteFile(appointment.getInspoImageUrl());
            }
            String imageUrl = fileStorageService.storeFile(image);
            appointment.setInspoImageUrl(imageUrl);
        }

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public void deleteAppointmentByCustomer(Long appointmentId, Long customerId) {
        // Verify the appointment belongs to the customer
        Appointment appointment = getAppointmentByIdAndCustomer(appointmentId, customerId);

        // Only allow deletion if appointment is PENDING
        if (appointment.getStatus() != Appointment.Status.PENDING) {
            throw new RuntimeException("Cannot delete appointment that is not in PENDING status");
        }

        // Delete associated image if exists (don't fail if image deletion fails)
        if (appointment.getInspoImageUrl() != null && !appointment.getInspoImageUrl().isEmpty()) {
            try {
                fileStorageService.deleteFile(appointment.getInspoImageUrl());
            } catch (Exception e) {
                // Log but continue with appointment deletion
                System.err.println("Warning: Could not delete image file for appointment " + appointmentId + ": " + e.getMessage());
            }
        }

        // Delete related notifications first (to avoid foreign key constraint violation)
        try {
            notificationRepository.deleteByAppointmentId(appointmentId);
        } catch (Exception e) {
            System.err.println("Warning: Could not delete notifications for appointment " + appointmentId + ": " + e.getMessage());
        }

        // Delete the appointment from database
        appointmentRepository.deleteById(appointmentId);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAllByOrderByCreatedAtDesc();
    }

    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
    }

    @Transactional
    public Appointment approveAppointment(Long id) {
        Appointment appointment = getAppointmentById(id);
        appointment.setStatus(Appointment.Status.APPROVED);
        appointment = appointmentRepository.save(appointment);

        // Notify customer
        notificationService.createNotification(
                appointment.getCustomer(),
                appointment,
                "Appointment Approved",
                "Your appointment has been approved. Please come to give measurements.",
                Notification.NotificationType.APPOINTMENT_APPROVED
        );

        return appointment;
    }

    @Transactional
    public Appointment declineAppointment(Long id, String reason) {
        Appointment appointment = getAppointmentById(id);
        appointment.setStatus(Appointment.Status.DECLINED);
        appointment.setNotes(reason);
        appointment = appointmentRepository.save(appointment);

        // Notify customer
        notificationService.createNotification(
                appointment.getCustomer(),
                appointment,
                "Appointment Declined",
                "Your appointment has been declined. Reason: " + reason,
                Notification.NotificationType.APPOINTMENT_DECLINED
        );

        return appointment;
    }

    @Transactional
    public Appointment updateAppointmentStatus(Long id, String statusStr) {
        Appointment appointment = getAppointmentById(id);

        try {
            Appointment.Status status = Appointment.Status.valueOf(statusStr.toUpperCase());
            appointment.setStatus(status);
            appointment = appointmentRepository.save(appointment);

            // Send notification based on status
            if (status == Appointment.Status.COMPLETED) {
                notificationService.createNotification(
                        appointment.getCustomer(),
                        appointment,
                        "Jacket Ready",
                        "Your saree jacket is ready! Please come to collect it.",
                        Notification.NotificationType.JACKET_READY
                );
            }

            return appointment;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + statusStr);
        }
    }

    @Transactional
    public void deleteAppointment(Long id) {
        // Delete related notifications first (to avoid foreign key constraint violation)
        try {
            notificationRepository.deleteByAppointmentId(id);
        } catch (Exception e) {
            System.err.println("Warning: Could not delete notifications for appointment " + id + ": " + e.getMessage());
        }

        appointmentRepository.deleteById(id);
    }

    private void notifyAdmins(Appointment appointment) {
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);

        for (User admin : admins) {
            notificationService.createNotification(
                    admin,
                    appointment,
                    "New Appointment",
                    "New appointment request from " + appointment.getCustomerName(),
                    Notification.NotificationType.APPOINTMENT_BOOKED
            );
        }
    }
}