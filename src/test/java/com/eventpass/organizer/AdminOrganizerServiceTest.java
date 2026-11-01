package com.eventpass.organizer;

import com.eventpass.TestContainersConfiguration;
import com.eventpass.common.BusinessRuleException;
import com.eventpass.event.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// @DataJpaTest only loads JPA beans, so we @Import the service under test
// plus a test-only bean that forces a failure after suspension.
@DataJpaTest
@Import({
        TestContainersConfiguration.class,
        AdminOrganizerService.class,
        AdminOrganizerServiceTest.FailingSuspensionService.class
})
class AdminOrganizerServiceTest {

    private static final Long SEED_ORGANIZER_ID = 1L; // "Neon Festivals" — has PUBLISHED events

    @Autowired
    private AdminOrganizerService adminOrganizerService;

    @Autowired
    private FailingSuspensionService failingSuspensionService;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private EventRepository eventRepository;

    /**
     * Happy path (runs inside the default @DataJpaTest transaction, rolled back afterwards).
     * Organizer becomes SUSPENDED and ALL their published events are unpublished.
     */
    @Test
    void suspend_marksOrganizerSuspendedAndUnpublishesAllEvents() {
        long publishedBefore = eventRepository.countPublishedEventsByOrganizerId(SEED_ORGANIZER_ID);
        assertThat(publishedBefore).isPositive();

        SuspendOrganizerResponse response = adminOrganizerService.suspend(SEED_ORGANIZER_ID);

        assertThat(response.organizer().status()).isEqualTo(OrganizerStatus.SUSPENDED);
        assertThat(response.unpublishedEventCount()).isEqualTo((int) publishedBefore);

        // clearAutomatically emptied the persistence context, so this re-reads from MySQL
        Organizer reloaded = organizerRepository.findById(SEED_ORGANIZER_ID).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrganizerStatus.SUSPENDED);
        assertThat(eventRepository.countPublishedEventsByOrganizerId(SEED_ORGANIZER_ID)).isZero();
    }

    @Test
    void suspend_whenAlreadySuspended_throwsBusinessRuleException() {
        adminOrganizerService.suspend(SEED_ORGANIZER_ID);

        assertThatThrownBy(() -> adminOrganizerService.suspend(SEED_ORGANIZER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Organizer with id 1 is already SUSPENDED");
    }

    @Test
    void approve_reinstatesSuspendedOrganizer_andRejectsSecondApproval() {
        adminOrganizerService.suspend(SEED_ORGANIZER_ID);

        OrganizerResponse approved = adminOrganizerService.approve(SEED_ORGANIZER_ID);
        assertThat(approved.status()).isEqualTo(OrganizerStatus.APPROVED);

        assertThatThrownBy(() -> adminOrganizerService.approve(SEED_ORGANIZER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Organizer with id 1 is already APPROVED");
    }

    /**
     * ROLLBACK PROOF (BR6).
     * NOT_SUPPORTED disables the test transaction, so the service commits/rolls back for real.
     * The organizer UPDATE and the bulk events UPDATE both execute in MySQL,
     * then an exception is thrown — and BOTH must be undone.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void suspend_whenFailureOccursMidTransaction_rollsBackOrganizerAndEvents() {
        OrganizerStatus statusBefore = organizerRepository.findById(SEED_ORGANIZER_ID)
                .orElseThrow()
                .getStatus();
        long publishedBefore = eventRepository.countPublishedEventsByOrganizerId(SEED_ORGANIZER_ID);

        // Preconditions: otherwise the test would pass without proving anything
        assertThat(statusBefore).isNotEqualTo(OrganizerStatus.SUSPENDED);
        assertThat(publishedBefore).isPositive();

        assertThatThrownBy(() -> failingSuspensionService.suspendThenFail(SEED_ORGANIZER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Simulated failure after suspension");

        // Nothing changed: organizer status AND published event count are exactly as before
        Organizer afterRollback = organizerRepository.findById(SEED_ORGANIZER_ID).orElseThrow();
        assertThat(afterRollback.getStatus()).isEqualTo(statusBefore);
        assertThat(eventRepository.countPublishedEventsByOrganizerId(SEED_ORGANIZER_ID))
                .isEqualTo(publishedBefore);
    }

    /**
     * Test-only bean: opens an outer transaction, lets suspend() JOIN it (REQUIRED),
     * then throws an unchecked exception so the whole unit of work rolls back.
     */
    static class FailingSuspensionService {

        private final AdminOrganizerService adminOrganizerService;

        FailingSuspensionService(AdminOrganizerService adminOrganizerService) {
            this.adminOrganizerService = adminOrganizerService;
        }

        @Transactional
        public void suspendThenFail(Long organizerId) {
            adminOrganizerService.suspend(organizerId);
            throw new IllegalStateException("Simulated failure after suspension");
        }
    }
}
