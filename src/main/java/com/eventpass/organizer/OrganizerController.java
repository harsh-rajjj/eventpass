package com.eventpass.organizer;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Organizer - Profile", description = "Endpoints for an organizer to view and update their own organization profile")
@RestController 
@RequestMapping("/api/v1/organizer/profile")
@RequiredArgsConstructor 
public class OrganizerController {

     private final OrganizerService organizerService;

     @GetMapping 
     public ResponseEntity<OrganizerResponse> getMyProfile(Authentication authentication) {
          return ResponseEntity.ok(organizerService.getMyProfile(authentication.getName()));
     }

     @PutMapping 
     public ResponseEntity<OrganizerResponse> updateMyProfile(Authentication authentication, @Valid @RequestBody UpdateOrganizerProfileRequest request) {
          return ResponseEntity.ok(organizerService.updateMyProfile(authentication.getName(), request));
     }

}
