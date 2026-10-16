package com.eventpass.event;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor 
public class EventController {

     private final EventService eventService;

     @GetMapping
     public ResponseEntity<List<EventSummary>> getPublishedEvents() {
          List<EventSummary> events = eventService.findPublishedEvents();
          return ResponseEntity.ok(events);
     }

     @GetMapping("/{id}")
     public ResponseEntity<EventDetailResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.findEventById(id));
     }
     

}
