package com.eventpass.auth;

import com.eventpass.common.DuplicateEmailException;
import com.eventpass.organizer.Organizer;
import com.eventpass.organizer.OrganizerRepository;
import com.eventpass.security.JwtService;
import com.eventpass.user.Role;
import com.eventpass.user.User;
import com.eventpass.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.authentication.password.CompromisedPasswordException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizerRepository organizerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompromisedPasswordChecker compromisedPasswordChecker;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public UserResponse registerAttendee(RegisterAttendeeRequest request) {
        String email = normalizeEmail(request.email());
        validateNewUser(email, request.password());

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.ATTENDEE);

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    @Transactional // Saves both Organizer and User in ONE database transaction
    public UserResponse registerOrganizer(RegisterOrganizerRequest request) {
        String email = normalizeEmail(request.email());
        validateNewUser(email, request.password());

        // 1. Create the organization (status defaults to OrganizerStatus.PENDING)
        Organizer organizer = new Organizer();
        organizer.setName(request.organizerName());
        organizer.setDescription(request.description());
        organizer.setWebsite(request.website());
        organizer.setContactEmail(normalizeEmail(request.contactEmail()));
        Organizer savedOrganizer = organizerRepository.save(organizer);

        // 2. Create the organizer user linked to that organization
        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.ORGANIZER);
        user.setOrganizer(savedOrganizer);

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        // 1. Triggers CustomUserDetailsService + BCrypt passwordEncoder.matches()
        //    Throws BadCredentialsException (401) automatically if email or password is wrong!
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        // 2. Credentials are valid -> fetch User and generate a signed JWT
        User user = userRepository.findByEmail(email).orElseThrow();
        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return new AuthResponse(token);
    }

    private void validateNewUser(String email, String rawPassword) {
        // Reject duplicate email with 409 Conflict
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        // Reject breached passwords with 400 Bad Request
        CompromisedPasswordDecision decision = compromisedPasswordChecker.check(rawPassword);
        if (decision.isCompromised()) {
            throw new CompromisedPasswordException(
                    "The provided password has appeared in a data breach and cannot be used");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}