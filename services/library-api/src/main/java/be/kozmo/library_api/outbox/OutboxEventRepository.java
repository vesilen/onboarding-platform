package be.kozmo.library_api.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

	/**
	 * The next batch for the relay: unpublished events, oldest first, locked until the transaction
	 * ends. SKIP LOCKED makes a second instance of the API skip rows the first one is already
	 * publishing, instead of waiting for them or publishing them twice.
	 */
	@Query(value = """
			SELECT * FROM outbox_events
			WHERE published_at IS NULL
			ORDER BY created_at
			LIMIT :batchSize
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<OutboxEvent> lockNextBatch(@Param("batchSize") int batchSize);

	List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(String aggregateType, String aggregateId);
}
