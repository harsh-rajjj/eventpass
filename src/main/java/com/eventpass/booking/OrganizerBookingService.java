package com.eventpass.booking;

import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.PageResponse;
import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.event.EventRepository;
import com.eventpass.event.EventStatus;
import com.eventpass.logging.Audited;
import com.eventpass.organizer.Organizer;
import com.eventpass.organizer.OrganizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizerBookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final OrganizerService organizerService;

    public PageResponse<OrganizerBookingResponse> listEventBookings(
            String userEmail,
            Long eventId,
            Pageable pageable
    ) {
        Organizer organizer = organizerService.getOrganizerForUser(userEmail);
        if (!eventRepository.existsByIdAndOrganizerId(eventId, organizer.getId())) {
            throw new ResourceNotFoundException("Event", eventId);
        }

        Page<Booking> page = bookingRepository.findByEventId(eventId, pageable);
        return PageResponse.from(page, OrganizerBookingResponse::from);
    }

    @Audited(action = "CHECK_IN_BOOKING")
    @Transactional
    public OrganizerBookingResponse checkIn(String userEmail, String bookingRef) {
        Organizer organizer = organizerService.getOrganizerForUser(userEmail);
        Booking booking = bookingRepository.findWithDetailsByBookingRef(bookingRef)
                .filter(b -> b.getEvent().getOrganizer().getId().equals(organizer.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingRef));

        if (booking.getEvent().getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot check in attendees for a CANCELLED event");
        }
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            throw new BusinessRuleException("Booking " + bookingRef + " is already checked in");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled booking " + bookingRef + " cannot be checked in");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(Instant.now());

        log.info("Checked in booking ref={} eventId={} organizerId={}",
                bookingRef, booking.getEvent().getId(), organizer.getId());
        return OrganizerBookingResponse.from(booking);
    }
}