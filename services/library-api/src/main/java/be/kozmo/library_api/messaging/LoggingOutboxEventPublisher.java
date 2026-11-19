package be.kozmo.library_api.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import be.kozmo.library_api.outbox.OutboxEvent;
import be.kozmo.library_api.outbox.OutboxEventPublisher;

/** Fallback when Service Bus is disabled (local runs, CI, tests): only logs the event. */
@Component
@ConditionalOnProperty(name = "spring.cloud.azure.servicebus.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingOutboxEventPublisher implements OutboxEventPublisher {

	private static final Logger logger = LoggerFactory.getLogger(LoggingOutboxEventPublisher.class);

	@Override
	public void publish(OutboxEvent event) {
		logger.info("Service Bus disabled, not publishing {} for {} {} (event {}): {}",
				event.getEventType(), event.getAggregateType(), event.getAggregateId(), event.getId(),
				event.getPayload());
	}
}
