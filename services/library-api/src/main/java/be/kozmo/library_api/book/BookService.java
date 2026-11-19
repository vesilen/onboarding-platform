package be.kozmo.library_api.book;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.kozmo.library_api.outbox.OutboxWriter;

/**
 * Book use cases. Every change to a book and its outbox event are saved in one transaction:
 * either both are committed or neither is.
 */
@Service
@Transactional(readOnly = true)
public class BookService {

	private final BookRepository bookRepository;
	private final OutboxWriter outboxWriter;

	public BookService(BookRepository bookRepository, OutboxWriter outboxWriter) {
		this.bookRepository = bookRepository;
		this.outboxWriter = outboxWriter;
	}

	public List<BookDto> findAll() {
		return bookRepository.findAll().stream().map(BookDto::from).toList();
	}

	public BookDto findById(Long id) {
		return BookDto.from(getBookOrThrow(id));
	}

	@Transactional
	public BookDto create(BookDto request) {
		// saveAndFlush: the INSERT runs now, so a duplicate ISBN fails here, before the event is written
		Book book = bookRepository.saveAndFlush(request.toEntity());
		outboxWriter.append(BookEvents.AGGREGATE_TYPE, book.getId(), BookEvents.BOOK_CREATED,
				BookEvents.BookChanged.from(book));
		return BookDto.from(book);
	}

	@Transactional
	public BookDto update(Long id, BookDto request) {
		Book book = getBookOrThrow(id);
		request.applyTo(book);
		bookRepository.saveAndFlush(book);
		outboxWriter.append(BookEvents.AGGREGATE_TYPE, book.getId(), BookEvents.BOOK_UPDATED,
				BookEvents.BookChanged.from(book));
		return BookDto.from(book);
	}

	@Transactional
	public void delete(Long id) {
		Book book = getBookOrThrow(id);
		bookRepository.delete(book);
		outboxWriter.append(BookEvents.AGGREGATE_TYPE, id, BookEvents.BOOK_DELETED, new BookEvents.BookDeleted(id));
	}

	private Book getBookOrThrow(Long id) {
		return bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
	}
}
