package com.stitch.app.controller;

import org.springframework.web.bind.annotation.*;

/**
 * CustomerController - Reserved for future customer-specific endpoints
 *
 * Note: All appointment-related endpoints have been moved to AppointmentController
 * to avoid duplication and maintain a single source of truth.
 *
 * Current appointment endpoints:
 * - POST /api/appointments/customer - Create appointment
 * - GET /api/appointments/customer/my-appointments - Get customer's own appointments
 * - GET /api/appointments/customer/{customerId}/appointments - Get appointments by customer ID
 * - GET /api/appointments/customer/{id} - Get specific appointment by ID
 *
 * This controller can be used for other customer-specific features like:
 * - Profile management
 * - Preferences
 * - Dashboard data
 * - etc.
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    // Reserved for future customer-specific endpoints
    // All appointment endpoints are now in AppointmentController
}

