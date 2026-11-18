package be.onboarding.api.messaging;

import be.onboarding.api.application.ApplicationSubmitted;
import be.onboarding.api.application.EventPublicationException;
import be.onboarding.api.application.OnboardingEventPublisher;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends events to the Service Bus queue. Active when {@code SERVICEBUS_ENABLED=true}.
 *
 * <p>The {@link ServiceBusSenderClient} is auto-configured by Spring Cloud Azure from
 * {@code spring.cloud.azure.servicebus.*} and authenticates with the managed identity in Azure
 * (your {@code az login} locally). No connection string.
 */
@Component
@ConditionalOnProperty(name = "spring.cloud.azure.servicebus.enabled", havingValue = "true")
public class ServiceBusEventPublisher implements OnboardingEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusEventPublisher.class);

    private final ServiceBusSenderClient sender;
    private final JsonMapper jsonMapper;

    public ServiceBusEventPublisher(ServiceBusSenderClient sender, JsonMapper jsonMapper) {
        this.sender = sender;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void publish(ApplicationSubmitted event) {
        var message = new ServiceBusMessage(jsonMapper.writeValueAsString(event));
        // Same id for a retried send of the same application: the queue's duplicate detection drops it.
        message.setMessageId(event.applicationId().toString());
        message.setContentType("application/json");
        message.setSubject("ApplicationSubmitted");
        try {
            sender.sendMessage(message);
        } catch (RuntimeException e) {
            throw new EventPublicationException(
                    "Could not publish ApplicationSubmitted for " + event.applicationId(), e);
        }
        logger.info("Published ApplicationSubmitted for application {}", event.applicationId());
    }
}
