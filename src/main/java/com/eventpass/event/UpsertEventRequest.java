package com.eventpass.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpsertEventRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        String description,

        @NotNull(message = "Category is required")
        EventCategory category,

        @NotBlank(message = "Venue name is required")
        @Size(max = 200, message = "Venue name must not exceed 200 characters")
        String venueName,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @NotNull(message = "Start time is required")
        Instant startsAt,

        @NotNull(message = "End time is required")
        Instant endsAt
) {
}