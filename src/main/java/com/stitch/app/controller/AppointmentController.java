package com.stitch.app.controller;

import com.stitch.app.dto.AppointmentDTO;
import com.stitch.app.entity.Appointment;
import com.stitch.app.entity.User;
import com.stitch.app.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // Customer endpoints
    @PostMapping("/customer")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Appointment> createAppointment(
            @RequestPart("appointment") AppointmentDTO dto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal User user) {
        Appointment appointment = appointmentService.createAppointment(dto, image, user);
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
    public ResponseEntity<Appointment> updateAppointment(
            @PathVariable Long id,
            @RequestPart("appointment") AppointmentDTO dto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal User user) {
        Appointment appointment = appointmentService.updateAppointmentByCustomer(id, dto, image, user.getId());
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
}