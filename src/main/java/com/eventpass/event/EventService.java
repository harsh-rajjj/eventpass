package com.eventpass.event;

import com.eventpass.common.CacheConfig;
import com.eventpass.common.PageResponse;
import com.eventpass.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

    public PageResponse<EventSummary> searchPublishedEvents(
            String city,
            EventCategory category,
            Instant from,
            Instant to,
            String q,
            Pageable pageable
    ) {
        String normalizedCity = normalizeFilter(city);
        String normalizedQuery = normalizeFilter(q);

        log.debug("Searching published events city={} category={} from={} to={} q={} pageable={}",
                normalizedCity, category, from, to, normalizedQuery, pageable);

        Page<Event> eventPage = eventRepository.searchPublishedEvents(
                normalizedCity, category, from, to, normalizedQuery, pageable);

        return PageResponse.from(eventPage, EventMapper::toSummary);
    }

    @Cacheable(cacheNames = CacheConfig.EVENT_DETAILS_CACHE, key = "#id")
    public EventDetailResponse findEventById(Long id) {
        Event event = eventRepository
                .findWithDetailsByIdAndStatus(id, EventStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        return EventMapper.toDetail(event);
    }

    private String normalizeFilter(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
