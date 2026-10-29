package com.eventpass.support;
import jakarta.validation.constraints.NotNull;

public record UpdateSupportTicketStatusRequest(
     @NotNull(message = "Status is required")
     SupportTicketStatus status
) {

}
