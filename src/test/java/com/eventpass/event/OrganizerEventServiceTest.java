package com.eventpass.event;

import com.eventpass.TestContainersConfiguration;
import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.JpaAuditingConfig;
import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.organizer.Organizer;
import com.eventpass.organizer.OrganizerRepository;
import com.eventpass.organizer.OrganizerService;
import com.eventpass.organizer.OrganizerStatus;
import com.eventpass.user.Role;
import com.eventpass.user.User;
import com.eventpass.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
        TestContainersConfiguration.class,
        JpaAuditingConfig.class,
        OrganizerService.class,
        OrganizerEventService.class
})
class OrganizerEventServiceTest {

    private static final String ORG_A_EMAIL = "ownerA@eventpass.test";
    private static final String ORG_B_EMAIL = "ownerB@eventpass.test";
    private static final String PENDING_ORG_EMAIL = "pending@eventpass.test";

    @Autowired
    private OrganizerEventService organizerEventService;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUpOrganizers() {
        createOrganizerUser("Alpha Events", ORG_A_EMAIL, OrganizerStatus.APPROVED);
        createOrganizerUser("Beta Live", ORG_B_EMAIL, OrganizerStatus.APPROVED);
        createOrganizerUser("Pending Studio", PENDING_ORG_EMAIL, OrganizerStatus.PENDING);
    }

    @Test
    void otherOrganizer_cannotModifyPublishDeleteOrAddTiersToAnotherOrganizersEvent_BR7() {
        EventDetailResponse createdByA = organizerEventService.createDraftEvent(ORG_A_EMAIL, validEventRequest());
        Long eventId = createdByA.id();

        assertThatThrownBy(() -> organizerEventService.updateEvent(ORG_B_EMAIL, eventId, validEventRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> organizerEventService.publishEvent(ORG_B_EMAIL, eventId))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> organizerEventService.deleteDraftEvent(ORG_B_EMAIL, eventId))
                .isInstanceOf(ResourceNotFoundException.class);

        var tierReq = new UpsertTicketTierRequest("VIP", new BigDecimal("99.00"), 50);
        assertThatThrownBy(() -> organizerEventService.addTier(ORG_B_EMAIL, eventId, tierReq))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void publishEvent_enforcesApprovedOrganizerAndAtLeastOneTier_BR5() {
        // 1. PENDING organizer cannot publish even with a tier
        EventDetailResponse pendingDraft = organizerEventService.createDraftEvent(PENDING_ORG_EMAIL, validEventRequest());
        organizerEventService.addTier(
                PENDING_ORG_EMAIL,
                pendingDraft.id(),
                new UpsertTicketTierRequest("General", new BigDecimal("25.00"), 100)
        );

        assertThatThrownBy(() -> organizerEventService.publishEvent(PENDING_ORG_EMAIL, pendingDraft.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Organizer must be APPROVED before publishing events");

        // 2. APPROVED organizer cannot publish a draft with zero tiers
        EventDetailResponse approvedDraft = organizerEventService.createDraftEvent(ORG_A_EMAIL, validEventRequest());
        assertThatThrownBy(() -> organizerEventService.publishEvent(ORG_A_EMAIL, approvedDraft.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Event must have at least one ticket tier before publishing");

        // 3. Adding >= 1 tier allows APPROVED organizer to publish
        organizerEventService.addTier(
                ORG_A_EMAIL,
                approvedDraft.id(),
                new UpsertTicketTierRequest("VIP", new BigDecimal("150.00"), 40)
        );
        EventDetailResponse published = organizerEventService.publishEvent(ORG_A_EMAIL, approvedDraft.id());

        assertThat(published.status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(published.ticketTiers()).hasSize(1);
    }

    @Test
    void publishedEvent_cannotBeDeleted_mustBeCancelledInstead_BR8() {
        EventDetailResponse draft = organizerEventService.createDraftEvent(ORG_A_EMAIL, validEventRequest());
        organizerEventService.addTier(
                ORG_A_EMAIL,
                draft.id(),
                new UpsertTicketTierRequest("Standard", new BigDecimal("40.00"), 100)
        );
        organizerEventService.publishEvent(ORG_A_EMAIL, draft.id());

        assertThatThrownBy(() -> organizerEventService.deleteDraftEvent(ORG_A_EMAIL, draft.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Only DRAFT events can be deleted; published events must be cancelled");

        EventDetailResponse cancelled = organizerEventService.cancelEvent(ORG_A_EMAIL, draft.id());
        assertThat(cancelled.status()).isEqualTo(EventStatus.CANCELLED);
    }

    private void createOrganizerUser(String orgName, String email, OrganizerStatus status) {
        Organizer org = new Organizer();
        org.setName(orgName);
        org.setContactEmail(email);
        org.setStatus(status);
        Organizer savedOrg = organizerRepository.save(org);

        User user = new User();
        user.setFullName(orgName + " Owner");
        user.setEmail(email);
        user.setPasswordHash("$2a$10$dummyHashForSliceTest");
        user.setRole(Role.ORGANIZER);
        user.setOrganizer(savedOrg);
        userRepository.save(user);
    }

    private UpsertEventRequest validEventRequest() {
        return new UpsertEventRequest(
                "Cloud Native Conf",
                "Backend engineering conference",
                EventCategory.TECH,
                "Convention Center",
                "Bengaluru",
                "MG Road",
                Instant.parse("2027-09-10T09:00:00Z"),
                Instant.parse("2027-09-10T17:00:00Z")
        );
    }
}
