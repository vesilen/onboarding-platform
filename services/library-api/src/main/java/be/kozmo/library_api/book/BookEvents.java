package be.kozmo.library_api.book;

/**
 * The events the library publishes about books. These records are the public contract for
 * consumers, deliberately separate from BookDto (the REST contract) so each can change on its own.
 */
public final class BookEvents {

	public static final String AGGREGATE_TYPE = "Book";

	public static final String BOOK_CREATED = "BookCreated";
	public static final String BOOK_UPDATED = "BookUpdated";
	public static final String BOOK_DELETED = "BookDeleted";

	private BookEvents() {
	}

	/** Payload of BookCreated and BookUpdated: the full state of the book after the change. */
	public record BookChanged(Long id, String title, String author, String isbn, Integer publishedYear) {

		static BookChanged from(Book book) {
			return new BookChanged(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
					book.getPublishedYear());
		}
	}

	/** Payload of BookDeleted. */
	public record BookDeleted(Long id) {
	}
}
