package be.kozmo.library_api.outbox;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves events from the outbox table to the message broker.
 *
 * <p>Each run locks the oldest unpublished events, publishes them one by one and marks them
 * published; the marks are committed together at the end of the run. Delivery is at-least-once:
 * if the application stops after publishing but before the commit, the events are sent again on
 * the next run, so consumers must ignore duplicates (the outbox id is the message id).
 */
@Component
public class OutboxRelay {

	private static final Logger logger = LoggerFactory.getLogger(OutboxRelay.class);

	private final OutboxEventRepository repository;
	private final OutboxEventPublisher publisher;
	private final int batchSize;

	public OutboxRelay(OutboxEventRepository repository, OutboxEventPublisher publisher,
			@Value("${outbox.relay.batch-size:50}") int batchSize) {
		this.repository = repository;
		this.publisher = publisher;
		this.batchSize = batchSize;
	}

	/**
	 * Publishes one batch and returns how many events were published. Called on a fixed delay by
	 * OutboxRelayScheduling, and directly by tests.
	 *
	 * <p>Stops at the first failure so events of the same book are never published out of order;
	 * the failed event and everything after it are retried on the next run.
	 */
	@Transactional
	public int publishPending() {
		List<OutboxEvent> batch = repository.lockNextBatch(batchSize);
		int published = 0;
		for (OutboxEvent event : batch) {
			try {
				publisher.publish(event);
			} catch (RuntimeException e) {
				logger.warn("Publishing {} {} failed, retrying on the next run", event.getEventType(), event.getId(), e);
				break;
			}
			event.markPublished(); // dirty checking: Hibernate writes the UPDATE at commit
			published++;
		}
		if (published > 0) {
			logger.debug("Published {} outbox event(s)", published);
		}
		return published;
	}
}
