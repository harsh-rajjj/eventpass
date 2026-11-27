package com.eventpass.booking;

import java.math.BigDecimal;
import java.time.Instant;

public record OrganizerBookingResponse(
        String bookingRef,
        Long eventId,
        String attendeeName,
        String attendeeEmail,
        String tierName,
        int quantity,
        BigDecimal totalAmount,
        BookingStatus status,
        Instant createdAt,
        Instant checkedInAt
) {

    public static OrganizerBookingResponse from(Booking booking) {
        return new OrganizerBookingResponse(
                booking.getBookingRef(),
                booking.getEvent().getId(),
                booking.getUser().getFullName(),
                booking.getUser().getEmail(),
                booking.getTicketTier().getName(),
                booking.getQuantity(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getCheckedInAt()
        );
    }
}