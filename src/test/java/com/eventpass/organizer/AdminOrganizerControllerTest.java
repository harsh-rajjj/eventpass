package com.eventpass.organizer;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(AdminOrganizerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class AdminOrganizerControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AdminOrganizerService adminOrganizerService;

    private OrganizerResponse organizer(OrganizerStatus status) {
        return new OrganizerResponse(
                1L, "Neon Festivals", "Music festivals", "https://neon.example.com",
                "hello@neon.example.com", status
        );
    }

    @Test
    void suspend_whenUnauthenticated_returns401() {
        assertThat(mvc.post().uri("/api/v1/admin/organizers/1/suspend"))
                .hasStatus(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(adminOrganizerService);
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void suspend_whenOrganizerRole_returns403() {
        assertThat(mvc.post().uri("/api/v1/admin/organizers/1/suspend"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Forbidden");

        verifyNoInteractions(adminOrganizerService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listOrganizers_whenAdmin_returnsPagedPendingOrganizers() {
        var page = new PageResponse<>(List.of(organizer(OrganizerStatus.PENDING)), 0, 10, 1L, 1, true, true);
        when(adminOrganizerService.listOrganizers(eq(OrganizerStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);

        assertThat(mvc.get().uri("/api/v1/admin/organizers").param("status", "PENDING"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.content[0].name").isEqualTo("Neon Festivals");
                    assertThat(json).extractingPath("$.content[0].status").isEqualTo("PENDING");
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approve_whenAdmin_returns200Approved() {
        when(adminOrganizerService.approve(1L)).thenReturn(organizer(OrganizerStatus.APPROVED));

        assertThat(mvc.post().uri("/api/v1/admin/organizers/1/approve"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("APPROVED");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void suspend_whenAdmin_returnsSuspendedOrganizerAndUnpublishedCount() {
        when(adminOrganizerService.suspend(1L))
                .thenReturn(new SuspendOrganizerResponse(organizer(OrganizerStatus.SUSPENDED), 3));

        assertThat(mvc.post().uri("/api/v1/admin/organizers/1/suspend"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.organizer.status").isEqualTo("SUSPENDED");
                    assertThat(json).extractingPath("$.unpublishedEventCount").isEqualTo(3);
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void suspend_whenAlreadySuspended_returns409() {
        when(adminOrganizerService.suspend(1L))
                .thenThrow(new BusinessRuleException("Organizer with id 1 is already SUSPENDED"));

        assertThat(mvc.post().uri("/api/v1/admin/organizers/1/suspend"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Business Rule Violation");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approve_whenOrganizerMissing_returns404() {
        when(adminOrganizerService.approve(99L))
                .thenThrow(new ResourceNotFoundException("Organizer", 99L));

        assertThat(mvc.post().uri("/api/v1/admin/organizers/99/approve"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Resource Not Found");
    }
}
