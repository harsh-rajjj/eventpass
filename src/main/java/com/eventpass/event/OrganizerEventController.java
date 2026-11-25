package com.eventpass.event;

import com.eventpass.common.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Organizer - Events", description = "Endpoints for organizers to manage their own events and ticket tiers")
@RestController
@RequestMapping("/api/v1/organizer/events")
@RequiredArgsConstructor
public class OrganizerEventController {

    private final OrganizerEventService organizerEventService;

    @GetMapping
    public ResponseEntity<PageResponse<EventSummary>> listMyEvents(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(organizerEventService.listMyEvents(authentication.getName(), pageable));
    }

    @PostMapping
    public ResponseEntity<EventDetailResponse> createDraftEvent(
            Authentication authentication,
            @Valid @RequestBody UpsertEventRequest request
    ) {
        EventDetailResponse created = organizerEventService.createDraftEvent(authentication.getName(), request);
        URI location = URI.create("/api/v1/events/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> updateEvent(
            Authentication authentication,
            @PathVariable Long eventId,
            @Valid @RequestBody UpsertEventRequest request
    ) {
        return ResponseEntity.ok(
                organizerEventService.updateEvent(authentication.getName(), eventId, request)
        );
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteDraftEvent(
            Authentication authentication,
            @PathVariable Long eventId
    ) {
        organizerEventService.deleteDraftEvent(authentication.getName(), eventId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/publish")
    public ResponseEntity<EventDetailResponse> publishEvent(
            Authentication authentication,
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(organizerEventService.publishEvent(authentication.getName(), eventId));
    }

    @PostMapping("/{eventId}/cancel")
    public ResponseEntity<EventDetailResponse> cancelEvent(
            Authentication authentication,
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(organizerEventService.cancelEvent(authentication.getName(), eventId));
    }

    @PostMapping("/{eventId}/tiers")
    public ResponseEntity<TicketTierResponse> addTier(
            Authentication authentication,
            @PathVariable Long eventId,
            @Valid @RequestBody UpsertTicketTierRequest request
    ) {
        TicketTierResponse created = organizerEventService.addTier(authentication.getName(), eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{eventId}/tiers/{tierId}")
    public ResponseEntity<TicketTierResponse> updateTier(
            Authentication authentication,
            @PathVariable Long eventId,
            @PathVariable Long tierId,
            @Valid @RequestBody UpsertTicketTierRequest request
    ) {
        return ResponseEntity.ok(
                organizerEventService.updateTier(authentication.getName(), eventId, tierId, request)
        );
    }

    @DeleteMapping("/{eventId}/tiers/{tierId}")
    public ResponseEntity<Void> deleteTier(
            Authentication authentication,
            @PathVariable Long eventId,
            @PathVariable Long tierId
    ) {
        organizerEventService.deleteTier(authentication.getName(), eventId, tierId);
        return ResponseEntity.noContent().build();
    }
}