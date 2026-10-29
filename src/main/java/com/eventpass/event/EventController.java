package com.eventpass.event;

import com.eventpass.common.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;

@Tag(name = "Events", description = "Public catalog endpoints for browsing published events")
@RestController 
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor 
public class EventController {

     private final EventService eventService;

     @GetMapping
     public ResponseEntity<PageResponse<EventSummary>> getPublishedEvents(
          @RequestParam(required = false)
          String city, 
          @RequestParam(required = false)
          EventCategory category,
          @RequestParam(required = false)
          Instant from,
          @RequestParam(required = false)
          Instant to,
          @RequestParam(required = false)
          String q,
          @PageableDefault(size = 10, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable)
      {

          PageResponse<EventSummary> page = eventService.searchPublishedEvents(city, category, from, to, q, pageable);
          return ResponseEntity.ok(page);
     }

     @GetMapping("/{id}")
     public ResponseEntity<EventDetailResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.findEventById(id));
     }
     

}
