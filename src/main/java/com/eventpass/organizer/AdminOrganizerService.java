package com.eventpass.organizer;

import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.CacheConfig;
import com.eventpass.common.PageResponse;
import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.event.EventRepository;
import com.eventpass.event.EventStatus;
import com.eventpass.logging.Audited;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrganizerService {

    private final OrganizerRepository organizerRepository;
    private final EventRepository eventRepository;

    public PageResponse<OrganizerResponse> listOrganizers(OrganizerStatus status, Pageable pageable) {
        return PageResponse.from(
                organizerRepository.findAllByStatusOptional(status, pageable),
                OrganizerResponse::from);
    }

    @Audited(action = "APPROVE_ORGANIZER")
    @Transactional
    public OrganizerResponse approve(Long organizerId) {
        Organizer organizer = findOrganizer(organizerId);

        if (organizer.getStatus() == OrganizerStatus.APPROVED) {
            throw new BusinessRuleException("Organizer with id " + organizerId + " is already APPROVED");
        }

        organizer.setStatus(OrganizerStatus.APPROVED);
        log.info("Approved organizer id={} name={}", organizer.getId(), organizer.getName());
        return OrganizerResponse.from(organizer);
    }

    @Audited(action = "SUSPEND_ORGANIZER")
    @CacheEvict(cacheNames = CacheConfig.EVENT_DETAILS_CACHE, allEntries = true)
    @Transactional
    public SuspendOrganizerResponse suspend(Long organizerId) {
        Organizer organizer = findOrganizer(organizerId);

        if (organizer.getStatus() == OrganizerStatus.SUSPENDED) {
            throw new BusinessRuleException("Organizer with id " + organizerId + " is already SUSPENDED");
        }

        // Update managed entity before the bulk UPDATE so flushAutomatically writes it before clearAutomatically detaches it
        organizer.setStatus(OrganizerStatus.SUSPENDED);

        int unpublishedCount = eventRepository.updatePublishedEventsStatusByOrganizerId(
                organizerId, EventStatus.DRAFT);

        log.info("Suspended organizer id={} and unpublished {} events", organizerId, unpublishedCount);
        return new SuspendOrganizerResponse(OrganizerResponse.from(organizer), unpublishedCount);
    }

    private Organizer findOrganizer(Long organizerId) {
        return organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer", organizerId));
    }
}
