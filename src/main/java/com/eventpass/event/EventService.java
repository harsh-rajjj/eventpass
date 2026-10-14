package com.eventpass.event;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service  
@RequiredArgsConstructor      
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
                              .orElseThrow(() -> new RuntimeException("Event not found"));

          return EventMapper.toDetail(event);
    }
}
