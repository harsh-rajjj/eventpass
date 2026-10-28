package com.eventpass.event;

import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.security.JwtAuthenticationFilter;
import com.eventpass.security.JwtService;
import com.eventpass.security.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// @WebMvcTest loads ONLY the web layer for EventController.
// No real service, no DB — we mock the service with @MockitoBean.
// MockMvcTester is Spring Boot 4's fluent API for testing HTTP calls.
@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class EventControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean                       // replaces the real EventService with a mock
    private EventService eventService;

    @Test
    void getPublishedEvents_returns200WithList() {
        // Arrange: tell the mock what to return
        var summary = new EventSummary(
                1L, "Test Event", EventCategory.TECH,
                "Mumbai", Instant.parse("2027-06-01T10:00:00Z"),
                "Test Org"
        );
        when(eventService.searchPublishedEvents(null, null, null, null, null))
                .thenReturn(List.of(summary));

        // Act + Assert
        assertThat(mvc.get().uri("/api/v1/events"))
                .hasStatusOk()                              // 200
                .bodyJson()
                .extractingPath("$[0].title")               // first element's title
                .isEqualTo("Test Event");
    }

    @Test
    void getPublishedEvents_returnsOrganizerName() {
        var summary = new EventSummary(
                1L, "Test Event", EventCategory.TECH,
                "Mumbai", Instant.parse("2027-06-01T10:00:00Z"),
                "Test Org"
        );
        when(eventService.searchPublishedEvents(null, null, null, null, null))
                .thenReturn(List.of(summary));

        assertThat(mvc.get().uri("/api/v1/events"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].organizerName")
                .isEqualTo("Test Org");
    }

    @Test
    void getPublishedEvents_withQueryParams_passesFiltersToService() {
        Instant from = Instant.parse("2027-05-01T00:00:00Z");
        Instant to = Instant.parse("2027-07-01T00:00:00Z");
        var summary = new EventSummary(
                1L, "Spring Boot Summit", EventCategory.TECH,
                "Mumbai", Instant.parse("2027-06-01T10:00:00Z"),
                "Test Org"
        );

        when(eventService.searchPublishedEvents("Mumbai", EventCategory.TECH, from, to, "Spring"))
                .thenReturn(List.of(summary));

        assertThat(mvc.get()
                .uri("/api/v1/events")
                .param("city", "Mumbai")
                .param("category", "TECH")
                .param("from", "2027-05-01T00:00:00Z")
                .param("to", "2027-07-01T00:00:00Z")
                .param("q", "Spring"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].title")
                .isEqualTo("Spring Boot Summit");

        verify(eventService).searchPublishedEvents("Mumbai", EventCategory.TECH, from, to, "Spring");
    }

    @Test
    void getEvent_returns200WithDetail() {
        // Arrange
        var tier = new TicketTierResponse(1L, "VIP", new BigDecimal("5000.00"), 20);
        var detail = new EventDetailResponse(
                1L, "Test Event", "Description", EventCategory.TECH,
                "Venue", "Mumbai", "Address",
                Instant.parse("2027-06-01T10:00:00Z"),
                Instant.parse("2027-06-01T18:00:00Z"),
                EventStatus.PUBLISHED, null, "Test Org",
                List.of(tier)
        );
        when(eventService.findEventById(1L)).thenReturn(detail);

        // Act + Assert
        assertThat(mvc.get().uri("/api/v1/events/1"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Test Event");
    }

    @Test
    void getEvent_returnsTiersWithAvailability() {
        var tier = new TicketTierResponse(1L, "VIP", new BigDecimal("5000.00"), 20);
        var detail = new EventDetailResponse(
                1L, "Test Event", "Description", EventCategory.TECH,
                "Venue", "Mumbai", "Address",
                Instant.parse("2027-06-01T10:00:00Z"),
                Instant.parse("2027-06-01T18:00:00Z"),
                EventStatus.PUBLISHED, null, "Test Org",
                List.of(tier)
        );
        when(eventService.findEventById(1L)).thenReturn(detail);

        assertThat(mvc.get().uri("/api/v1/events/1"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ticketTiers[0].available")
                .isEqualTo(20);
    }

    @Test
    void getEvent_returns404ProblemDetailWhenNotFound() {
        when(eventService.findEventById(999L))
                .thenThrow(new ResourceNotFoundException("Event", 999L));

        assertThat(mvc.get().uri("/api/v1/events/999"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.title").isEqualTo("Resource Not Found");
                    assertThat(json).extractingPath("$.status").isEqualTo(404);
                    assertThat(json).extractingPath("$.detail").isEqualTo("Event with id 999 not found");
                });
    }
}
