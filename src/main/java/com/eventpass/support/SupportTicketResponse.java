package com.eventpass.support;
import java.time.Instant;


public record SupportTicketResponse(
     Long id, 
     String name,
     String email,
     String subject,
     String message,
     SupportTicketStatus status,
     Instant createdAt,
     Instant closedAt
     
) {
     public SupportTicketResponse(
          Long id,
          String name,
          String email,
          String subject,
          String message,
          SupportTicketStatus status,
          Instant createdAt
     ) {
          this(id, name, email, subject, message, status, createdAt, null);
     }

     public static SupportTicketResponse from(SupportTicket ticket) {
          return new SupportTicketResponse(  
               ticket.getId(),
               ticket.getName(),
               ticket.getEmail(),
               ticket.getSubject(),
               ticket.getMessage(),
               ticket.getStatus(),
               ticket.getCreatedAt(),
               ticket.getClosedAt()
          );
     }

}
