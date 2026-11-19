package be.kozmo.library_api.book;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Web-layer test: the service is mocked with Mockito, so only HTTP mapping and JSON are tested. */
@ExtendWith(MockitoExtension.class)
class BookControllerTest {

	private static final String BOOK_JSON = """
			{"title": "Dune", "author": "Frank Herbert", "isbn": "9780441013593", "publishedYear": 1965}
			""";

	private static final BookDto DUNE = new BookDto(1L, "Dune", "Frank Herbert", "9780441013593", 1965);

	@Mock
	private BookService bookService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new BookController(bookService)).build();
	}

	@Test
	void findAllReturnsEveryBook() throws Exception {
		when(bookService.findAll()).thenReturn(List.of(
				DUNE,
				new BookDto(2L, "Neuromancer", "William Gibson", null, null)));

		mockMvc.perform(get("/api/v1/books"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].title").value("Dune"))
				.andExpect(jsonPath("$[1].author").value("William Gibson"));
	}

	@Test
	void findByIdReturnsTheBook() throws Exception {
		when(bookService.findById(1L)).thenReturn(DUNE);

		mockMvc.perform(get("/api/v1/books/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.title").value("Dune"))
				.andExpect(jsonPath("$.author").value("Frank Herbert"));
	}

	@Test
	void findByIdReturnsNotFoundForUnknownBook() throws Exception {
		when(bookService.findById(42L)).thenThrow(new BookNotFoundException(42L));

		mockMvc.perform(get("/api/v1/books/42"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createReturnsCreatedWithTheSavedBook() throws Exception {
		when(bookService.create(any(BookDto.class)))
				.thenReturn(new BookDto(7L, "Dune", "Frank Herbert", "9780441013593", 1965));

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
	void updateReturnsTheUpdatedBook() throws Exception {
		when(bookService.update(eq(1L), any(BookDto.class))).thenReturn(DUNE);

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
		when(bookService.update(eq(42L), any(BookDto.class))).thenThrow(new BookNotFoundException(42L));

		mockMvc.perform(put("/api/v1/books/42")
						.contentType(MediaType.APPLICATION_JSON)
						.content(BOOK_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/v1/books/1"))
				.andExpect(status().isNoContent());

		verify(bookService).delete(1L);
	}

	@Test
	void deleteReturnsNotFoundForUnknownBook() throws Exception {
		doThrow(new BookNotFoundException(42L)).when(bookService).delete(42L);

		mockMvc.perform(delete("/api/v1/books/42"))
				.andExpect(status().isNotFound());
	}
}
