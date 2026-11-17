package be.kozmo.library_api.book;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import be.kozmo.library_api.TestcontainersConfiguration;

/** Persistence test against a real Postgres: checks the entity mapping and the derived queries. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BookRepositoryTest {

	@Autowired
	private BookRepository bookRepository;

	@Test
	void saveAssignsAnId() {
		Book saved = bookRepository.save(new Book("Dune", "Frank Herbert", "9780441013593", 1965));

		assertThat(saved.getId()).isNotNull();
		assertThat(bookRepository.findById(saved.getId())).isPresent();
	}

	@Test
	void findByIsbnReturnsTheMatchingBook() {
		bookRepository.save(new Book("Dune", "Frank Herbert", "9780441013593", 1965));
		bookRepository.save(new Book("Neuromancer", "William Gibson", "9780441569595", 1984));

		assertThat(bookRepository.findByIsbn("9780441569595"))
				.get()
				.extracting(Book::getTitle)
				.isEqualTo("Neuromancer");
		assertThat(bookRepository.findByIsbn("0000000000000")).isEmpty();
	}

	@Test
	void findByAuthorContainingIgnoreCaseMatchesPartOfTheName() {
		bookRepository.save(new Book("Dune", "Frank Herbert", "9780441013593", 1965));
		bookRepository.save(new Book("Dune Messiah", "Frank Herbert", "9780593098233", 1969));
		bookRepository.save(new Book("Neuromancer", "William Gibson", "9780441569595", 1984));

		assertThat(bookRepository.findByAuthorContainingIgnoreCase("herb"))
				.extracting(Book::getTitle)
				.containsExactlyInAnyOrder("Dune", "Dune Messiah");
	}
}
