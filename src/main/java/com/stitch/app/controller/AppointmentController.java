package com.stitch.app.controller;

import com.stitch.app.dto.AppointmentDTO;
import com.stitch.app.entity.Appointment;
import com.stitch.app.entity.User;
import com.stitch.app.repository.UserRepository;
import com.stitch.app.service.AppointmentService;
import com.stitch.app.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    // Customer endpoints
    @PostMapping("/customer")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Appointment> createAppointment(
            @RequestPart("appointment") AppointmentDTO dto,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @AuthenticationPrincipal User user) {
        Appointment appointment = appointmentService.createAppointment(dto, images, user);
        return ResponseEntity.ok(appointment);
    }

    @GetMapping("/customer/my-appointments")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<List<Appointment>> getMyAppointments(@AuthenticationPrincipal User user) {
        List<Appointment> appointments = appointmentService.getAppointmentsByCustomer(user.getId());
        return ResponseEntity.ok(appointments);
    }

    // New: allow a customer to fetch their appointments by id path and enforce that the authenticated user matches
    @GetMapping("/customer/{customerId}/appointments")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<List<Appointment>> getAppointmentsByCustomer(
            @PathVariable Long customerId,
            @AuthenticationPrincipal User user) {
        if (user == null || !user.getId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<Appointment> appointments = appointmentService.getAppointmentsByCustomer(customerId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/customer/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Appointment> getAppointmentById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        Appointment appointment = appointmentService.getAppointmentByIdAndCustomer(id, user.getId());
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/customer/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Appointment> updateAppointmentByCustomer(
            @PathVariable Long id,
            @RequestPart("appointment") AppointmentDTO dto,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "deleteIndices", required = false) String deleteIndices,
            @AuthenticationPrincipal UserDetails userDetails) {

        User customer = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Appointment appointment = appointmentService.updateAppointmentByCustomer(
                id, dto, images, deleteIndices, customer.getId()
        );

        return ResponseEntity.ok(appointment);
    }

    @DeleteMapping("/customer/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Void> deleteAppointmentByCustomer(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        appointmentService.deleteAppointmentByCustomer(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    // Admin endpoints
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        List<Appointment> appointments = appointmentService.getAllAppointments();
        return ResponseEntity.ok(appointments);
    }

    // Admin: fetch appointments for any customer by id
    @GetMapping("/admin/customer/{customerId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Appointment>> getAppointmentsByCustomerAdmin(@PathVariable Long customerId) {
        List<Appointment> appointments = appointmentService.getAppointmentsByCustomer(customerId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Appointment> getAppointmentByIdAdmin(@PathVariable Long id) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/admin/{id}/approve")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Appointment> approveAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.approveAppointment(id);
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/admin/{id}/decline")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Appointment> declineAppointment(
            @PathVariable Long id,
            @RequestBody DeclineRequest request) {
        Appointment appointment = appointmentService.declineAppointment(id, request.getReason());
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Appointment> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        Appointment appointment = appointmentService.updateAppointmentStatus(id, request.getStatus());
        return ResponseEntity.ok(appointment);
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }

    // Serve image for admin
    @GetMapping("/admin/{id}/image")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Resource> getAppointmentImageAdmin(@PathVariable Long id,
                                                             @RequestParam(value = "index", required = false, defaultValue = "0") int index) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        String fileNames = appointment.getInspoImageUrl();
        if (fileNames == null || fileNames.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String[] parts = fileNames.split(",");
        if (index < 0 || index >= parts.length) {
            return ResponseEntity.badRequest().build();
        }
        String fileName = parts[index];

        try {
            Path filePath = fileStorageService.loadFile(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(filePath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                    .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Serve image for customer (only their own appointment)
    @GetMapping("/customer/{id}/image")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Resource> getAppointmentImageCustomer(@PathVariable Long id,
                                                                @AuthenticationPrincipal User user,
                                                                @RequestParam(value = "index", required = false, defaultValue = "0") int index) {
        Appointment appointment = appointmentService.getAppointmentByIdAndCustomer(id, user.getId());
        String fileNames = appointment.getInspoImageUrl();
        if (fileNames == null || fileNames.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String[] parts = fileNames.split(",");
        if (index < 0 || index >= parts.length) {
            return ResponseEntity.badRequest().build();
        }
        String fileName = parts[index];

        try {
            Path filePath = fileStorageService.loadFile(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(filePath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                    .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- New endpoints for bill upload/download/delete ---

    // Admin upload bill for appointment (only when appointment.status == COMPLETED)
    @PostMapping("/admin/{appointmentId}/bill")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Appointment> uploadBillByAdmin(@PathVariable Long appointmentId,
                                                         @RequestPart("bill") MultipartFile billFile) {
        Appointment updated = appointmentService.uploadBillByAdmin(appointmentId, billFile);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/admin/{appointmentId}/bill")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteBillByAdmin(@PathVariable Long appointmentId) {
        appointmentService.deleteBillByAdmin(appointmentId);
        return ResponseEntity.noContent().build();
    }

    // Customer/Admin download bill - customer can only download for their own appointment
    @GetMapping("/{appointmentId}/bill")
    @PreAuthorize("hasAnyAuthority('ADMIN','CUSTOMER')")
    public ResponseEntity<Resource> downloadBill(@PathVariable Long appointmentId,
                                                 @AuthenticationPrincipal User user) {
        Appointment appointment = appointmentService.getAppointmentById(appointmentId);

        // If current user is customer, ensure ownership
        if (user != null && user.getRole() == User.Role.CUSTOMER) {
            if (!appointment.getCustomer().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        String fileName = appointment.getBillFileName();
        if (fileName == null || fileName.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = fileStorageService.loadFile(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(filePath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                    .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- New endpoints for measurements upload/download/delete ---

    // Admin upload measurements for appointment
    @PostMapping("/admin/{appointmentId}/measurements")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Appointment> uploadMeasurementsByAdmin(@PathVariable Long appointmentId,
                                                                  @RequestPart("measurements") MultipartFile measurementsFile) {
        Appointment updated = appointmentService.uploadMeasurementsByAdmin(appointmentId, measurementsFile);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/admin/{appointmentId}/measurements")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteMeasurementsByAdmin(@PathVariable Long appointmentId) {
        appointmentService.deleteMeasurementsByAdmin(appointmentId);
        return ResponseEntity.noContent().build();
    }

    // Customer/Admin download measurements - customer can only download for their own appointment
    @GetMapping("/{appointmentId}/measurements")
    @PreAuthorize("hasAnyAuthority('ADMIN','CUSTOMER')")
    public ResponseEntity<Resource> downloadMeasurements(@PathVariable Long appointmentId,
                                                         @AuthenticationPrincipal User user) {
        Appointment appointment = appointmentService.getAppointmentById(appointmentId);

        // If current user is customer, ensure ownership
        if (user != null && user.getRole() == User.Role.CUSTOMER) {
            if (!appointment.getCustomer().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        String fileName = appointment.getMeasurementsFileName();
        if (fileName == null || fileName.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = fileStorageService.loadFile(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(filePath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                    .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

