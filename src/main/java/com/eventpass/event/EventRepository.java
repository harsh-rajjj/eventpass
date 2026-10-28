package com.eventpass.event;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 1. JPQL + JOIN FETCH with optional search filters.
     *    - JOIN FETCH e.organizer loads Event + Organizer in ONE query (eliminates N+1).
     *    - (:param IS NULL OR ...) makes every filter optional.
     */
    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.organizer o
            WHERE e.status = com.eventpass.event.EventStatus.PUBLISHED
              AND (:city IS NULL OR LOWER(e.city) = LOWER(:city))
              AND (:category IS NULL OR e.category = :category)
              AND (:fromDate IS NULL OR e.startsAt >= :fromDate)
              AND (:toDate IS NULL OR e.startsAt <= :toDate)
              AND (:q IS NULL
                   OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY e.startsAt ASC
            """)
    List<Event> searchPublishedEvents(
            @Param("city") String city,
            @Param("category") EventCategory category,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("q") String q
    );

    /**
     * 2. @EntityGraph: Eagerly fetches Organizer and TicketTiers in a single SQL LEFT JOIN
     *    for the event detail endpoint (GET /api/v1/events/{id}).
     */
    @EntityGraph(attributePaths = {"organizer", "ticketTiers"})
    Optional<Event> findWithDetailsById(Long id);

    /**
     * 3. Bound to @NamedQuery(name = "Event.findByOrganizerIdOrderByStartsAtAsc") on Event entity.
     */
    List<Event> findByOrganizerIdOrderByStartsAtAsc(@Param("organizerId") Long organizerId);

    /**
     * 4. Native SQL query: Counts published events for a specific organizer directly on the DB table.
     */
    @Query(
            value = "SELECT COUNT(*) FROM events WHERE organizer_id = :organizerId AND status = 'PUBLISHED'",
            nativeQuery = true
    )
    long countPublishedEventsByOrganizerId(@Param("organizerId") Long organizerId);
}