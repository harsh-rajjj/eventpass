package com.eventpass.event;

import com.eventpass.common.PageResponse;
import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.security.JwtAuthenticationFilter;
import com.eventpass.security.JwtService;
import com.eventpass.security.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class EventControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private EventService eventService;

    @Test
    void getPublishedEvents_returns200WithPageResponse() {
        var summary = new EventSummary(
                1L, "Test Event", EventCategory.TECH,
                "Mumbai", Instant.parse("2027-06-01T10:00:00Z"),
                "Test Org"
        );
        var pageResponse = new PageResponse<>(List.of(summary), 0, 10, 1L, 1, true, true);

        when(eventService.searchPublishedEvents(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pageResponse);

        assertThat(mvc.get().uri("/api/v1/events"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.content[0].title").isEqualTo("Test Event");
                    assertThat(json).extractingPath("$.page").isEqualTo(0);
                    assertThat(json).extractingPath("$.size").isEqualTo(10);
                    assertThat(json).extractingPath("$.totalElements").isEqualTo(1);
                });
    }

    @Test
    void getPublishedEvents_returnsOrganizerName() {
        var summary = new EventSummary(
                1L, "Test Event", EventCategory.TECH,
                "Mumbai", Instant.parse("2027-06-01T10:00:00Z"),
                "Test Org"
        );
        var pageResponse = new PageResponse<>(List.of(summary), 0, 10, 1L, 1, true, true);

        when(eventService.searchPublishedEvents(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pageResponse);

        assertThat(mvc.get().uri("/api/v1/events"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content[0].organizerName")
                .isEqualTo("Test Org");
    }

    @Test
    void getPublishedEvents_withQueryParamsAndPagination_passesFiltersAndPageableToService() {
        Instant from = Instant.parse("2027-05-01T00:00:00Z");
        Instant to = Instant.parse("2027-07-01T00:00:00Z");
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "startsAt"));

        var summary = new EventSummary(
                1L, "Spring Boot Summit", EventCategory.TECH,
                "Mumbai", Instant.parse("2027-06-01T10:00:00Z"),
                "Test Org"
        );
        var pageResponse = new PageResponse<>(List.of(summary), 0, 10, 1L, 1, true, true);

        when(eventService.searchPublishedEvents(
                eq("Mumbai"), eq(EventCategory.TECH), eq(from), eq(to), eq("Spring"), eq(expectedPageable)
        )).thenReturn(pageResponse);

        assertThat(mvc.get()
                .uri("/api/v1/events")
                .param("city", "Mumbai")
                .param("category", "TECH")
                .param("from", "2027-05-01T00:00:00Z")
                .param("to", "2027-07-01T00:00:00Z")
                .param("q", "Spring")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "startsAt,asc"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content[0].title")
                .isEqualTo("Spring Boot Summit");

        verify(eventService).searchPublishedEvents(
                "Mumbai", EventCategory.TECH, from, to, "Spring", expectedPageable
        );
    }

    @Test
    void getEvent_returns200WithDetail() {
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
