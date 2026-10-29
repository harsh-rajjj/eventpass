package com.eventpass.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

     @Query("""
               SELECT t FROM SupportTicket t WHERE (:status IS NULL OR t.status = :status)
               """)

               Page<SupportTicket> findAllByStatusOptional(@Param("status") SupportTicketStatus status, Pageable Pageable);
}
