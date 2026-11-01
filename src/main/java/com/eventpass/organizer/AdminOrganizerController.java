
package com.eventpass.organizer;

import com.eventpass.common.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Organizers", description = "Admin endpoints for approving and suspending organizers")
@RestController 
@RequestMapping("/api/v1/admin/organizers")
@RequiredArgsConstructor 
public class AdminOrganizerController {

     private final AdminOrganizerService adminOrganizerService;

     /**
      * Oldest first (id ASC) so admins word through the PENDING queue in arrival order.
      */

     @GetMapping 
     public ResponseEntity<PageResponse<OrganizerResponse>> listOrganizers(@RequestParam(required = false) OrganizerStatus status, @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
          return ResponseEntity.ok(adminOrganizerService.listOrganizers(status, pageable));
     }

     @PostMapping("/{organizerId}/approve")
     public ResponseEntity<OrganizerResponse> approve(@PathVariable Long organizerId) {
          return ResponseEntity.ok(adminOrganizerService.approve(organizerId));
     }

     @PostMapping("/{organizerId}/suspend")
     public ResponseEntity<SuspendOrganizerResponse> suspend(@PathVariable Long organizerId) {
          return ResponseEntity.ok(adminOrganizerService.suspend(organizerId));
     }
}
