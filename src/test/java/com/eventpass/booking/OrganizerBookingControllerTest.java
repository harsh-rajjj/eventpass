package com.eventpass.booking;

import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.PageResponse;
import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.security.JwtAuthenticationFilter;
import com.eventpass.security.JwtService;
import com.eventpass.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(OrganizerBookingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class OrganizerBookingControllerTest {

    private static final String ORGANIZER = "org@neon.com";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private OrganizerBookingService organizerBookingService;

    private OrganizerBookingResponse sampleResponse(BookingStatus status, Instant checkedInAt) {
        return new OrganizerBookingResponse(
                "EVP-8F3K2A", 1L, "Aarav Sharma", "aarav@example.com", "VIP",
                2, new BigDecimal("4998.00"), status,
                Instant.parse("2026-11-26T10:00:00Z"), checkedInAt
        );
    }

    @Test
    void checkIn_whenUnauthenticated_returns401() {
        assertThat(mvc.post().uri("/api/v1/organizer/bookings/EVP-8F3K2A/check-in"))
                .hasStatus(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(organizerBookingService);
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void checkIn_whenAttendeeRole_returns403() {
        assertThat(mvc.post().uri("/api/v1/organizer/bookings/EVP-8F3K2A/check-in"))
                .hasStatus(HttpStatus.FORBIDDEN);

        verifyNoInteractions(organizerBookingService);
    }

    @Test
    @WithMockUser(username = ORGANIZER, roles = "ORGANIZER")
    void listEventBookings_whenOrganizerOwnsEvent_returns200PagedList() {
        var page = new PageResponse<>(
                List.of(sampleResponse(BookingStatus.CONFIRMED, null)), 0, 20, 1L, 1, true, true);
        when(organizerBookingService.listEventBookings(eq(ORGANIZER), eq(1L), any(Pageable.class)))
                .thenReturn(page);

        assertThat(mvc.get().uri("/api/v1/organizer/events/1/bookings"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.content[0].bookingRef").isEqualTo("EVP-8F3K2A");
                    assertThat(json).extractingPath("$.content[0].attendeeEmail").isEqualTo("aarav@example.com");
                });
    }

    @Test
    @WithMockUser(username = ORGANIZER, roles = "ORGANIZER")
    void checkIn_whenConfirmed_returns200CheckedIn() {
        Instant now = Instant.parse("2027-02-14T13:30:00Z");
        when(organizerBookingService.checkIn(ORGANIZER, "EVP-8F3K2A"))
                .thenReturn(sampleResponse(BookingStatus.CHECKED_IN, now));

        assertThat(mvc.post().uri("/api/v1/organizer/bookings/EVP-8F3K2A/check-in"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.status").isEqualTo("CHECKED_IN");
                    assertThat(json).extractingPath("$.checkedInAt").isEqualTo("2027-02-14T13:30:00Z");
                });
    }

    @Test
    @WithMockUser(username = ORGANIZER, roles = "ORGANIZER")
    void checkIn_whenAlreadyCheckedIn_returns409() {
        when(organizerBookingService.checkIn(ORGANIZER, "EVP-8F3K2A"))
                .thenThrow(new BusinessRuleException("Booking EVP-8F3K2A is already checked in"));

        assertThat(mvc.post().uri("/api/v1/organizer/bookings/EVP-8F3K2A/check-in"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Business Rule Violation");
    }

    @Test
    @WithMockUser(username = ORGANIZER, roles = "ORGANIZER")
    void listEventBookings_whenNotOwned_returns404() {
        when(organizerBookingService.listEventBookings(eq(ORGANIZER), eq(99L), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Event", 99L));

        assertThat(mvc.get().uri("/api/v1/organizer/events/99/bookings"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Resource Not Found");
    }
}