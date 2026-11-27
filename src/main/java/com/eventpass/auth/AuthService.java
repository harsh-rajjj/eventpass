package com.eventpass.auth;

import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.DuplicateEmailException;
import com.eventpass.logging.Audited;
import com.eventpass.organizer.Organizer;
import com.eventpass.organizer.OrganizerRepository;
import com.eventpass.security.JwtService;
import com.eventpass.user.Role;
import com.eventpass.user.User;
import com.eventpass.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.authentication.password.CompromisedPasswordException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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

    @Audited(action = "REGISTER_ATTENDEE")
    @Transactional
    public UserResponse registerAttendee(RegisterAttendeeRequest request) {
        String email = normalizeEmail(request.email());
        validateNewUser(email, request.password());

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.ATTENDEE);

        User saved = userRepository.save(user);
        log.info("Registered attendee id={} email={}", saved.getId(), saved.getEmail());
        return UserResponse.from(saved);
    }

    @Audited(action = "REGISTER_ORGANIZER")
    @Transactional
    public UserResponse registerOrganizer(RegisterOrganizerRequest request) {
        String email = normalizeEmail(request.email());
        String organizerName = request.organizerName().trim();
        validateNewUser(email, request.password());

        if (organizerRepository.existsByNameIgnoreCase(organizerName)) {
            throw new BusinessRuleException("Organizer name '" + organizerName + "' is already in use");
        }

        Organizer organizer = new Organizer();
        organizer.setName(organizerName);
        organizer.setDescription(request.description());
        organizer.setWebsite(request.website());
        organizer.setContactEmail(normalizeEmail(request.contactEmail()));
        Organizer savedOrganizer = organizerRepository.save(organizer);

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.ORGANIZER);
        user.setOrganizer(savedOrganizer);

        User savedUser = userRepository.save(user);
        log.info("Registered organizer user id={} organizerId={}", savedUser.getId(), savedOrganizer.getId());
        return UserResponse.from(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        User user = userRepository.findByEmail(email).orElseThrow();
        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        log.info("User logged in email={} role={}", user.getEmail(), user.getRole());
        return new AuthResponse(token);
    }

    private void validateNewUser(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            log.warn("Registration rejected: duplicate email={}", email);
            throw new DuplicateEmailException(email);
        }

        try {
            CompromisedPasswordDecision decision = compromisedPasswordChecker.check(rawPassword);
            if (decision.isCompromised()) {
                log.warn("Registration rejected: compromised password for email={}", email);
                throw new CompromisedPasswordException(
                        "The provided password has appeared in a data breach and cannot be used");
            }
        } catch (CompromisedPasswordException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Compromised password check unavailable; skipping external lookup: {}", ex.getMessage());
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
