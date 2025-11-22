package com.stitch.app.service;

import com.stitch.app.dto.AppointmentDTO;
import com.stitch.app.entity.Appointment;
import com.stitch.app.entity.Notification;
import com.stitch.app.entity.User;
import com.stitch.app.repository.AppointmentRepository;
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
        return appointmentRepository.findByCustomerId(customerId);
    }

    public Appointment getAppointmentByIdAndCustomer(Long appointmentId, Long customerId) {
        return appointmentRepository.findByIdAndCustomerId(appointmentId, customerId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
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