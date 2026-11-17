package be.kozmo.library_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Smoke test: the full application context starts against a real Postgres. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LibraryApiApplicationTests {

	@Test
	void contextLoads() {
	}
}
