package com.eventpass.user;

import com.eventpass.TestContainersConfiguration;
import com.eventpass.common.JpaAuditingConfig;
import com.eventpass.user.Role;
import com.eventpass.user.User;
import com.eventpass.user.UserRepository;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestContainersConfiguration.class, JpaAuditingConfig.class})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByEmail_populatesAuditFieldsAndChecksExistence() {
        User user = new User();
        user.setFullName("Aarav Sharma");
        user.setEmail("aarav@example.com");
        user.setPasswordHash("$2a$10$dummyBcryptHashForTest");
        user.setRole(Role.ATTENDEE);

        userRepository.save(user);

        assertThat(userRepository.existsByEmail("aarav@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();

        Optional<User> found = userRepository.findByEmail("aarav@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(Role.ATTENDEE);
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getCreatedBy()).isEqualTo("system");
    }
}