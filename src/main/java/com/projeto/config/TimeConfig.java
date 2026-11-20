package com.projeto.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class TimeConfig {

    @Bean
    @Primary
    public Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    public Clock cotacaoAcquisitionClock(@Qualifier("applicationClock") Clock baseClock) {
        // Acquisition resolution is explicit at the source; provider values remain exact.
        return Clock.tick(baseClock, Duration.ofNanos(1_000));
    }
}
