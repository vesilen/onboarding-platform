package be.kozmo.library_api.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import be.kozmo.library_api.TestcontainersConfiguration;
import be.kozmo.library_api.outbox.OutboxEvent;
import be.kozmo.library_api.outbox.OutboxEventRepository;

/**
 * Checks the core promise of the outbox against a real Postgres (schema created by Flyway):
 * a book change and its event are committed together, or not at all.
 * No @Transactional on the test on purpose: every service call commits for real, as in production.
 */
@SpringBootTest(properties = "outbox.relay.enabled=false") // the relay would mark events published mid-test
@Import(TestcontainersConfiguration.class)
class BookServiceTest {

	private static final BookDto DUNE = new BookDto(null, "Dune", "Frank Herbert", "9780441013593", 1965);

	@Autowired
	private BookService bookService;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@BeforeEach
	void cleanDatabase() {
		outboxEventRepository.deleteAll();
		bookRepository.deleteAll();
	}

	@Test
	void createSavesTheBookAndABookCreatedEvent() {
		BookDto created = bookService.create(DUNE);

		assertThat(bookRepository.findById(created.id())).isPresent();
		assertThat(eventsFor(created.id()))
				.singleElement()
				.satisfies(event -> {
					assertThat(event.getEventType()).isEqualTo(BookEvents.BOOK_CREATED);
					assertThat(event.getPayload()).contains("\"title\"").contains("Dune");
					assertThat(event.isPublished()).isFalse();
				});
	}

	@Test
	void failedCreateLeavesNoBookAndNoEvent() {
		bookService.create(DUNE);
		BookDto sameIsbn = new BookDto(null, "Another book", "Someone", DUNE.isbn(), 2000);

		assertThatThrownBy(() -> bookService.create(sameIsbn))
				.isInstanceOf(DataIntegrityViolationException.class);

		assertThat(bookRepository.count()).isEqualTo(1);
		assertThat(outboxEventRepository.count()).isEqualTo(1); // only the event of the first book
	}

	@Test
	void updateAndDeleteEachAddAnEvent() {
		BookDto created = bookService.create(DUNE);

		bookService.update(created.id(), new BookDto(null, "Dune (50th anniversary)", "Frank Herbert", DUNE.isbn(), 2015));
		bookService.delete(created.id());

		assertThat(bookRepository.findById(created.id())).isEmpty();
		assertThat(eventsFor(created.id()))
				.extracting(OutboxEvent::getEventType)
				.containsExactly(BookEvents.BOOK_CREATED, BookEvents.BOOK_UPDATED, BookEvents.BOOK_DELETED);
	}

	@Test
	void updateOfUnknownBookAddsNoEvent() {
		assertThatThrownBy(() -> bookService.update(42L, DUNE)).isInstanceOf(BookNotFoundException.class);

		assertThat(outboxEventRepository.count()).isZero();
	}

	private List<OutboxEvent> eventsFor(Long bookId) {
		return outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
				BookEvents.AGGREGATE_TYPE, String.valueOf(bookId));
	}
}
