package be.kozmo.library_api.book;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Web-layer test: the repository is mocked with Mockito, so only HTTP mapping and JSON are tested. */
@ExtendWith(MockitoExtension.class)
class BookControllerTest {

	private static final String BOOK_JSON = """
			{"title": "Dune", "author": "Frank Herbert", "isbn": "9780441013593", "publishedYear": 1965}
			""";

	@Mock
	private BookRepository bookRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new BookController(bookRepository)).build();
	}

	@Test
	void findAllReturnsEveryBook() throws Exception {
		when(bookRepository.findAll()).thenReturn(List.of(
				book(1L, "Dune", "Frank Herbert"),
				book(2L, "Neuromancer", "William Gibson")));

		mockMvc.perform(get("/api/v1/books"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].title").value("Dune"))
				.andExpect(jsonPath("$[1].author").value("William Gibson"));
	}

	@Test
	void findByIdReturnsTheBook() throws Exception {
		when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1L, "Dune", "Frank Herbert")));

		mockMvc.perform(get("/api/v1/books/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.title").value("Dune"))
				.andExpect(jsonPath("$.author").value("Frank Herbert"));
	}

	@Test
	void findByIdReturnsNotFoundForUnknownBook() throws Exception {
		when(bookRepository.findById(42L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/books/42"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createSavesTheBookAndReturnsCreated() throws Exception {
		when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
			Book saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", 7L); // what the database would do
			return saved;
		});

		mockMvc.perform(post("/api/v1/books")
						.contentType(MediaType.APPLICATION_JSON)
						.content(BOOK_JSON))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.title").value("Dune"))
				.andExpect(jsonPath("$.isbn").value("9780441013593"))
				.andExpect(jsonPath("$.publishedYear").value(1965));
	}

	@Test
	void updateChangesTheExistingBook() throws Exception {
		when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1L, "Old title", "Old author")));
		when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(put("/api/v1/books/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(BOOK_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.title").value("Dune"))
				.andExpect(jsonPath("$.author").value("Frank Herbert"));
	}

	@Test
	void updateReturnsNotFoundForUnknownBook() throws Exception {
		when(bookRepository.findById(42L)).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/v1/books/42")
						.contentType(MediaType.APPLICATION_JSON)
						.content(BOOK_JSON))
				.andExpect(status().isNotFound());

		verify(bookRepository, never()).save(any());
	}

	@Test
	void deleteRemovesTheBook() throws Exception {
		Book book = book(1L, "Dune", "Frank Herbert");
		when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

		mockMvc.perform(delete("/api/v1/books/1"))
				.andExpect(status().isNoContent());

		verify(bookRepository).delete(book);
	}

	@Test
	void deleteReturnsNotFoundForUnknownBook() throws Exception {
		when(bookRepository.findById(42L)).thenReturn(Optional.empty());

		mockMvc.perform(delete("/api/v1/books/42"))
				.andExpect(status().isNotFound());

		verify(bookRepository, never()).delete(any());
	}

	private static Book book(Long id, String title, String author) {
		Book book = new Book(title, author, null, null);
		ReflectionTestUtils.setField(book, "id", id);
		return book;
	}
}
