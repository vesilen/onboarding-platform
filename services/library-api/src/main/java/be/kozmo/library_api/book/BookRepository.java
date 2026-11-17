package be.kozmo.library_api.book;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data generates the implementation at startup; CRUD methods come from JpaRepository. */
public interface BookRepository extends JpaRepository<Book, Long> {

	Optional<Book> findByIsbn(String isbn);

	List<Book> findByAuthorContainingIgnoreCase(String author);
}
