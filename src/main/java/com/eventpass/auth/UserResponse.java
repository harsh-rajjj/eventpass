package com.eventpass.auth;

import java.time.Instant;

import com.eventpass.user.Role;
import com.eventpass.user.User;

public record UserResponse(
     Long id,
     String fullName,
     String email,
     Role role,
     Long organizerId,
     Instant createdAt
) {
     public static UserResponse from(User user){
          return new UserResponse(
               user.getId(),
               user.getFullName(),
               user.getEmail(),
               user.getRole(),
               user.getOrganizer() != null ? user.getOrganizer().getId() : null,
               user.getCreatedAt()
          );
     }

}
