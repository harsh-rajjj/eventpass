package com.eventpass.common;

import com.eventpass.booking.BookingProperties;
import com.eventpass.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesTestConfig.class);

    @Test
    void propertiesBindCleanly_fromEnvironmentStyleProdValues() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "app.jwt.secret=prod-secret-key-at-least-32-characters-long-1234",
                        "app.jwt.expiration=PT2H",
                        "app.booking.max-tickets-per-event=4",
                        "app.booking.cancel-cutoff-hours=48",
                        "app.cors.allowed-origins=https://eventpass.example.com"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtProperties.class).expiration()).isEqualTo(Duration.ofHours(2));
                    assertThat(context.getBean(BookingProperties.class).maxTicketsPerEvent()).isEqualTo(4);
                    assertThat(context.getBean(BookingProperties.class).cancelCutoffHours()).isEqualTo(48);
                    assertThat(context.getBean(CorsProperties.class).allowedOrigins())
                            .containsExactly("https://eventpass.example.com");
                    assertThat(context).doesNotHaveBean(DevSeedAccountInitializer.class);
                });
    }

    @Test
    void startupFailsFast_whenJwtSecretIsShorterThan32Characters() {
        contextRunner
                .withPropertyValues(
                        "app.jwt.secret=too-short",
                        "app.jwt.expiration=PT1H",
                        "app.booking.max-tickets-per-event=6",
                        "app.booking.cancel-cutoff-hours=24",
                        "app.cors.allowed-origins=https://eventpass.example.com"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties({JwtProperties.class, BookingProperties.class, CorsProperties.class})
    static class PropertiesTestConfig {
    }
}