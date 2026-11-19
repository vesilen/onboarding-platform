package be.kozmo.library_api.outbox;

/**
 * Sends one outbox event to the message broker.
 *
 * <p>The relay only knows this interface; whether events go to Service Bus or just to the log is
 * decided by configuration (see the implementations in {@code be.kozmo.library_api.messaging}).
 * An implementation must throw when the event was not delivered, so the relay leaves it unpublished
 * and retries it on the next run.
 */
public interface OutboxEventPublisher {

	void publish(OutboxEvent event);
}
