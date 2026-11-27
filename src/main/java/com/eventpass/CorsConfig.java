package com.eventpass;

import com.eventpass.common.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

     private final CorsProperties corsProperties;

     @Override
     public void addCorsMappings(CorsRegistry registry) {
          registry
               .addMapping("/api/**")
               .allowedOrigins(corsProperties.allowedOrigins().toArray(String[] :: new))
               .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
               .allowedHeaders("*");
     }
     
}
