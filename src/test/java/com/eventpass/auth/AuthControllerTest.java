package com.eventpass.auth;

import com.eventpass.common.DuplicateEmailException;
import com.eventpass.security.JwtAuthenticationFilter;
import com.eventpass.security.JwtService;
import com.eventpass.security.SecurityConfig;
import com.eventpass.user.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.password.CompromisedPasswordException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class AuthControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerAttendee_returns201WhenValid() {
        var userResponse = new UserResponse(
                1L,
                "Aarav Sharma",
                "aarav@example.com",
                Role.ATTENDEE,
                null,
                Instant.parse("2026-10-25T10:00:00Z")
        );
        when(authService.registerAttendee(any(RegisterAttendeeRequest.class)))
                .thenReturn(userResponse);

        String json = """
                {
                    "fullName": "Aarav Sharma",
                    "email": "aarav@example.com",
                    "password": "EvtPass!92#StrongKey"
                }
                """;

        assertThat(mvc.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .satisfies(body -> {
                    assertThat(body).extractingPath("$.id").isEqualTo(1);
                    assertThat(body).extractingPath("$.email").isEqualTo("aarav@example.com");
                    assertThat(body).extractingPath("$.role").isEqualTo("ATTENDEE");
                });
    }

    @Test
    void registerAttendee_returns409ProblemDetailWhenDuplicateEmail() {
        when(authService.registerAttendee(any(RegisterAttendeeRequest.class)))
                .thenThrow(new DuplicateEmailException("aarav@example.com"));

        String json = """
                {
                    "fullName": "Aarav Sharma",
                    "email": "aarav@example.com",
                    "password": "EvtPass!92#StrongKey"
                }
                """;

        assertThat(mvc.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .satisfies(body -> {
                    assertThat(body).extractingPath("$.title").isEqualTo("Duplicate Email");
                    assertThat(body).extractingPath("$.status").isEqualTo(409);
                });
    }

    @Test
    void registerAttendee_returns400ProblemDetailWhenCompromisedPassword() {
        when(authService.registerAttendee(any(RegisterAttendeeRequest.class)))
                .thenThrow(new CompromisedPasswordException("Password has appeared in a data breach"));

        String json = """
                {
                    "fullName": "Aarav Sharma",
                    "email": "aarav@example.com",
                    "password": "password123"
                }
                """;

        assertThat(mvc.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .satisfies(body -> {
                    assertThat(body).extractingPath("$.title").isEqualTo("Compromised Password");
                    assertThat(body).extractingPath("$.status").isEqualTo(400);
                });
    }

    @Test
    void login_returns200AndAccessTokenWhenCredentialsValid() {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("eyJhbGciOiJIUzI1NiJ9.test.token"));

        String json = """
                {
                    "email": "aarav@example.com",
                    "password": "EvtPass!92#StrongKey"
                }
                """;

        assertThat(mvc.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.accessToken")
                .isEqualTo("eyJhbGciOiJIUzI1NiJ9.test.token");
    }

    @Test
    void login_returns401ProblemDetailWhenBadCredentials() {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        String json = """
                {
                    "email": "aarav@example.com",
                    "password": "WrongPassword123!"
                }
                """;

        assertThat(mvc.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .satisfies(body -> {
                    assertThat(body).extractingPath("$.title").isEqualTo("Authentication Failed");
                    assertThat(body).extractingPath("$.status").isEqualTo(401);
                    assertThat(body).extractingPath("$.detail").isEqualTo("Invalid email or password");
                });
    }
}