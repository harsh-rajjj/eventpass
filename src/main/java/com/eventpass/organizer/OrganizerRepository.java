package com.eventpass.organizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizerRepository extends JpaRepository<Organizer, Long> {

     @Query("""
               SELECT o FROM Organizer o WHERE (:status IS NULL OR o.status = :statu)
               """)
     Page<Organizer> findAllByStatusOptional(@Param("status") OrganizerStatus status, Pageable pageable);
}
