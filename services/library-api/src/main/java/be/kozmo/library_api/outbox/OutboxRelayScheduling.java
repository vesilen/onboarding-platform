package be.kozmo.library_api.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Runs the outbox relay on a fixed delay. Set outbox.relay.enabled=false to switch it off, for
 * example in tests that call OutboxRelay.publishPending() themselves.
 *
 * <p>The @Scheduled method lives here and not in OutboxRelay on purpose: calling publishPending()
 * from another bean goes through Spring's proxy, so its @Transactional applies. A call from inside
 * OutboxRelay itself would bypass the proxy and run without a transaction.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
class OutboxRelayScheduling {

	private final OutboxRelay relay;

	OutboxRelayScheduling(OutboxRelay relay) {
		this.relay = relay;
	}

	@Scheduled(fixedDelayString = "${outbox.relay.fixed-delay:5s}")
	void run() {
		relay.publishPending();
	}
}
