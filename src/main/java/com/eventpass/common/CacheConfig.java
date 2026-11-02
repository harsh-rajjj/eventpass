package com.eventpass.common;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;


@Configuration 
@EnableCaching 
public class CacheConfig {
     
     public static final String EVENT_DETAILS_CACHE = "eventDetails";

     @Bean
     public CacheManager cacheManager() {
          CaffeineCacheManager cacheManager = new CaffeineCacheManager(EVENT_DETAILS_CACHE);
          cacheManager.setCaffeine(
               Caffeine.newBuilder()
                       .maximumSize(500)
                       .expireAfterWrite(Duration.ofMinutes(5)).recordStats()
          );

          return cacheManager;
     }
}
