package com.eventpass.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Authentication", description = "Public endpoints for registration and JWT login")
@RestController 
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor 
public class AuthController {

     private final AuthService authService;

     @PostMapping("/register")
     public ResponseEntity<UserResponse> registerAttendee(@Valid @RequestBody RegisterAttendeeRequest request){

          UserResponse response = authService.registerAttendee(request);
          return ResponseEntity.status(HttpStatus.CREATED).body(response);
     }

     @PostMapping("/register/organizer")
     public ResponseEntity<UserResponse> registerOrganizer(@Valid @RequestBody RegisterOrganizerRequest request){
          UserResponse response = authService.registerOrganizer(request);
          return ResponseEntity.status(HttpStatus.CREATED).body(response);
     }

     @PostMapping("/login")
     public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
          AuthResponse response = authService.login(request);
          return ResponseEntity.ok(response);
     }
}
