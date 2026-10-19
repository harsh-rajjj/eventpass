package com.eventpass.support;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.eventpass.security.JwtAuthenticationFilter;
import com.eventpass.security.JwtService;
import com.eventpass.security.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(SupportTicketController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class SupportTicketControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SupportTicketService supportTicketService;

    @Test
    void createTicket_returns201AndLocationWhenValid() {
        var response = new SupportTicketResponse(
                42L,
                "Aarav Sharma",
                "aarav@example.com",
                "Refund request",
                "Can I cancel my VIP ticket 48 hours before the concert?",
                SupportTicketStatus.NEW,
                Instant.parse("2026-10-17T10:00:00Z")
        );
        when(supportTicketService.createTicket(any(CreateSupportTicketRequest.class)))
                .thenReturn(response);

        String validJson = """
                {
                    "name": "Aarav Sharma",
                    "email": "aarav@example.com",
                    "subject": "Refund request",
                    "message": "Can I cancel my VIP ticket 48 hours before the concert?"
                }
                """;

        assertThat(mvc.post()
                .uri("/api/v1/support-tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/v1/support-tickets/42")
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(42);
    }

    @Test
    void createTicket_returns400ProblemDetailWhenInvalid() {
        String invalidJson = """
                {
                    "name": "",
                    "email": "not-an-email",
                    "subject": "",
                    "message": "short"
                }
                """;

        assertThat(mvc.post()
                .uri("/api/v1/support-tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.title").isEqualTo("Validation Error");
                    assertThat(json).extractingPath("$.status").isEqualTo(400);
                    assertThat(json).extractingPath("$.errors.name").isEqualTo("Name is required");
                    assertThat(json).extractingPath("$.errors.email").isEqualTo("Email must be a valid email address");
                    assertThat(json).extractingPath("$.errors.subject").isEqualTo("Subject is required");
                    assertThat(json).extractingPath("$.errors.message").isNotNull();
                });

        verifyNoInteractions(supportTicketService);
    }
}
