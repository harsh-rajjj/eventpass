package com.eventpass;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan	
public class EventpassApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventpassApplication.class, args);
	}

}
