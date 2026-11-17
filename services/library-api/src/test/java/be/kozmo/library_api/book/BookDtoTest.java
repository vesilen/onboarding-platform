package be.kozmo.library_api.book;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Plain unit test for the mapping between the API record and the JPA entity. */
class BookDtoTest {

	@Test
	void fromCopiesAllFieldsOfTheEntity() {
		Book book = new Book("Dune", "Frank Herbert", "9780441013593", 1965);

		BookDto dto = BookDto.from(book);

		assertThat(dto).isEqualTo(new BookDto(null, "Dune", "Frank Herbert", "9780441013593", 1965));
	}

	@Test
	void toEntityCreatesANewBookWithoutId() {
		BookDto dto = new BookDto(99L, "Dune", "Frank Herbert", "9780441013593", 1965);

		Book book = dto.toEntity();

		assertThat(book.getId()).as("id from the request is ignored").isNull();
		assertThat(book.getTitle()).isEqualTo("Dune");
		assertThat(book.getAuthor()).isEqualTo("Frank Herbert");
		assertThat(book.getIsbn()).isEqualTo("9780441013593");
		assertThat(book.getPublishedYear()).isEqualTo(1965);
	}

	@Test
	void applyToOverwritesTheFieldsOfAnExistingBook() {
		Book book = new Book("Old title", "Old author", "0000000000000", 2000);
		BookDto update = new BookDto(null, "Dune Messiah", "Frank Herbert", "9780593098233", 1969);

		update.applyTo(book);

		assertThat(BookDto.from(book))
				.isEqualTo(new BookDto(null, "Dune Messiah", "Frank Herbert", "9780593098233", 1969));
	}
}
