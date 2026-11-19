package be.kozmo.library_api.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import be.kozmo.library_api.TestcontainersConfiguration;

/**
 * The relay against a real Postgres (so the native SKIP LOCKED query runs for real), with the
 * publisher mocked. Scheduling is off: the test calls publishPending() itself.
 */
@SpringBootTest(properties = "outbox.relay.enabled=false")
@Import(TestcontainersConfiguration.class)
class OutboxRelayTest {

	@Autowired
	private OutboxRelay relay;

	@Autowired
	private OutboxEventRepository repository;

	@MockitoBean
	private OutboxEventPublisher publisher;

	@BeforeEach
	void cleanOutbox() {
		repository.deleteAll();
	}

	@Test
	void publishesPendingEventsOldestFirstAndMarksThemPublished() {
		OutboxEvent first = save("BookCreated");
		OutboxEvent second = save("BookUpdated");

		int published = relay.publishPending();

		assertThat(published).isEqualTo(2);
		InOrder inOrder = inOrder(publisher);
		inOrder.verify(publisher).publish(argThat(event -> event.getId().equals(first.getId())));
		inOrder.verify(publisher).publish(argThat(event -> event.getId().equals(second.getId())));
		assertThat(repository.findAll()).allMatch(OutboxEvent::isPublished);
	}

	@Test
	void doesNotPublishAnEventTwice() {
		save("BookCreated");
		relay.publishPending();

		int publishedOnSecondRun = relay.publishPending();

		assertThat(publishedOnSecondRun).isZero();
	}

	@Test
	void stopsAtTheFirstFailureAndRetriesOnTheNextRun() {
		OutboxEvent ok = save("BookCreated");
		OutboxEvent failing = save("BookUpdated");
		OutboxEvent after = save("BookDeleted");
		doThrow(new IllegalStateException("broker down"))
				.when(publisher).publish(argThat(event -> event != null && event.getId().equals(failing.getId())));

		int published = relay.publishPending();

		assertThat(published).isEqualTo(1);
		assertThat(isPublished(ok.getId())).isTrue();
		assertThat(isPublished(failing.getId())).isFalse();
		assertThat(isPublished(after.getId())).isFalse(); // order is kept: nothing overtakes the failed event

		doNothing().when(publisher).publish(any());
		assertThat(relay.publishPending()).isEqualTo(2);
		assertThat(repository.findAll()).allMatch(OutboxEvent::isPublished);
	}

	@Test
	void doesNothingWhenTheOutboxIsEmpty() {
		assertThat(relay.publishPending()).isZero();
		verifyNoInteractions(publisher);
	}

	private OutboxEvent save(String eventType) {
		return repository.save(new OutboxEvent("Book", "1", eventType, "{\"id\": 1}"));
	}

	private boolean isPublished(UUID id) {
		return repository.findById(id).orElseThrow().isPublished();
	}
}
