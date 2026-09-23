package com.todoservice.todoservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.todoservice.todoservice.model.Todo;
import com.todoservice.todoservice.repository.TodoRepository;

/**
 * INTEGRATION TEST for {@link TodoService}.
 *
 * Unlike TodoServiceTest (which uses Mockito to mock the repository),
 * this test boots the REAL Spring application context and talks to a
 * REAL PostgreSQL database running inside a Docker container.
 *
 * What this exercises end-to-end:
 *   - Spring context startup (component scanning, bean wiring)
 *   - JPA / Hibernate entity mapping for Todo
 *   - Spring Data JPA repository (TodoRepository)
 *   - TodoService business logic on top of a real database
 *
 * The database is provided by Testcontainers: a throwaway PostgreSQL
 * container is started before the test class runs and stopped afterwards,
 * so tests never depend on a locally installed database or docker-compose.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class TodoServiceIntegrationTest {

	/**
	 * The PostgreSQL container used by this test class.
	 *
	 * `static` + @Container = one container per test CLASS (started once,
	 * reused for every test method, stopped after the class finishes).
	 * This is faster than starting a container per method.
	 *
	 * The image matches docker-compose.yml (postgres:16-alpine) and uses the
	 * same database/user/password names, but Testcontainers assigns a
	 * RANDOM host port, so it never clashes with a local PostgreSQL.
	 */
	@Container
	@ServiceConnection // Spring Boot reads the container's JDBC URL/user/password
					  // and auto-configures the DataSource for us - no
					  // @DynamicPropertySource boilerplate needed.
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
			.withDatabaseName("todoservice")
			.withUsername("todoservice")
			.withPassword("todoservice");

	// The real service under test, wired by Spring from the application context.
	@Autowired
	private TodoService todoService;

	// The real repository - used only to clean and seed data between tests.
	@Autowired
	private TodoRepository todoRepository;

	/**
	 * Runs before EVERY test method.
	 * We wipe the `todos` table so each test starts from a known, empty state
	 * and can never be affected by data left behind by a previous test.
	 */
	@BeforeEach
	void cleanDatabase() {
		todoRepository.deleteAll();
	}

	@Test
	@DisplayName("create() persists a new Todo in the real database")
	void shouldCreateAndPersistTodo() {
		// ACT: call the service with real JPA + PostgreSQL behind it.
		Todo created = todoService.create("Buy milk", false);

		// ASSERT: the service returned a persisted entity...
		assertThat(created.getId()).isNotNull(); // database generated the ID
		assertThat(created.getTitle()).isEqualTo("Buy milk");
		assertThat(created.isCompleted()).isFalse();

		// ...and it is ACTUALLY stored: read it back through an independent
		// repository lookup (a separate round-trip to the database).
		Optional<Todo> found = todoRepository.findById(created.getId());
		assertThat(found).isPresent();
		assertThat(found.get().getTitle()).isEqualTo("Buy milk");
	}

	@Test
	@DisplayName("create() persists completed=true correctly")
	void shouldCreateCompletedTodo() {
		Todo created = todoService.create("Pay the bills", true);
		// I expect created.isCompleted() to be true.
		assertThat(created.isCompleted()).isTrue();

		// Verify the flag survived the round-trip through the database.
		assertThat(todoRepository.findById(created.getId()).orElseThrow().isCompleted())
				.isTrue();
	}

	@Test
	@DisplayName("findAll() returns every Todo stored in the database")
	void shouldFindAllTodos() {
		// Arrange: seed 2 rows directly through the repository.
		todoRepository.save(new Todo("Buy milk", false));
		todoRepository.save(new Todo("Do homework", true));

		// Act
		List<Todo> result = todoService.findAll();

		// Assert: the service sees exactly what the database contains.
		assertThat(result).hasSize(2);
		assertThat(result).extracting(Todo::getTitle)
				.containsExactlyInAnyOrder("Buy milk", "Do homework");
	}

	@Test
	@DisplayName("findAll() returns an empty list when the table is empty")
	void shouldReturnEmptyListWhenNoTodos() {
		// @BeforeEach already emptied the table.
		List<Todo> result = todoService.findAll();

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findById() returns the stored Todo when it exists")
	void shouldFindByIdWhenExists() {
		Todo saved = todoRepository.save(new Todo("Buy milk", false));

		Optional<Todo> result = todoService.findById(saved.getId());

		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(saved.getId());
		assertThat(result.get().getTitle()).isEqualTo("Buy milk");
	}

	@Test
	@DisplayName("findById() returns empty for an id that is not in the database")
	void shouldReturnEmptyWhenTodoDoesNotExist() {
		Optional<Todo> result = todoService.findById(9999L);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("update() changes title and completed and persists the change")
	void shouldUpdateTitleAndCompleted() {
		Todo saved = todoRepository.save(new Todo("Old title", false));

		Optional<Todo> result = todoService.update(saved.getId(), "New title", true);

		// Service returned the updated entity...
		assertThat(result).isPresent();
		assertThat(result.get().getTitle()).isEqualTo("New title");
		assertThat(result.get().isCompleted()).isTrue();

		// ...and the UPDATE really hit the database (fresh read).
		Todo reloaded = todoRepository.findById(saved.getId()).orElseThrow();
		assertThat(reloaded.getTitle()).isEqualTo("New title");
		assertThat(reloaded.isCompleted()).isTrue();
	}

	@Test
	@DisplayName("update() with completed=null keeps the existing completed value")
	void shouldUpdateTitleOnlyWhenCompletedIsNull() {
		Todo saved = todoRepository.save(new Todo("Old title", true));

		Optional<Todo> result = todoService.update(saved.getId(), "New title", null);

		assertThat(result).isPresent();
		assertThat(result.get().getTitle()).isEqualTo("New title");
		// The original completed=true must NOT be overwritten by null.
		assertThat(result.get().isCompleted()).isTrue();
	}

	@Test
	@DisplayName("update() with title=null keeps the existing title")
	void shouldUpdateCompletedOnlyWhenTitleIsNull() {
		Todo saved = todoRepository.save(new Todo("Keep this title", false));

		Optional<Todo> result = todoService.update(saved.getId(), null, true);

		assertThat(result).isPresent();
		assertThat(result.get().getTitle()).isEqualTo("Keep this title");
		assertThat(result.get().isCompleted()).isTrue();
	}

	@Test
	@DisplayName("update() on a missing id returns empty and writes nothing")
	void shouldReturnEmptyWhenUpdatingMissingTodo() {
		Optional<Todo> result = todoService.update(9999L, "New title", true);

		assertThat(result).isEmpty();
		// Nothing new should have appeared in the database.
		assertThat(todoRepository.count()).isZero();
	}

	@Test
	@DisplayName("delete() removes an existing Todo from the database")
	void shouldDeleteExistingTodo() {
		Todo saved = todoRepository.save(new Todo("Buy milk", false));

		boolean deleted = todoService.delete(saved.getId());

		// Service reports success and the row is really gone.
		assertThat(deleted).isTrue();
		assertThat(todoRepository.findById(saved.getId())).isEmpty();
	}

	@Test
	@DisplayName("delete() returns false for a missing id and changes nothing")
	void shouldReturnFalseWhenTodoDoesNotExist() {
		boolean deleted = todoService.delete(9999L);

		assertThat(deleted).isFalse();
	}
}
