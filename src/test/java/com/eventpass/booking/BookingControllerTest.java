package com.eventpass.booking;

import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.PageResponse;
import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.common.SoldOutException;
import com.eventpass.security.JwtAuthenticationFilter;
import com.eventpass.security.JwtService;
import com.eventpass.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

@WebMvcTest(BookingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class BookingControllerTest {

    private static final String ATTENDEE = "attendee@eventpass.test";

    private static final String BOOKING_JSON = """
            {
                "ticketTierId": 7,
                "quantity": 2
            }
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private BookingService bookingService;

    private BookingResponse booking(BookingStatus status) {
        return new BookingResponse(
                "EVP-8F3K2A", 1L, "Neon Sunburn 2027", Instant.parse("2027-02-14T14:00:00Z"),
                7L, "General Admission", 2, new BigDecimal("2999.00"), new BigDecimal("5998.00"),
                status, Instant.parse("2026-11-26T10:00:00Z"), null, null
        );
    }

    @Test
    void createBooking_whenUnauthenticated_returns401() {
        assertThat(mvc.post().uri("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BOOKING_JSON))
                .hasStatus(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(bookingService);
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void createBooking_whenOrganizerRole_returns403() {
        assertThat(mvc.post().uri("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BOOKING_JSON))
                .hasStatus(HttpStatus.FORBIDDEN);

        verifyNoInteractions(bookingService);
    }

    @Test
    @WithMockUser(username = ATTENDEE, roles = "ATTENDEE")
    void createBooking_whenAttendee_returns201WithLocation() {
        when(bookingService.createBooking(eq(ATTENDEE), any(CreateBookingRequest.class)))
                .thenReturn(booking(BookingStatus.CONFIRMED));

        assertThat(mvc.post().uri("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BOOKING_JSON))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/v1/bookings/EVP-8F3K2A")
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.bookingRef").isEqualTo("EVP-8F3K2A");
                    assertThat(json).extractingPath("$.status").isEqualTo("CONFIRMED");
                    assertThat(json).extractingPath("$.quantity").isEqualTo(2);
                });
    }

    @Test
    @WithMockUser(username = ATTENDEE, roles = "ATTENDEE")
    void createBooking_whenQuantityAboveSix_returns400() {
        String tooMany = """
                {
                    "ticketTierId": 7,
                    "quantity": 7
                }
                """;

        assertThat(mvc.post().uri("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tooMany))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Validation Error");

        verifyNoInteractions(bookingService);
    }

    @Test
    @WithMockUser(username = ATTENDEE, roles = "ATTENDEE")
    void createBooking_whenSoldOut_returns409SoldOut() {
        when(bookingService.createBooking(eq(ATTENDEE), any(CreateBookingRequest.class)))
                .thenThrow(new SoldOutException("Not enough seats available for tier: General Admission"));

        assertThat(mvc.post().uri("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BOOKING_JSON))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Sold Out");
    }

    @Test
    @WithMockUser(username = ATTENDEE, roles = "ATTENDEE")
    void listMyBookings_returnsPagedBookings() {
        var page = new PageResponse<>(List.of(booking(BookingStatus.CONFIRMED)), 0, 10, 1L, 1, true, true);
        when(bookingService.listMyBookings(eq(ATTENDEE), any(Pageable.class))).thenReturn(page);

        assertThat(mvc.get().uri("/api/v1/me/bookings"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content[0].bookingRef")
                .isEqualTo("EVP-8F3K2A");
    }

    @Test
    @WithMockUser(username = ATTENDEE, roles = "ATTENDEE")
    void getMyBooking_whenNotOwned_returns404() {
        when(bookingService.getMyBooking(ATTENDEE, "EVP-OTHER1"))
                .thenThrow(new ResourceNotFoundException("Booking not found: EVP-OTHER1"));

        assertThat(mvc.get().uri("/api/v1/bookings/EVP-OTHER1"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Resource Not Found");
    }

    @Test
    @WithMockUser(username = ATTENDEE, roles = "ATTENDEE")
    void cancelBooking_whenInsideCutoff_returns409() {
        when(bookingService.cancelBooking(ATTENDEE, "EVP-8F3K2A"))
                .thenThrow(new BusinessRuleException(
                        "Bookings can only be cancelled at least 24 hours before event start"));

        assertThat(mvc.post().uri("/api/v1/bookings/EVP-8F3K2A/cancel"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Business Rule Violation");
    }
}
