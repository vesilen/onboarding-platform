package be.kozmo.library_api.book;

/**
 * What the API sends and receives, so the JPA entity is never exposed directly.
 * The id is ignored on create/update and filled in on responses.
 */
public record BookDto(Long id, String title, String author, String isbn, Integer publishedYear) {

	public static BookDto from(Book book) {
		return new BookDto(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
				book.getPublishedYear());
	}

	public Book toEntity() {
		return new Book(title, author, isbn, publishedYear);
	}

	public void applyTo(Book book) {
		book.setTitle(title);
		book.setAuthor(author);
		book.setIsbn(isbn);
		book.setPublishedYear(publishedYear);
	}
}
