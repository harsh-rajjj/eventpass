package com.eventpass.user;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;   

import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {

     Optional<User> findByEmail(String email);


     @Lock(LockModeType.PESSIMISTIC_WRITE)
     @Query("SELECT u FROM User u WHERE u.email = :email")
     Optional<User> findByEmailForUpdate(@Param("email") String email);

     
     @EntityGraph(attributePaths = "organizer")
     Optional<User> findWithOrganizerByEmail(String email);

     boolean existsByEmail(String email);

}
