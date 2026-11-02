package com.eventpass.organizer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrganizerProfileRequest(
        @NotBlank(message = "Organizer name is required")
        @Size(max = 150, message = "Organizer name must not exceed 150 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @Size(max = 255, message = "Website must not exceed 255 characters")
        String website,

        @NotBlank(message = "Contact email is required")
        @Email(message = "Contact email must be a valid email address")
        @Size(max = 255, message = "Contact email must not exceed 255 characters")
        String contactEmail
) {
}
