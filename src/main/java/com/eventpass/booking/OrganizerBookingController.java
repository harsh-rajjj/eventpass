package com.eventpass.booking;

import com.eventpass.common.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Organizer - Bookings", description = "Endpoints for organizers to view event attendees and check in tickets")
@RestController
@RequestMapping("/api/v1/organizer")
@RequiredArgsConstructor
public class OrganizerBookingController {

    private final OrganizerBookingService organizerBookingService;

    @GetMapping("/events/{eventId}/bookings")
    public ResponseEntity<PageResponse<OrganizerBookingResponse>> listEventBookings(
            Authentication authentication,
            @PathVariable Long eventId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                organizerBookingService.listEventBookings(authentication.getName(), eventId, pageable));
    }

    @PostMapping("/bookings/{bookingRef}/check-in")
    public ResponseEntity<OrganizerBookingResponse> checkIn(
            Authentication authentication,
            @PathVariable String bookingRef
    ) {
        return ResponseEntity.ok(
                organizerBookingService.checkIn(authentication.getName(), bookingRef));
    }
}