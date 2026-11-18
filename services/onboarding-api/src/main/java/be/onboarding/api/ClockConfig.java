package be.onboarding.api;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The system clock as a bean, so code that needs "now" can get a fixed clock in tests. */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
