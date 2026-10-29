package com.eventpass.support;

import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.PageResponse;
import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.logging.Audited;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

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

     public PageResponse<SupportTicketResponse> listTickets(SupportTicketStatus status, Pageable pageable) {
          Page<SupportTicket> page = supportTicketRepository.findAllByStatusOptional(status, pageable);
          return PageResponse.from(page, SupportTicketResponse::from);
     }

     @Audited(action = "CLOSE_SUPPORT_TICKET")
     @Transactional 
     public SupportTicketResponse updateTicketStatus(Long ticketId, UpdateSupportTicketStatusRequest request) {
          SupportTicket ticket = supportTicketRepository.findById(ticketId).orElseThrow(
                    () -> new ResourceNotFoundException("SupportTicket", ticketId)
          );

          //Support ticket flow only allows NEW -> CLOSED
          if(request.status() != SupportTicketStatus.CLOSED) {
               throw new BusinessRuleException("Support tickets can only be transitioned to CLOSED status");
          }

          if(ticket.getStatus() == SupportTicketStatus.CLOSED) {
               throw new BusinessRuleException("Support ticket with id " + ticketId + " is already CLOSED");
          }

          ticket.setStatus(SupportTicketStatus.CLOSED);
          ticket.setClosedAt(Instant.now());

          SupportTicket saved = supportTicketRepository.save(ticket);
          log.info("Closed support ticket id={} closedAt={}", saved.getId(), saved.getClosedAt());

          return SupportTicketResponse.from(saved);
     }
}
