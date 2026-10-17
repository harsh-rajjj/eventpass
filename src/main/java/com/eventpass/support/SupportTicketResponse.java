package com.eventpass.support;
import java.time.Instant;


public record SupportTicketResponse(
     Long id, 
     String name,
     String email,
     String subject,
     String message,
     SupportTicketStatus status,
     Instant createdAt
     
) {
     public static SupportTicketResponse from(SupportTicket ticket) {
          return new SupportTicketResponse(  
               ticket.getId(),
               ticket.getName(),
               ticket.getEmail(),
               ticket.getSubject(),
               ticket.getMessage(),
               ticket.getStatus(),
               ticket.getCreatedAt()
          );
     }

}
