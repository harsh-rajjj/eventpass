package com.eventpass.event;

import com.eventpass.booking.BookingRepository;
import com.eventpass.common.CacheConfig;
import com.eventpass.organizer.AdminOrganizerService;
import com.eventpass.organizer.Organizer;
import com.eventpass.organizer.OrganizerRepository;
import com.eventpass.organizer.OrganizerService;
import com.eventpass.organizer.OrganizerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig({
        CacheConfig.class,
        EventService.class,
        OrganizerEventService.class,
        AdminOrganizerService.class
})
class EventCachingTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private OrganizerEventService organizerEventService;

    @Autowired
    private AdminOrganizerService adminOrganizerService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private TicketTierRepository ticketTierRepository;

    @MockitoBean
    private OrganizerRepository organizerRepository;

    @MockitoBean
    private OrganizerService organizerService;

    @MockitoBean
    private BookingRepository bookingRepository;

    @BeforeEach
    void clearCache() {
        Objects.requireNonNull(cacheManager.getCache(CacheConfig.EVENT_DETAILS_CACHE)).clear();
    }

    @Test
    void findEventById_cachesResultOnFirstCall_andSkipsRepositoryOnSecondCall() {
        Organizer org = sampleOrganizer(10L, "Neon Festivals", OrganizerStatus.APPROVED);
        Event event = sampleEvent(1L, "Neon Sunburn 2027", org);
        when(eventRepository.findWithDetailsById(1L)).thenReturn(Optional.of(event));

        // 1st call -> cache miss -> hits repository
        EventDetailResponse first = eventService.findEventById(1L);
        // 2nd call -> cache hit -> served from Caffeine RAM
        EventDetailResponse second = eventService.findEventById(1L);

        assertThat(first.title()).isEqualTo("Neon Sunburn 2027");
        assertThat(second).isEqualTo(first);
        verify(eventRepository, times(1)).findWithDetailsById(1L);
    }

    @Test
    void updateEvent_evictsOnlyTargetEventKey_leavingOtherCachedEventsIntact() {
        Organizer org = sampleOrganizer(10L, "Neon Festivals", OrganizerStatus.APPROVED);
        Event event1 = sampleEvent(1L, "Event One", org);
        Event event2 = sampleEvent(2L, "Event Two", org);

        when(eventRepository.findWithDetailsById(1L)).thenReturn(Optional.of(event1));
        when(eventRepository.findWithDetailsById(2L)).thenReturn(Optional.of(event2));
        when(organizerService.getOrganizerForUser("org@neon.com")).thenReturn(org);
        when(eventRepository.findByIdAndOrganizerId(1L, 10L)).thenReturn(Optional.of(event1));

        // Warm both cache keys (1L and 2L)
        eventService.findEventById(1L);
        eventService.findEventById(2L);

        // Update ONLY event 1L -> @CacheEvict(key = "#eventId")
        var request = new UpsertEventRequest(
                "Event One Updated",
                "Updated description",
                EventCategory.MUSIC,
                "Arena",
                "Mumbai",
                "123 Main St",
                Instant.parse("2027-08-01T10:00:00Z"),
                Instant.parse("2027-08-01T18:00:00Z")
        );
        organizerEventService.updateEvent("org@neon.com", 1L, request);

        // Read both again: 1L must reload from repo (2 total calls); 2L stays cached (1 total call)
        EventDetailResponse reloaded1 = eventService.findEventById(1L);
        eventService.findEventById(2L);

        assertThat(reloaded1.title()).isEqualTo("Event One Updated");
        verify(eventRepository, times(2)).findWithDetailsById(1L);
        verify(eventRepository, times(1)).findWithDetailsById(2L);
    }

    @Test
    void suspendOrganizer_evictsAllEntriesFromEventDetailsCache() {
        Organizer org = sampleOrganizer(10L, "Neon Festivals", OrganizerStatus.APPROVED);
        when(eventRepository.findWithDetailsById(1L)).thenReturn(Optional.of(sampleEvent(1L, "Event 1", org)));
        when(eventRepository.findWithDetailsById(2L)).thenReturn(Optional.of(sampleEvent(2L, "Event 2", org)));
        when(organizerRepository.findById(10L)).thenReturn(Optional.of(org));
        when(eventRepository.updatePublishedEventsStatusByOrganizerId(10L, EventStatus.DRAFT)).thenReturn(2);

        // Populate cache with two events
        eventService.findEventById(1L);
        eventService.findEventById(2L);

        Cache cache = Objects.requireNonNull(cacheManager.getCache(CacheConfig.EVENT_DETAILS_CACHE));
        assertThat(cache.get(1L)).isNotNull();
        assertThat(cache.get(2L)).isNotNull();

        // Suspend organizer -> @CacheEvict(allEntries = true)
        adminOrganizerService.suspend(10L);

        assertThat(cache.get(1L)).isNull();
        assertThat(cache.get(2L)).isNull();
    }

    private Organizer sampleOrganizer(Long id, String name, OrganizerStatus status) {
        Organizer organizer = new Organizer();
        organizer.setId(id);
        organizer.setName(name);
        organizer.setContactEmail("contact@neon.com");
        organizer.setStatus(status);
        return organizer;
    }

    private Event sampleEvent(Long id, String title, Organizer organizer) {
        Event event = new Event();
        event.setId(id);
        event.setOrganizer(organizer);
        event.setTitle(title);
        event.setCategory(EventCategory.MUSIC);
        event.setVenueName("Main Arena");
        event.setCity("Mumbai");
        event.setStartsAt(Instant.parse("2027-08-01T10:00:00Z"));
        event.setEndsAt(Instant.parse("2027-08-01T18:00:00Z"));
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }
}
