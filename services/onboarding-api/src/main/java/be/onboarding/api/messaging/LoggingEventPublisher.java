package be.onboarding.api.messaging;

import be.onboarding.api.application.ApplicationSubmitted;
import be.onboarding.api.application.OnboardingEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Fallback when Service Bus is disabled (local runs, CI, tests): only logs the event. */
@Component
@ConditionalOnProperty(name = "spring.cloud.azure.servicebus.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEventPublisher implements OnboardingEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publish(ApplicationSubmitted event) {
        // Only the id: names and e-mail addresses are personal data and stay out of the logs.
        logger.info("Service Bus disabled, not publishing ApplicationSubmitted for application {}",
                event.applicationId());
    }
}
