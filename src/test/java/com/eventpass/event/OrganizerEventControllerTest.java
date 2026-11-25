package com.eventpass.event;

import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.organizer.OrganizerController;
import com.eventpass.organizer.OrganizerResponse;
import com.eventpass.organizer.OrganizerService;
import com.eventpass.organizer.OrganizerStatus;
import com.eventpass.security.JwtAuthenticationFilter;
import com.eventpass.security.JwtService;
import com.eventpass.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest({OrganizerController.class, OrganizerEventController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class OrganizerEventControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private OrganizerService organizerService;

    @MockitoBean
    private OrganizerEventService organizerEventService;

    @Test
    void organizerEndpoints_whenUnauthenticated_return401Json() {
        assertThat(mvc.get().uri("/api/v1/organizer/events"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Unauthorized");

        verifyNoInteractions(organizerEventService);
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void organizerEndpoints_whenAttendeeRole_return403Json() {
        assertThat(mvc.get().uri("/api/v1/organizer/events"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Forbidden");

        verifyNoInteractions(organizerEventService);
    }

    @Test
    @WithMockUser(username = "org@neon.com", roles = "ORGANIZER")
    void getMyProfile_whenOrganizer_returns200() {
        var profile = new OrganizerResponse(
                1L, "Neon Festivals", "Music festivals", "https://neon.example.com",
                "hello@neon.example.com", OrganizerStatus.APPROVED
        );
        when(organizerService.getMyProfile("org@neon.com")).thenReturn(profile);

        assertThat(mvc.get().uri("/api/v1/organizer/profile"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Neon Festivals");
    }

    @Test
    @WithMockUser(username = "org@neon.com", roles = "ORGANIZER")
    void createDraftEvent_whenValid_returns201AndLocationHeader() {
        var created = new EventDetailResponse(
                42L, "Spring I/O", "Java conference", EventCategory.TECH,
                "Expo Hall", "Mumbai", "BKC",
                Instant.parse("2027-10-01T09:00:00Z"),
                Instant.parse("2027-10-01T17:00:00Z"),
                EventStatus.DRAFT, null, "Neon Festivals", List.of()
        );
        when(organizerEventService.createDraftEvent(eq("org@neon.com"), any(UpsertEventRequest.class)))
                .thenReturn(created);

        String json = """
                {
                    "title": "Spring I/O",
                    "description": "Java conference",
                    "category": "TECH",
                    "venueName": "Expo Hall",
                    "city": "Mumbai",
                    "address": "BKC",
                    "startsAt": "2027-10-01T09:00:00Z",
                    "endsAt": "2027-10-01T17:00:00Z"
                }
                """;

        assertThat(mvc.post()
                .uri("/api/v1/organizer/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/v1/events/42")
                .bodyJson()
                .satisfies(body -> {
                    assertThat(body).extractingPath("$.id").isEqualTo(42);
                    assertThat(body).extractingPath("$.status").isEqualTo("DRAFT");
                });
    }

    @Test
    @WithMockUser(username = "org@neon.com", roles = "ORGANIZER")
    void createDraftEvent_whenBlankTitle_returns400ValidationError() {
        String invalidJson = """
                {
                    "title": "   ",
                    "category": "TECH",
                    "venueName": "Expo Hall",
                    "city": "Mumbai",
                    "startsAt": "2027-10-01T09:00:00Z",
                    "endsAt": "2027-10-01T17:00:00Z"
                }
                """;

        assertThat(mvc.post()
                .uri("/api/v1/organizer/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Validation Error");

        verifyNoInteractions(organizerEventService);
    }

    @Test
    @WithMockUser(username = "org@neon.com", roles = "ORGANIZER")
    void publishEvent_whenOwnedByAnotherOrganizer_returns404() {
        when(organizerEventService.publishEvent("org@neon.com", 99L))
                .thenThrow(new ResourceNotFoundException("Event", 99L));

        assertThat(mvc.post().uri("/api/v1/organizer/events/99/publish"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Resource Not Found");
    }

    @Test
    @WithMockUser(username = "org@neon.com", roles = "ORGANIZER")
    void deleteDraftEvent_whenAlreadyPublished_returns409Conflict() {
        doThrow(new BusinessRuleException("Only DRAFT events can be deleted; published events must be cancelled"))
                .when(organizerEventService).deleteDraftEvent("org@neon.com", 10L);

        assertThat(mvc.delete().uri("/api/v1/organizer/events/10"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Business Rule Violation");
    }
}
