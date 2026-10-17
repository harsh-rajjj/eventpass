package com.eventpass.support;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;


import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Support Tickets", description = "Public endpoint for submitting support requests")
@RestController 
@RequestMapping("/api/v1/support-tickets")
@RequiredArgsConstructor
public class SupportTicketController {
     
     private final SupportTicketService supportTicketService;

     @PostMapping
     public ResponseEntity<SupportTicketResponse> createTicket(@Valid @RequestBody CreateSupportTicketRequest request) {
          SupportTicketResponse response = supportTicketService.createTicket(request);
          URI location = URI.create("/api/v1/support-tickets/" + response.id());
          return ResponseEntity.created(location).body(response);

     } 
     
}
