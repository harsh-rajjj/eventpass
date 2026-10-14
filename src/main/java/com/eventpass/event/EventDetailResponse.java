package com.eventpass.event;

import java.time.Instant;
import java.util.List;

public record EventDetailResponse(
     Long id,
     String title,
     String description,
     EventCategory category,
     String venueName,
     String city,
     String address,
     Instant startsAt,
     Instant endsAt,
     EventStatus status,
     String bannerImagePath,
     String organizerName,
     List<TicketTierResponse> ticketTiers
) {

}
