package com.eventpass.support;

import com.eventpass.common.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Tag(name = "Admin - Support Tickets", description = "Admin endpoints for listing and closing support tickets")
@RestController 
@RequestMapping("/api/v1/admin/support-tickets")
@RequiredArgsConstructor 
public class AdminSupportTicketController {

     private final SupportTicketService supportTicketService;

     @GetMapping 
     public ResponseEntity<PageResponse<SupportTicketResponse>> listTickets(@RequestParam(required = false)
SupportTicketStatus status, @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
     return ResponseEntity.ok(supportTicketService.listTickets(status, pageable));
}

@PatchMapping("/{ticketId}")
public ResponseEntity<SupportTicketResponse> updateTicketStatus(@PathVariable Long ticketId, @Valid @RequestBody UpdateSupportTicketStatusRequest request) {
     return ResponseEntity.ok(supportTicketService.updateTicketStatus(ticketId, request));
}

}
