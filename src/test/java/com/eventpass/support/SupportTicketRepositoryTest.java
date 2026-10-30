package com.eventpass.support;

import com.eventpass.TestContainersConfiguration;
import com.eventpass.common.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestContainersConfiguration.class, JpaAuditingConfig.class})
class SupportTicketRepositoryTest {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Test
    void save_populatesAuditFieldsAndDefaultStatus() {
        SupportTicket ticket = new SupportTicket();
        ticket.setName("Aarav Sharma");
        ticket.setEmail("aarav@example.com");
        ticket.setSubject("Refund request");
        ticket.setMessage("Can I cancel my VIP ticket 48 hours before the concert?");

        SupportTicket saved = supportTicketRepository.save(ticket);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(SupportTicketStatus.NEW);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("system");
        assertThat(saved.getUpdatedBy()).isEqualTo("system");
    }

    @Test
    void findAllByStatusOptional_filtersByStatusAndPagesResults() {
        SupportTicket newTicket = new SupportTicket();
        newTicket.setName("Priya Patel");
        newTicket.setEmail("priya@example.com");
        newTicket.setSubject("Ticket transfer");
        newTicket.setMessage("Can I transfer my ticket to a friend?");
        supportTicketRepository.save(newTicket);

        SupportTicket closedTicket = new SupportTicket();
        closedTicket.setName("Rohan Verma");
        closedTicket.setEmail("rohan@example.com");
        closedTicket.setSubject("Venue parking");
        closedTicket.setMessage("Is parking available at the venue?");
        closedTicket.setStatus(SupportTicketStatus.CLOSED);
        closedTicket.setClosedAt(Instant.now());
        supportTicketRepository.save(closedTicket);

        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<SupportTicket> newPage = supportTicketRepository.findAllByStatusOptional(
                SupportTicketStatus.NEW, pageable
        );
        Page<SupportTicket> closedPage = supportTicketRepository.findAllByStatusOptional(
                SupportTicketStatus.CLOSED, pageable
        );

        assertThat(newPage.getContent())
                .allMatch(t -> t.getStatus() == SupportTicketStatus.NEW);
        assertThat(closedPage.getContent())
                .allMatch(t -> t.getStatus() == SupportTicketStatus.CLOSED);
    }
}
