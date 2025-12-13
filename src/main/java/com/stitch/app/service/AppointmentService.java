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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    @Transactional
    public Appointment createAppointment(AppointmentDTO dto, MultipartFile[] images, User customer) {
        String imageUrl = null;

        // Upload images if provided
        if (images != null && images.length > 0) {
            List<String> stored = new ArrayList<>();
            for (MultipartFile img : images) {
                if (img != null && !img.isEmpty()) {
                    stored.add(fileStorageService.storeFile(img));
                }
            }
            if (!stored.isEmpty()) {
                // store as comma-separated filenames
                imageUrl = String.join(",", stored);
            }
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
    public Appointment updateAppointmentByCustomer(Long appointmentId, AppointmentDTO dto, MultipartFile[] images, String deleteIndices, Long customerId) {
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

        // Handle image deletions FIRST
        if (deleteIndices != null && !deleteIndices.isEmpty()) {
            String existing = appointment.getInspoImageUrl();
            if (existing != null && !existing.isEmpty()) {
                String[] files = existing.split(",");

                List<Integer> toDelete = Arrays.stream(deleteIndices.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .filter(i -> i >= 0)
                        .sorted(Comparator.reverseOrder()) // Delete from highest index first
                        .collect(Collectors.toList());

                List<String> remaining = new ArrayList<>(Arrays.asList(files));
                for (int idx : toDelete) {
                    if (idx >= 0 && idx < remaining.size()) {
                        // Delete the physical file
                        try {
                            fileStorageService.deleteFile(remaining.get(idx));
                        } catch (Exception e) {
                            System.err.println("Warning: Could not delete file: " + e.getMessage());
                        }
                        remaining.remove(idx);
                    }
                }

                // Update with remaining images
                appointment.setInspoImageUrl(remaining.isEmpty() ? null : String.join(",", remaining));
            }
        }

        // Then append new images if provided
        if (images != null && images.length > 0) {
            List<String> stored = new ArrayList<>();
            for (MultipartFile img : images) {
                if (img != null && !img.isEmpty()) {
                    stored.add(fileStorageService.storeFile(img));
                }
            }
            if (!stored.isEmpty()) {
                String existing = appointment.getInspoImageUrl();
                String appended = String.join(",", stored);
                if (existing != null && !existing.isEmpty()) {
                    appointment.setInspoImageUrl(existing + "," + appended);
                } else {
                    appointment.setInspoImageUrl(appended);
                }
            }
        }

        appointment = appointmentRepository.save(appointment);

        // Notify all admins about the appointment update
        notifyAdminsAboutAppointmentUpdate(appointment);

        return appointment;
    }

    private void notifyAdminsAboutAppointmentUpdate(Appointment appointment) {
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);

        for (User admin : admins) {
            notificationService.createNotification(
                    admin,
                    appointment,
                    "Appointment Updated",
                    appointment.getCustomerName() + " has updated their appointment",
                    Notification.NotificationType.APPOINTMENT_BOOKED
            );
        }
    }

    @Transactional
    public void deleteAppointmentByCustomer(Long appointmentId, Long customerId) {
        // Verify the appointment belongs to the customer
        Appointment appointment = getAppointmentByIdAndCustomer(appointmentId, customerId);

        // Only allow deletion if appointment is PENDING
        if (appointment.getStatus() != Appointment.Status.PENDING) {
            throw new RuntimeException("Cannot delete appointment that is not in PENDING status");
        }

        // Delete associated images if exists (don't fail if image deletion fails)
        if (appointment.getInspoImageUrl() != null && !appointment.getInspoImageUrl().isEmpty()) {
            try {
                String[] files = appointment.getInspoImageUrl().split(",");
                for (String f : files) {
                    fileStorageService.deleteFile(f);
                }
            } catch (Exception e) {
                // Log but continue with appointment deletion
                System.err.println("Warning: Could not delete image files for appointment " + appointmentId + ": " + e.getMessage());
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
                "Your appointment has been approved. Our admin will contact you through a call within 2 days to confirm your appointment details. You will need to visit our physical location and give measurements.",
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
            Appointment.Status oldStatus = appointment.getStatus();
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
            } else if (oldStatus != status) {
                // Notify customer about any status change
                notificationService.createNotification(
                        appointment.getCustomer(),
                        appointment,
                        "Appointment Status Updated",
                        "Your appointment status has been updated to: " + status.toString().replace("_", " "),
                        Notification.NotificationType.APPOINTMENT_STATUS_CHANGED
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

        // Delete stored image files if any
        try {
            Appointment appointment = getAppointmentById(id);
            if (appointment.getInspoImageUrl() != null && !appointment.getInspoImageUrl().isEmpty()) {
                String[] files = appointment.getInspoImageUrl().split(",");
                for (String f : files) {
                    try {
                        fileStorageService.deleteFile(f);
                    } catch (Exception ex) {
                        System.err.println("Warning: Could not delete image file " + f + ": " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // ignore
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

    // --- New bill related methods ---

    @Transactional
    public Appointment uploadBillByAdmin(Long appointmentId, MultipartFile billFile) {
        Appointment appointment = getAppointmentById(appointmentId);

        // Only allow upload when appointment status is COMPLETED
        if (appointment.getStatus() != Appointment.Status.COMPLETED) {
            throw new RuntimeException("Can only upload bill when appointment is COMPLETED");
        }

        // Store the file
        String storedFileName = fileStorageService.storeFile(billFile);

        // If there's an existing bill, delete it
        if (appointment.getBillFileName() != null && !appointment.getBillFileName().isEmpty()) {
            try {
                fileStorageService.deleteFile(appointment.getBillFileName());
            } catch (Exception e) {
                System.err.println("Warning: Could not delete existing bill file: " + e.getMessage());
            }
        }

        appointment.setBillFileName(storedFileName);
        appointment = appointmentRepository.save(appointment);

        // Notify customer about bill upload
        notificationService.createNotification(
                appointment.getCustomer(),
                appointment,
                "Bill Uploaded",
                "A bill has been uploaded for your appointment.",
                Notification.NotificationType.PAYMENT_REMINDER
        );

        return appointment;
    }

    @Transactional
    public void deleteBillByAdmin(Long appointmentId) {
        Appointment appointment = getAppointmentById(appointmentId);

        if (appointment.getBillFileName() == null || appointment.getBillFileName().isEmpty()) {
            return; // nothing to delete
        }

        try {
            fileStorageService.deleteFile(appointment.getBillFileName());
        } catch (Exception e) {
            System.err.println("Warning: Could not delete bill file: " + e.getMessage());
        }

        appointment.setBillFileName(null);
        appointmentRepository.save(appointment);
    }

    public String getBillFileName(Long appointmentId) {
        Appointment appointment = getAppointmentById(appointmentId);
        return appointment.getBillFileName();
    }

    // --- Measurements related methods ---

    @Transactional
    public Appointment uploadMeasurementsByAdmin(Long appointmentId, MultipartFile measurementsFile) {
        Appointment appointment = getAppointmentById(appointmentId);

        // Store the file
        String storedFileName = fileStorageService.storeFile(measurementsFile);

        // If there's an existing measurements file, delete it
        if (appointment.getMeasurementsFileName() != null && !appointment.getMeasurementsFileName().isEmpty()) {
            try {
                fileStorageService.deleteFile(appointment.getMeasurementsFileName());
            } catch (Exception e) {
                System.err.println("Warning: Could not delete existing measurements file: " + e.getMessage());
            }
        }

        appointment.setMeasurementsFileName(storedFileName);
        appointment = appointmentRepository.save(appointment);

        // Notify customer about measurements upload
        notificationService.createNotification(
                appointment.getCustomer(),
                appointment,
                "Measurements Uploaded",
                "Measurements file has been uploaded for your appointment.",
                Notification.NotificationType.MEASUREMENT_REMINDER
        );

        return appointment;
    }

    @Transactional
    public void deleteMeasurementsByAdmin(Long appointmentId) {
        Appointment appointment = getAppointmentById(appointmentId);

        if (appointment.getMeasurementsFileName() == null || appointment.getMeasurementsFileName().isEmpty()) {
            return; // nothing to delete
        }

        try {
            fileStorageService.deleteFile(appointment.getMeasurementsFileName());
        } catch (Exception e) {
            System.err.println("Warning: Could not delete measurements file: " + e.getMessage());
        }

        appointment.setMeasurementsFileName(null);
        appointmentRepository.save(appointment);
    }

    public String getMeasurementsFileName(Long appointmentId) {
        Appointment appointment = getAppointmentById(appointmentId);
        return appointment.getMeasurementsFileName();
    }
}
