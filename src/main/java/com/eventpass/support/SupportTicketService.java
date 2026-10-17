package com.eventpass.support;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
@Transactional(readOnly = true)
public class SupportTicketService {
     private final SupportTicketRepository supportTicketRepository;

     @Transactional
     public SupportTicketResponse createTicket(CreateSupportTicketRequest request) {
          SupportTicket ticket = new SupportTicket();
          ticket.setName(request.name());
          ticket.setEmail(request.email());
          ticket.setSubject(request.subject());
          ticket.setMessage(request.message());

          SupportTicket saved = supportTicketRepository.save(ticket);
          return SupportTicketResponse.from(saved);
     }
}
