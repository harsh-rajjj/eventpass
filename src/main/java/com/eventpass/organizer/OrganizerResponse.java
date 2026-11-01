package com.eventpass.organizer;

public record OrganizerResponse(
     Long id,
     String name,
     String description,
     String website,
     String contactEmail,
     OrganizerStatus status
) {
     public static OrganizerResponse from(Organizer organizer) {
          return new OrganizerResponse(
               organizer.getId(),
               organizer.getName(),
               organizer.getDescription(),
               organizer.getWebsite(),
               organizer.getContactEmail(),
               organizer.getStatus()
          );
    }

}
