package com.eventpass.support;

import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.PageResponse;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(AdminSupportTicketController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class AdminSupportTicketControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SupportTicketService supportTicketService;

    @Test
    void listTickets_whenUnauthenticated_returns401ProblemDetailJson() {
        assertThat(mvc.get().uri("/api/v1/admin/support-tickets"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.title").isEqualTo("Unauthorized");
                    assertThat(json).extractingPath("$.status").isEqualTo(401);
                    assertThat(json).extractingPath("$.instance").isEqualTo("/api/v1/admin/support-tickets");
                });

        verifyNoInteractions(supportTicketService);
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void listTickets_whenAttendeeRole_returns403ProblemDetailJson() {
        assertThat(mvc.get().uri("/api/v1/admin/support-tickets"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.title").isEqualTo("Forbidden");
                    assertThat(json).extractingPath("$.status").isEqualTo(403);
                    assertThat(json).extractingPath("$.instance").isEqualTo("/api/v1/admin/support-tickets");
                });

        verifyNoInteractions(supportTicketService);
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void listTickets_whenOrganizerRole_returns403ProblemDetailJson() {
        assertThat(mvc.get().uri("/api/v1/admin/support-tickets"))
                .hasStatus(HttpStatus.FORBIDDEN);

        verifyNoInteractions(supportTicketService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listTickets_whenAdminRole_returns200WithPageResponse() {
        var ticket = new SupportTicketResponse(
                10L,
                "Aarav Sharma",
                "aarav@example.com",
                "Payment issue",
                "Need receipt for my booking",
                SupportTicketStatus.NEW,
                Instant.parse("2026-10-29T10:00:00Z"),
                null
        );
        var pageResponse = new PageResponse<>(List.of(ticket), 0, 10, 1L, 1, true, true);

        when(supportTicketService.listTickets(eq(SupportTicketStatus.NEW), any(Pageable.class)))
                .thenReturn(pageResponse);

        assertThat(mvc.get()
                .uri("/api/v1/admin/support-tickets")
                .param("status", "NEW"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.content[0].id").isEqualTo(10);
                    assertThat(json).extractingPath("$.content[0].status").isEqualTo("NEW");
                    assertThat(json).extractingPath("$.totalElements").isEqualTo(1);
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateTicketStatus_whenValidNewToClosed_returns200WithClosedTicket() {
        var closedResponse = new SupportTicketResponse(
                10L,
                "Aarav Sharma",
                "aarav@example.com",
                "Payment issue",
                "Need receipt for my booking",
                SupportTicketStatus.CLOSED,
                Instant.parse("2026-10-29T10:00:00Z"),
                Instant.parse("2026-10-29T12:00:00Z")
        );

        when(supportTicketService.updateTicketStatus(eq(10L), any(UpdateSupportTicketStatusRequest.class)))
                .thenReturn(closedResponse);

        assertThat(mvc.patch()
                .uri("/api/v1/admin/support-tickets/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "status": "CLOSED"
                        }
                        """))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.status").isEqualTo("CLOSED");
                    assertThat(json).extractingPath("$.closedAt").isEqualTo("2026-10-29T12:00:00Z");
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateTicketStatus_whenAlreadyClosed_returns409BusinessRuleViolation() {
        when(supportTicketService.updateTicketStatus(eq(10L), any(UpdateSupportTicketStatusRequest.class)))
                .thenThrow(new BusinessRuleException("Support ticket with id 10 is already CLOSED"));

        assertThat(mvc.patch()
                .uri("/api/v1/admin/support-tickets/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "status": "CLOSED"
                        }
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.title").isEqualTo("Business Rule Violation");
                    assertThat(json).extractingPath("$.status").isEqualTo(409);
                    assertThat(json).extractingPath("$.detail").isEqualTo("Support ticket with id 10 is already CLOSED");
                });
    }
}
