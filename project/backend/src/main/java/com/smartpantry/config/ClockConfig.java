package com.smartpantry.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The single Clock bean. The expiry classifier and the item validator both use
 * it for "today" (A-005, DR-022), so tests can fix the date in one place.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
