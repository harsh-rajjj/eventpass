package com.eventpass.common;

//import java.beans.BeanProperty;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration 
public class OpenApiConfig {
     @Bean
     public OpenAPI eventPassOpenAPI() {
          return new OpenAPI()
                              .info(new Info()
                                             .title("EventPass API")
                                             .version("v1")
                                             .description("Event ticketing REST API - browse events, book tickets, and manage organizers.")
                                             .contact(new Contact().name("EventPass Team")));
     }
}
