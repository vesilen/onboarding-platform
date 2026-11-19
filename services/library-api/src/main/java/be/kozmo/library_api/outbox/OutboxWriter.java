package be.kozmo.library_api.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

/**
 * Adds events to the outbox. Propagation.MANDATORY makes the pattern explicit: calling this
 * without a surrounding transaction fails, because an outbox row written in its own transaction
 * would bring back exactly the dual-write problem the outbox is meant to solve.
 */
@Component
public class OutboxWriter {

	private final OutboxEventRepository repository;
	private final JsonMapper jsonMapper;

	public OutboxWriter(OutboxEventRepository repository, JsonMapper jsonMapper) {
		this.repository = repository;
		this.jsonMapper = jsonMapper;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public OutboxEvent append(String aggregateType, Object aggregateId, String eventType, Object payload) {
		String json = jsonMapper.writeValueAsString(payload);
		return repository.save(new OutboxEvent(aggregateType, String.valueOf(aggregateId), eventType, json));
	}
}
