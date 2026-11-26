package com.eventpass.booking;

import com.eventpass.common.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Attendee - Bookings", description = "Endpoints for attendees to book, view, and cancel event tickets")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(
            Authentication authentication,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        BookingResponse created = bookingService.createBooking(authentication.getName(), request);
        URI location = URI.create("/api/v1/bookings/" + created.bookingRef());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/me/bookings")
    public ResponseEntity<PageResponse<BookingResponse>> listMyBookings(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(bookingService.listMyBookings(authentication.getName(), pageable));
    }

    @GetMapping("/bookings/{bookingRef}")
    public ResponseEntity<BookingResponse> getMyBooking(
            Authentication authentication,
            @PathVariable String bookingRef
    ) {
        return ResponseEntity.ok(bookingService.getMyBooking(authentication.getName(), bookingRef));
    }

    @PostMapping("/bookings/{bookingRef}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            Authentication authentication,
            @PathVariable String bookingRef
    ) {
        return ResponseEntity.ok(bookingService.cancelBooking(authentication.getName(), bookingRef));
    }
}