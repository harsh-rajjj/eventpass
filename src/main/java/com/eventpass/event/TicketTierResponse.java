package com.eventpass.event;

import java.math.BigDecimal;

public record TicketTierResponse(
     Long id, 
     String name, 
     BigDecimal price, 
     int available  // Capacity - soldCount
) {

}
