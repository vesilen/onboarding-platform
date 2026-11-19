package be.kozmo.library_api.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

	/** The next batch for the relay (step 4): unpublished events, oldest first. */
	List<OutboxEvent> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();

	List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(String aggregateType, String aggregateId);
}
