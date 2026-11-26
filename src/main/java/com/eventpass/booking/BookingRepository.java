package com.eventpass.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByBookingRef(String bookingRef);

    @EntityGraph(attributePaths = {"event", "ticketTier"})
    Page<Booking> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "ticketTier", "user"})
    Optional<Booking> findByBookingRefAndUserId(String bookingRef, Long userId);

    @EntityGraph(attributePaths = {"event", "event.organizer", "ticketTier", "user"})
    Optional<Booking> findWithDetailsByBookingRef(String bookingRef);

    @EntityGraph(attributePaths = {"user", "ticketTier"})
    Page<Booking> findByEventId(Long eventId, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(b.quantity), 0)
            FROM Booking b
            WHERE b.user.id = :userId
              AND b.event.id = :eventId
              AND b.status <> com.eventpass.booking.BookingStatus.CANCELLED
            """)
    int sumActiveTicketsForUserAndEvent(
            @Param("userId") Long userId,
            @Param("eventId") Long eventId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Booking b
            SET b.status = com.eventpass.booking.BookingStatus.CANCELLED,
                b.cancelledAt = :cancelledAt
            WHERE b.event.id = :eventId
              AND b.status = com.eventpass.booking.BookingStatus.CONFIRMED
            """)
    int cancelConfirmedBookingsByEventId(
            @Param("eventId") Long eventId,
            @Param("cancelledAt") Instant cancelledAt
    );
}