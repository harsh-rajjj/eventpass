package com.eventpass.support;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.eventpass.logging.Audited;

@Slf4j
@Service 
@RequiredArgsConstructor 
@Transactional(readOnly = true)
public class SupportTicketService {
     private final SupportTicketRepository supportTicketRepository;

     @Audited(action = "CREATE_SUPPORT_TICKET")
     @Transactional
     public SupportTicketResponse createTicket(CreateSupportTicketRequest request) {
          SupportTicket ticket = new SupportTicket();
          ticket.setName(request.name());
          ticket.setEmail(request.email());
          ticket.setSubject(request.subject());
          ticket.setMessage(request.message());

          SupportTicket saved = supportTicketRepository.save(ticket);
          log.info("Created support ticket id={} status={}", saved.getId(), saved.getStatus());
          return SupportTicketResponse.from(saved);
     }
}
