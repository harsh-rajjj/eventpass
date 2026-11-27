package com.eventpass.booking;

import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.event.Event;
import com.eventpass.event.EventRepository;
import com.eventpass.event.EventStatus;
import com.eventpass.event.TicketTier;
import com.eventpass.organizer.Organizer;
import com.eventpass.organizer.OrganizerService;
import com.eventpass.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizerBookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrganizerService organizerService;

    @InjectMocks
    private OrganizerBookingService organizerBookingService;

    @Test
    void checkIn_succeedsOnce_andRejectsSecondCheckInWithConflict() {
        Organizer ownerOrg = organizerWithId(1L);
        Booking booking = sampleBooking("EVP-8F3K2A", ownerOrg, BookingStatus.CONFIRMED);

        when(organizerService.getOrganizerForUser("org@neon.com")).thenReturn(ownerOrg);
        when(bookingRepository.findWithDetailsByBookingRef("EVP-8F3K2A")).thenReturn(Optional.of(booking));

        OrganizerBookingResponse first = organizerBookingService.checkIn("org@neon.com", "EVP-8F3K2A");
        assertThat(first.status()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(first.checkedInAt()).isNotNull();

        assertThatThrownBy(() -> organizerBookingService.checkIn("org@neon.com", "EVP-8F3K2A"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Booking EVP-8F3K2A is already checked in");
    }

    @Test
    void checkIn_whenBookingIsCancelled_throwsBusinessRuleException() {
        Organizer ownerOrg = organizerWithId(1L);
        Booking cancelled = sampleBooking("EVP-CANCEL", ownerOrg, BookingStatus.CANCELLED);

        when(organizerService.getOrganizerForUser("org@neon.com")).thenReturn(ownerOrg);
        when(bookingRepository.findWithDetailsByBookingRef("EVP-CANCEL")).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> organizerBookingService.checkIn("org@neon.com", "EVP-CANCEL"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cancelled booking EVP-CANCEL cannot be checked in");
    }

    @Test
    void checkIn_whenOrganizerDoesNotOwnEvent_throwsResourceNotFoundException() {
        Organizer ownerOrg = organizerWithId(1L);
        Organizer otherOrg = organizerWithId(2L);
        Booking booking = sampleBooking("EVP-8F3K2A", ownerOrg, BookingStatus.CONFIRMED);

        when(organizerService.getOrganizerForUser("other@beta.com")).thenReturn(otherOrg);
        when(bookingRepository.findWithDetailsByBookingRef("EVP-8F3K2A")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> organizerBookingService.checkIn("other@beta.com", "EVP-8F3K2A"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Organizer organizerWithId(Long id) {
        Organizer organizer = new Organizer();
        organizer.setId(id);
        return organizer;
    }

    private Booking sampleBooking(String ref, Organizer organizer, BookingStatus status) {
        Event event = new Event();
        event.setId(10L);
        event.setOrganizer(organizer);
        event.setStatus(EventStatus.PUBLISHED);

        TicketTier tier = new TicketTier();
        tier.setId(20L);
        tier.setName("VIP");

        User attendee = new User();
        attendee.setFullName("Aarav Sharma");
        attendee.setEmail("aarav@example.com");

        Booking booking = new Booking();
        booking.setBookingRef(ref);
        booking.setEvent(event);
        booking.setTicketTier(tier);
        booking.setUser(attendee);
        booking.setQuantity(2);
        booking.setTotalAmount(new BigDecimal("4998.00"));
        booking.setStatus(status);
        return booking;
    }
}