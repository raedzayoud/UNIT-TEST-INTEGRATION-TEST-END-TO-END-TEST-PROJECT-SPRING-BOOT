package com.todoservice.todoservice.service;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.todoservice.todoservice.model.Todo;
import com.todoservice.todoservice.repository.TodoRepository;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * END-TO-END (E2E) TEST for the whole HTTP stack.
 *
 * Compared to the other two test classes:
 *   - TodoServiceTest:       mocks the repository (fastest, no DB).
 *   - TodoServiceIntegrationTest: real DB, but calls the service directly.
 *   - THIS E2E test:         real DB + real Tomcat server + the full REST API
 *                            (HTTP request -> TodoController -> TodoService
 *                             -> TodoRepository -> PostgreSQL -> response).
 *
 * A throwaway PostgreSQL container (Testcontainers) supplies the database and
 * Spring Boot starts the app on a RANDOM free port. Requests are sent over real
 * HTTP with rest-assured, exactly like a browser or external client would.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EndToEndTestingTest {

	/**
	 * The PostgreSQL container for the whole test class.
	 * @ServiceConnection lets Spring Boot auto-configure the DataSource from
	 * the running container (JDBC URL, user, password) - no manual config.
	 */
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
			.withDatabaseName("todoservice")
			.withUsername("todoservice")
			.withPassword("todoservice");

	// Injects the random port Tomcat is listening on for THIS test run.
	@LocalServerPort
	int port;

	// Only used to reset the database between tests.
	@Autowired
	private TodoRepository todoRepository;

	@BeforeEach
	void setUp() {
		// Point rest-assured at the running application before each test.
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;

		// Start every test with an empty table so tests never interfere.
		todoRepository.deleteAll();
	}

	@Test
	@DisplayName("POST /api/todos creates a Todo and GET /api/todos returns it")
	void shouldCreateTodoViaApi() {
		// ACT: create a Todo through the real HTTP endpoint.
		int createdId = given()
				.contentType(ContentType.JSON)
				.body("{ \"title\": \"Buy milk\", \"completed\": false }")
				.when()
				.post("/api/todos")
				.then()
				// ASSERT: the API answers 201 Created with the stored entity.
				.statusCode(201)
				.body("title", equalTo("Buy milk"))
				.body("completed", equalTo(false))
				// Extract the database-generated id for the next request.
				.extract()
				.jsonPath()
				.getInt("id");

		// ACT: fetch the list via HTTP and ASSER the new Todo is there.
		when()
				.get("/api/todos")
				.then()
				.statusCode(200)
				.body("$", hasSize(1))
				.body("[0].id", equalTo(createdId))
				.body("[0].title", equalTo("Buy milk"));
	}

	@Test
	@DisplayName("GET /api/todos/{id} returns the Todo or 404 when missing")
	void shouldGetTodoById() {

		// Seed one row directly through the repository (independent of the API).
		Todo saved = todoRepository.save(new Todo("Do homework", true));

		// Fetching an existing id returns 200 with the correct body.
		when()
				.get("/api/todos/" + saved.getId())
				.then()
				.statusCode(200)
				.body("id", equalTo(saved.getId().intValue()))
				.body("title", equalTo("Do homework"))
				.body("completed", equalTo(true));

		// Fetching an unknown id returns 404 Not Found.
		when()
				.get("/api/todos/9999")
				.then()
				.statusCode(404);
	}

	@Test
	@DisplayName("PUT /api/todos/{id} updates a Todo over HTTP")
	void shouldUpdateTodoViaApi() {
		int createdId = given()
				.contentType(ContentType.JSON)
				.body("{ \"title\": \"Old title\", \"completed\": false }")
				.when()
				.post("/api/todos")
				.then()
				.statusCode(201)
				.extract()
				.jsonPath()
				.getInt("id");

		// ACT: update title AND completed.
		given()
				.contentType(ContentType.JSON)
				.body("{ \"title\": \"New title\", \"completed\": true }")
				.when()
				.put("/api/todos/" + createdId)
				.then()
				.statusCode(200)
				.body("title", equalTo("New title"))
				.body("completed", equalTo(true));

		// ASSERT: the change really stuck - read it back over HTTP.
		when()
				.get("/api/todos/" + createdId)
				.then()
				.statusCode(200)
				.body("title", equalTo("New title"))
				.body("completed", equalTo(true));

		// Updating a missing id returns 404.
		given()
				.contentType(ContentType.JSON)
				.body("{ \"title\": \"x\", \"completed\": false }")
				.when()
				.put("/api/todos/9999")
				.then()
				.statusCode(404);
	}

	@Test
	@DisplayName("DELETE /api/todos/{id} removes a Todo over HTTP")
	void shouldDeleteTodoViaApi() {
		int createdId = given()
				.contentType(ContentType.JSON)
				.body("{ \"title\": \"Buy milk\", \"completed\": false }")
				.when()
				.post("/api/todos")
				.then()
				.statusCode(201)
				.extract()
				.jsonPath()
				.getInt("id");

		// Delete returns 204 No Content and the row is really gone.
		when()
				.delete("/api/todos/" + createdId)
				.then()
				.statusCode(204);

		// A subsequent GET returns 404, proving the row was removed.
		when()
				.get("/api/todos/" + createdId)
				.then()
				.statusCode(404);

		// Deleting an already-deleted (or missing) id also returns 404.
		when()
				.delete("/api/todos/" + createdId)
				.then()
				.statusCode(404);
	}

	@Test
	@DisplayName("GET /api/todos returns an empty list for an empty table")
	void shouldReturnEmptyListWhenNoTodos() {
		// @BeforeEach already emptied the table.
		when()
				.get("/api/todos")
				.then()
				.statusCode(200)
				.body("$", hasSize(0));
	}
}