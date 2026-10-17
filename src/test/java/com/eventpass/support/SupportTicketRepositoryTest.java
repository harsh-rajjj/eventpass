package com.eventpass.support;

import com.eventpass.TestContainersConfiguration;
import com.eventpass.common.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

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
}
