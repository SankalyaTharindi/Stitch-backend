package com.stitch.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDTO {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    private Integer age;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotNull(message = "Deadline is required")
    private LocalDate deadline;

    private String notes;
}