package com.eventpass.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketTierRepository extends JpaRepository<TicketTier, Long> {



     /**
     * Atomic conditional update to prevent overselling under concurrent requests.
     * Returns 1 if seats were reserved, or 0 if capacity would be exceeded.
     */
     @Modifying(clearAutomatically = true, flushAutomatically = true)
          @Query("""
                    UPDATE TicketTier t SET t.soldCount = t.soldCount + :quantity WHERE t.id = :tierId AND t.soldCount + :quantity <= t.capacity
                    """)
     
     int reserveSeats(@Param("tierId") Long tierId, @Param("quantity") int quantity);




     /**
     * Atomically returns seats to inventory when a booking is cancelled.
     */
     @Modifying(clearAutomatically = true, flushAutomatically = true)
     @Query("""
               UPDATE TicketTier t SET t.soldCount = t.soldCount - :quantity WHERE t.id = :tierId AND t.soldCount >= :quantity
               """)
     int releaseSeats(@Param("tierId") Long tierId, @Param("quantity") int quantity);
}
