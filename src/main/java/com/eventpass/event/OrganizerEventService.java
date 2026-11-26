package com.eventpass.event;

import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.CacheConfig;
import com.eventpass.common.PageResponse;
import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.logging.Audited;
import com.eventpass.organizer.Organizer;
import com.eventpass.organizer.OrganizerService;
import com.eventpass.organizer.OrganizerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventpass.booking.BookingRepository;
import java.time.Instant;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizerEventService {

    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final OrganizerService organizerService;
    private final BookingRepository bookingRepository;

    public PageResponse<EventSummary> listMyEvents(String userEmail, Pageable pageable) {
        Organizer organizer = organizerService.getOrganizerForUser(userEmail);
        Page<Event> page = eventRepository.findByOrganizerId(organizer.getId(), pageable);
        return PageResponse.from(page, EventMapper::toSummary);
    }

    @Audited(action = "CREATE_DRAFT_EVENT")
    @Transactional
    public EventDetailResponse createDraftEvent(String userEmail, UpsertEventRequest request) {
        Organizer organizer = organizerService.getOrganizerForUser(userEmail);
        validateEventDates(request);

        Event event = new Event();
        event.setOrganizer(organizer);
        applyEventFields(event, request);
        event.setStatus(EventStatus.DRAFT);

        Event saved = eventRepository.save(event);
        log.info("Created draft event id={} organizerId={}", saved.getId(), organizer.getId());
        return EventMapper.toDetail(saved);
    }

    @Audited(action = "UPDATE_EVENT")
    @CacheEvict(cacheNames = CacheConfig.EVENT_DETAILS_CACHE, key = "#eventId")
    @Transactional
    public EventDetailResponse updateEvent(String userEmail, Long eventId, UpsertEventRequest request) {
        Event event = getOwnedEvent(userEmail, eventId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled events cannot be modified");
        }
        validateEventDates(request);

        applyEventFields(event, request);
        log.info("Updated event id={} status={}", event.getId(), event.getStatus());
        return EventMapper.toDetail(event);
    }

    @Audited(action = "DELETE_DRAFT_EVENT")
    @CacheEvict(cacheNames = CacheConfig.EVENT_DETAILS_CACHE, key = "#eventId")
    @Transactional
    public void deleteDraftEvent(String userEmail, Long eventId) {
        Event event = getOwnedEvent(userEmail, eventId);

        // BR8: Only DRAFT events can be deleted; published events must be cancelled
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT events can be deleted; published events must be cancelled");
        }

        eventRepository.delete(event);
        log.info("Deleted draft event id={}", eventId);
    }

    @Audited(action = "PUBLISH_EVENT")
    @CacheEvict(cacheNames = CacheConfig.EVENT_DETAILS_CACHE, key = "#eventId")
    @Transactional
    public EventDetailResponse publishEvent(String userEmail, Long eventId) {
        Event event = getOwnedEvent(userEmail, eventId);

        // BR5: Organizer must be APPROVED to publish events
        if (event.getOrganizer().getStatus() != OrganizerStatus.APPROVED) {
            throw new BusinessRuleException("Organizer must be APPROVED before publishing events");
        }
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT events can be published");
        }
        if (event.getTicketTiers().isEmpty()) {
            throw new BusinessRuleException("Event must have at least one ticket tier before publishing");
        }

        event.setStatus(EventStatus.PUBLISHED);
        log.info("Published event id={} organizerId={}", event.getId(), event.getOrganizer().getId());
        return EventMapper.toDetail(event);
    }

    @Audited(action = "CANCEL_EVENT")
@CacheEvict(cacheNames = CacheConfig.EVENT_DETAILS_CACHE, key = "#eventId")
@Transactional
public EventDetailResponse cancelEvent(String userEmail, Long eventId) {
    Event event = getOwnedEvent(userEmail, eventId);

    if (event.getStatus() == EventStatus.CANCELLED) {
        throw new BusinessRuleException("Event with id " + eventId + " is already CANCELLED");
    }

    event.setStatus(EventStatus.CANCELLED);
    Long organizerId = event.getOrganizer().getId();
    EventDetailResponse response = EventMapper.toDetail(event);

    int cancelledBookings = bookingRepository.cancelConfirmedBookingsByEventId(eventId, Instant.now());
    log.info("Cancelled event id={} organizerId={} cancelledBookings={}",
            eventId, organizerId, cancelledBookings);
    return response;
}

    @Audited(action = "ADD_TICKET_TIER")
    @CacheEvict(cacheNames = CacheConfig.EVENT_DETAILS_CACHE, key = "#eventId")
    @Transactional
    public TicketTierResponse addTier(String userEmail, Long eventId, UpsertTicketTierRequest request) {
        Event event = getOwnedEvent(userEmail, eventId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot add ticket tiers to a CANCELLED event");
        }

        TicketTier tier = new TicketTier();
        tier.setEvent(event);
        tier.setName(request.name().trim());
        tier.setPrice(request.price());
        tier.setCapacity(request.capacity());

        TicketTier saved = ticketTierRepository.save(tier);
        event.getTicketTiers().add(saved);

        log.info("Added ticket tier id={} eventId={} name={}", saved.getId(), eventId, saved.getName());
        return EventMapper.toTierResponse(saved);
    }

    @Audited(action = "UPDATE_TICKET_TIER")
    @CacheEvict(cacheNames = CacheConfig.EVENT_DETAILS_CACHE, key = "#eventId")
    @Transactional
    public TicketTierResponse updateTier(
            String userEmail,
            Long eventId,
            Long tierId,
            UpsertTicketTierRequest request
    ) {
        Event event = getOwnedEvent(userEmail, eventId);
        TicketTier tier = findOwnedTier(event, tierId);

        if (request.capacity() < tier.getSoldCount()) {
            throw new BusinessRuleException(
                    "Tier capacity (" + request.capacity() + ") cannot be less than already sold count (" + tier.getSoldCount() + ")"
            );
        }

        tier.setName(request.name().trim());
        tier.setPrice(request.price());
        tier.setCapacity(request.capacity());

        log.info("Updated ticket tier id={} eventId={}", tierId, eventId);
        return EventMapper.toTierResponse(tier);
    }

    @Audited(action = "DELETE_TICKET_TIER")
    @CacheEvict(cacheNames = CacheConfig.EVENT_DETAILS_CACHE, key = "#eventId")
    @Transactional
    public void deleteTier(String userEmail, Long eventId, Long tierId) {
        Event event = getOwnedEvent(userEmail, eventId);
        TicketTier tier = findOwnedTier(event, tierId);

        if (tier.getSoldCount() > 0) {
            throw new BusinessRuleException("Cannot remove a ticket tier that already has sold tickets");
        }

        // orphanRemoval = true on Event.ticketTiers deletes the row on commit
        event.getTicketTiers().remove(tier);
        log.info("Removed ticket tier id={} eventId={}", tierId, eventId);
    }

    private Event getOwnedEvent(String userEmail, Long eventId) {
        Organizer organizer = organizerService.getOrganizerForUser(userEmail);
        return eventRepository.findByIdAndOrganizerId(eventId, organizer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
    }

    private TicketTier findOwnedTier(Event event, Long tierId) {
        return event.getTicketTiers().stream()
                .filter(t -> t.getId().equals(tierId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("TicketTier", tierId));
    }

    private void validateEventDates(UpsertEventRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BusinessRuleException("Event end time must be after start time");
        }
    }

    private void applyEventFields(Event event, UpsertEventRequest request) {
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setVenueName(request.venueName().trim());
        event.setCity(request.city().trim());
        event.setAddress(request.address());
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
    }
}