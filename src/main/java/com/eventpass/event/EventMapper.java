package com.eventpass.event;

import java.util.List;

public final class EventMapper {
     private EventMapper() {} //utility class

     public static EventSummary toSummary(Event event) {
          return new EventSummary(
               event.getId(),
               event.getTitle(),
               event.getCategory(),
               event.getCity(),
               event.getStartsAt(),
               event.getOrganizer().getName()
          );
     }

     public static EventDetailResponse toDetail(Event event) {
          List<TicketTierResponse> tiers = event.getTicketTiers()
               .stream()
               .map(EventMapper::toTierResponse)
               .toList();

          return new EventDetailResponse(
               event.getId(),
               event.getTitle(),
               event.getDescription(),
               event.getCategory(),
               event.getVenueName(),
               event.getCity(),
               event.getAddress(),
               event.getStartsAt(),
               event.getEndsAt(),
               event.getStatus(),
               event.getBannerImagePath(),
               event.getOrganizer().getName(),
               tiers
          );
     }

     public static TicketTierResponse toTierResponse(TicketTier tier) {
          return new TicketTierResponse(
               tier.getId(),
               tier.getName(),
               tier.getPrice(),
               tier.getCapacity() - tier.getSoldCount() 
          );
     }
}
