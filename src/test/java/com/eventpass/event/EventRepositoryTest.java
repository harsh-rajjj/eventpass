package com.eventpass.event;

import com.eventpass.TestContainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest loads ONLY the JPA layer (entities, repositories, Flyway).
// No controllers, no services — just the database.
// @Import brings in our Testcontainers config so it uses a real MySQL container.
@DataJpaTest
@Import(TestContainersConfiguration.class)
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Test
    void shouldLoadAllEventsFromSeedData() {
        // V2 seed inserts 12 events (PUBLISHED + DRAFT + CANCELLED)
        assertThat(eventRepository.findAll()).hasSize(12);
    }

    @Test
    void shouldLoadEventWithTicketTiers() {
        // Event 1 = "Neon Sunburn 2027" — has 3 tiers
        Optional<Event> result = eventRepository.findById(1L);

        assertThat(result).isPresent();

        Event event = result.get();
        assertThat(event.getTitle()).isEqualTo("Neon Sunburn 2027");
        assertThat(event.getOrganizer().getName()).isEqualTo("Neon Festivals");
        assertThat(event.getTicketTiers()).hasSize(3);
    }

    @Test
    void shouldReturnEmptyForNonExistentEvent() {
        assertThat(eventRepository.findById(999L)).isEmpty();
    }
}
