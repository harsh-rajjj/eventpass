package com.eventpass.organizer;

import com.eventpass.TestContainersConfiguration;
import com.eventpass.common.BusinessRuleException;
import com.eventpass.event.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TestContainersConfiguration.class, AdminOrganizerService.class})
class AdminOrganizerServiceTest {

    private static final Long SEED_ORGANIZER_ID = 1L;

    @Autowired
    private AdminOrganizerService adminOrganizerService;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void suspend_marksOrganizerSuspendedAndUnpublishesAllEvents() {
        long publishedBefore = eventRepository.countPublishedEventsByOrganizerId(SEED_ORGANIZER_ID);
        assertThat(publishedBefore).isPositive();

        SuspendOrganizerResponse response = adminOrganizerService.suspend(SEED_ORGANIZER_ID);

        assertThat(response.organizer().status()).isEqualTo(OrganizerStatus.SUSPENDED);
        assertThat(response.unpublishedEventCount()).isEqualTo((int) publishedBefore);

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
}
