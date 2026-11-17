package be.kozmo.library_api.book;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** CRUD endpoints for books. */
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

	private final BookRepository bookRepository;

	public BookController(BookRepository bookRepository) {
		this.bookRepository = bookRepository;
	}

	@GetMapping
	public List<BookDto> findAll(@RequestParam(required = false) String author) {
		List<Book> books = (author == null)
				? bookRepository.findAll()
				: bookRepository.findByAuthorContainingIgnoreCase(author);
		return books.stream().map(BookDto::from).toList();
	}

	@GetMapping("/{id}")
	public BookDto findById(@PathVariable Long id) {
		return BookDto.from(getBookOrThrow(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookDto create(@RequestBody BookDto request) {
		return BookDto.from(bookRepository.save(request.toEntity()));
	}

	@PutMapping("/{id}")
	public BookDto update(@PathVariable Long id, @RequestBody BookDto request) {
		Book book = getBookOrThrow(id);
		request.applyTo(book);
		return BookDto.from(bookRepository.save(book));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		bookRepository.delete(getBookOrThrow(id));
	}

	private Book getBookOrThrow(Long id) {
		return bookRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book " + id + " not found"));
	}
}
