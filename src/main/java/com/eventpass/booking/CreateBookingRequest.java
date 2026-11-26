package com.eventpass.booking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
        @NotNull(message = "Ticket tier id is required")
        Long ticketTierId,

        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 6, message = "Cannot book more than 6 tickets per request")
        int quantity
) {
}