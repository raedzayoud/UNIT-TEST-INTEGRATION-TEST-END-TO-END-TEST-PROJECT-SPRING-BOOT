package com.todoservice.todoservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Default Spring Boot "context loads" smoke test.
 *
 * Uses a Testcontainers PostgreSQL instead of the local dev database
 * (application.properties -> localhost:5433) so the test is fully
 * self-contained and does not require docker-compose to be running.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class TodoserviceApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
			.withDatabaseName("todoservice")
			.withUsername("todoservice")
			.withPassword("todoservice");

	@Test
	void contextLoads() {
	}

}