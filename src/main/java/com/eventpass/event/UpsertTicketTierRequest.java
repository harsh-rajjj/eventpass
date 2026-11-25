package com.eventpass.event;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpsertTicketTierRequest(
        @NotBlank(message = "Tier name is required")
        @Size(max = 100, message = "Tier name must not exceed 100 characters")
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Price must be zero or positive")
        BigDecimal price,

        @Min(value = 1, message = "Capacity must be at least 1")
        int capacity
) {
}