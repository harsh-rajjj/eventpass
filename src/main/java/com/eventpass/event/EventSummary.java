package com.eventpass.event;

import java.time.Instant;

public record EventSummary(
     Long id, 
     String title,
     EventCategory category,
     String city,
     Instant startsAt,
     String organizerName
) {

}
