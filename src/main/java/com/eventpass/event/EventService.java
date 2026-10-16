package com.eventpass.event;

import java.util.List;
import com.eventpass.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service  
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository eventRepository;

    public List<EventSummary> findPublishedEvents() {
     return eventRepository
                         .findAll()
                         .stream()
                         .filter(e -> e.getStatus() == EventStatus.PUBLISHED)
                         .map(EventMapper::toSummary)
                         .toList();

    }

    public EventDetailResponse findEventById(Long id) {
     Event event = eventRepository
                              .findById(id)
                              .orElseThrow(() -> new ResourceNotFoundException("Event", id));

          return EventMapper.toDetail(event);
    }
}
