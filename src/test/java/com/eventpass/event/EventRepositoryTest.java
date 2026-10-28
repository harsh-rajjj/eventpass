package com.eventpass.event;

import com.eventpass.TestContainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Comparator;
import java.util.List;
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
    void findWithDetailsById_shouldLoadEventOrganizerAndTicketTiersInSingleGraph() {
        // Event 1 = "Neon Sunburn 2027" — has 3 tiers
        Optional<Event> result = eventRepository.findWithDetailsById(1L);

        assertThat(result).isPresent();

        Event event = result.get();
        assertThat(event.getTitle()).isEqualTo("Neon Sunburn 2027");
        assertThat(event.getOrganizer().getName()).isEqualTo("Neon Festivals");
        assertThat(event.getTicketTiers()).hasSize(3);
    }

    @Test
    void shouldReturnEmptyForNonExistentEvent() {
        assertThat(eventRepository.findWithDetailsById(999L)).isEmpty();
    }

    @Test
    void searchPublishedEvents_whenNoFilters_returnsOnlyPublishedSortedByStartsAt() {
        List<Event> results = eventRepository.searchPublishedEvents(null, null, null, null, null);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(e -> e.getStatus() == EventStatus.PUBLISHED);
        assertThat(results).isSortedAccordingTo(Comparator.comparing(Event::getStartsAt));
        // Verify JOIN FETCH loaded the organizer without LazyInitializationException
        assertThat(results.getFirst().getOrganizer().getName()).isNotBlank();
    }

    @Test
    void searchPublishedEvents_withCombinedFilters_matchesTargetEvent() {
        Event target = eventRepository.findWithDetailsById(1L).orElseThrow();

        List<Event> results = eventRepository.searchPublishedEvents(
                target.getCity().toLowerCase(),               // case-insensitive city check
                target.getCategory(),                         // category enum check
                target.getStartsAt().minusSeconds(3600),      // fromDate before event start
                target.getStartsAt().plusSeconds(3600),       // toDate after event start
                "Sunburn"                                     // keyword in title "Neon Sunburn 2027"
        );

        assertThat(results)
                .extracting(Event::getId)
                .contains(1L);
    }

    @Test
    void findByOrganizerIdOrderByStartsAtAsc_executesNamedQuery() {
        List<Event> events = eventRepository.findByOrganizerIdOrderByStartsAtAsc(1L);

        assertThat(events).isNotEmpty();
        assertThat(events).allMatch(e -> e.getOrganizer().getId().equals(1L));
        assertThat(events).isSortedAccordingTo(Comparator.comparing(Event::getStartsAt));
    }

    @Test
    void countPublishedEventsByOrganizerId_executesNativeQuery() {
        long publishedCount = eventRepository.countPublishedEventsByOrganizerId(1L);

        long expectedCount = eventRepository.findByOrganizerIdOrderByStartsAtAsc(1L).stream()
                .filter(e -> e.getStatus() == EventStatus.PUBLISHED)
                .count();

        assertThat(publishedCount).isPositive().isEqualTo(expectedCount);
    }
}
