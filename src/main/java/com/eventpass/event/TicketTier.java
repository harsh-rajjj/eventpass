package com.eventpass.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "ticket_tiers")
@Getter @Setter @NoArgsConstructor 
public class TicketTier {

     @Id 
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;

     @ManyToOne(fetch = FetchType.LAZY)
     @JoinColumn(name = "event_id", nullable = false)
     private Event event;

     @Column(nullable = false, length = 100)
     private String name;

     @Column(nullable = false, precision = 10, scale = 2)
     private BigDecimal price;

     @Column(nullable = false)
     private int capacity;

     @Column(name = "sold_count", nullable = false)
     private int soldCount = 0;
}
