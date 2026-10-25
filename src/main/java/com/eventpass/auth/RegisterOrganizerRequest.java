package com.eventpass.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterOrganizerRequest(

     @NotBlank(message = "Full name is required")
     @Size(max = 100, message = "Full name must be at most 100 characters")
     String fullName,

     @NotBlank(message = "Email is required")
     @Email(message = "Email must be a valid email address")
     @Size(max = 255, message = "Email must be at most 255 characters")
     String email, 

     @NotBlank(message = "Password is required")
     @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
     String password,

     @NotBlank(message = "Organizer name is required")
     @Size(max = 150, message = "Organizer name must be at most 150 characters")
     String organizerName,
     
     @Size(max = 2000, message = "Description must be at most 2000 characters")
     String description,

     @Size(max = 255, message = "Website must be at most 255 characters")
     String website,


     @NotBlank(message = "Contact email is required")
     @Email(message = "Contact email must be a valid email address")
     @Size(max = 255, message = "Contact email must be at most 255 characters")
     String contactEmail
) {

}
