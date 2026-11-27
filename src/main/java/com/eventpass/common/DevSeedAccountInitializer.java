package com.eventpass.common;

import com.eventpass.organizer.Organizer;
import com.eventpass.organizer.OrganizerRepository;
import com.eventpass.user.Role;
import com.eventpass.user.User;
import com.eventpass.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevSeedAccountInitializer implements ApplicationRunner {

    private static final String DEV_PASSWORD = "EventPass!Dev2026";

    private final UserRepository userRepository;
    private final OrganizerRepository organizerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUser("EventPass Admin", "admin@eventpass.dev", Role.ADMIN, null);
        seedUser("Aarav Attendee", "attendee@eventpass.dev", Role.ATTENDEE, null);

        Organizer neonFestivals = organizerRepository.findById(1L).orElse(null);
        if (neonFestivals != null) {
            seedUser("Neon Organizer", "organizer@eventpass.dev", Role.ORGANIZER, neonFestivals);
        }
    }

    private void seedUser(String fullName, String email, Role role, Organizer organizer) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(DEV_PASSWORD));
        user.setRole(role);
        user.setOrganizer(organizer);
        userRepository.save(user);
        log.info("Seeded dev account email={} role={}", email, role);
    }
}