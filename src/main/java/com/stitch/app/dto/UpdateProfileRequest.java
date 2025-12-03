package com.stitch.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String fullName;

    @Email(message = "Invalid email")
    private String email;

    @Size(min = 7, message = "Phone number seems too short")
    private String phoneNumber;
}

