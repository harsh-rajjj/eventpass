package com.eventpass.event;

import com.eventpass.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Slf4j 
@Service  
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository eventRepository;

    /**
     * Searches published events using optional 
     * filters in a single JOIN FETCH
     * query (no N+1).
     */
    public List<EventSummary> searchPublishedEvents(
        String city,
        EventCategory category,
        Instant from,
        Instant to,
        String q
    ){
        String normalizedCity = normalizeFilter(city);
        String normalizedQuery = normalizeFilter(q);

        log.debug("Searching published events city={} category={} from={} to={} q={}",
            normalizedCity, category, from, to, normalizedQuery
        );
    
            return eventRepository
                                .searchPublishedEvents(normalizedCity, category, from, to, normalizedQuery)
                                .stream()
                                .map(EventMapper::toSummary)
                                .toList();
    }

    /*
    Convenience overload when no filters are supplied
    */
    public List<EventSummary> findPublishedEvents() {
     return searchPublishedEvents(null, null, null, null, null);
    }

    /**
     * Loads a single event along with its Organizer and TicketTiers in ONE query via @EntityGraph
     */
    public EventDetailResponse findEventById(Long id) {
     Event event = eventRepository
                              .findWithDetailsById(id)
                              .orElseThrow(() -> new ResourceNotFoundException("Event", id));

          return EventMapper.toDetail(event);
    }

    private String normalizeFilter(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
