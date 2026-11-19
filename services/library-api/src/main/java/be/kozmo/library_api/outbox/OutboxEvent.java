package be.kozmo.library_api.outbox;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One row in the transactional outbox: an event that still has to be (or already was) published.
 * The id is generated here, not by the database, so it can later double as the message id
 * that consumers use to detect duplicates.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

	@Id
	private UUID id;

	@Column(nullable = false, length = 100)
	private String aggregateType;

	@Column(nullable = false, length = 100)
	private String aggregateId;

	@Column(nullable = false, length = 100)
	private String eventType;

	/** Event body as JSON text, stored in a Postgres jsonb column. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String payload;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant publishedAt;

	/** Required by JPA. */
	protected OutboxEvent() {
	}

	public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
		this.id = UUID.randomUUID();
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.payload = payload;
		this.createdAt = Instant.now();
	}

	public void markPublished() {
		this.publishedAt = Instant.now();
	}

	public boolean isPublished() {
		return publishedAt != null;
	}

	public UUID getId() {
		return id;
	}

	public String getAggregateType() {
		return aggregateType;
	}

	public String getAggregateId() {
		return aggregateId;
	}

	public String getEventType() {
		return eventType;
	}

	public String getPayload() {
		return payload;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getPublishedAt() {
		return publishedAt;
	}
}
