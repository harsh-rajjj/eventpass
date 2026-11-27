package com.eventpass.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank(message = "app.jwt.secret must be configured")
        @Size(min = 32, message = "app.jwt.secret must be at least 32 characters for HS256")
        String secret,

        @NotNull(message = "app.jwt.expiration must be configured")
        Duration expiration
) {
}