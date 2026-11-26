package com.eventpass.booking;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingResponse(
        String bookingRef,
        Long eventId,
        String eventTitle,
        Instant eventStartsAt,
        Long ticketTierId,
        String tierName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        BookingStatus status,
        Instant createdAt,
        Instant cancelledAt,
        Instant checkedInAt
) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getBookingRef(),
                booking.getEvent().getId(),
                booking.getEvent().getTitle(),
                booking.getEvent().getStartsAt(),
                booking.getTicketTier().getId(),
                booking.getTicketTier().getName(),
                booking.getQuantity(),
                booking.getUnitPrice(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getCancelledAt(),
                booking.getCheckedInAt()
        );
    }
}