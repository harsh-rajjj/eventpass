package com.eventpass.booking;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(
        @Min(value = 1, message = "maxTicketsPerEvent must be at least 1")
        int maxTicketsPerEvent,

        @Min(value = 0, message = "cancelCutoffHours must be zero or positive")
        long cancelCutoffHours
) {
}