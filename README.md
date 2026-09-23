 # Todo Service — Unit, Integration & End-to-End Testing with Spring Boot

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F)
![Tests](https://img.shields.io/badge/tests-31%20%F0%9F%94%92-brightgreen)

A small but **fully tested** REST API for managing Todos. It exists to demonstrate the **testing pyramid** in a real Spring Boot application: fast unit tests, database-backed integration tests, and end-to-end HTTP tests — all automated and CI-friendly.

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| Language / JDK | Java 25 |
| Framework | Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA) |
| Database | PostgreSQL 16 (local via Docker Compose, tests via Testcontainers) |
| Build | Maven (wrapper: `mvnw` / `mvnw.cmd`) |
| Unit tests | JUnit 5 + Mockito |
| Integration tests | Testcontainers 2.0.5 + `@ServiceConnection` |
| End-to-end tests | REST Assured 6.0.1 against a real embedded Tomcat |

## 🏗 Architecture

```
HTTP ──▶ TodoController ──▶ TodoService ──▶ TodoRepository ──▶ PostgreSQL
         (REST, JSON)        (business)     (Spring Data JPA)
```

## 📡 REST API

| Method | Endpoint | Body | Success | Not found |
|---|---|---|---|---|
| GET | `/api/todos` | — | `200` list | — |
| GET | `/api/todos/{id}` | — | `200` todo | `404` |
| POST | `/api/todos` | `{"title","completed"}` | `201` created | — |
| PUT | `/api/todos/{id}` | `{"title"?, "completed"?}` | `200` updated | `404` |
| DELETE | `/api/todos/{id}` | — | `204` | `404` |

**Example**

```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"Learn testing","completed":false}'
```

## 🧪 Testing Strategy — Three Layers

| Layer | Class | What it proves | Isolation |
|---|---|---|---|
| **Unit** (13 tests) | `TodoServiceTest` | Business logic of the service | Mockito mock repository — no DB, no HTTP, milliseconds |
| **Integration** (12 tests) | `TodoServiceIntegrationTest` | Service + JPA mapping against a **real PostgreSQL** | Testcontainers container, no HTTP server |
| **End-to-end** (5 tests) | `EndToEndTestingTest` | The full HTTP contract: Controller → Service → DB | Real Tomcat (`RANDOM_PORT`) + REST Assured + Testcontainers |
| **Smoke** (1 test) | `TodoserviceApplicationTests` | The whole Spring context boots | Testcontainers container |

```
        ┌────────────────────────┐
        │   E2E  (5)  — HTTP     │   ← slowest, fewest
        ├────────────────────────┤
        │ Integration (12) — DB  │
        ├────────────────────────┤
        │  Unit (13) — mocks     │   ← fastest, most
        └────────────────────────┘
```

**Total: 31 tests, 0 failures, no manual setup required.**

### Run all tests

```bash
# Linux / macOS
./mvnw test

# Windows
.\mvnw.cmd test
```

### Run one layer

```bash
./mvnw test -Dtest=TodoServiceTest               # unit
./mvnw test -Dtest=TodoServiceIntegrationTest    # integration
./mvnw test -Dtest=EndToEndTestingTest           # end-to-end
```

> All tests are self-contained: they start their own PostgreSQL containers via **Testcontainers**, so they run anywhere Docker runs (CI included). The integration and E2E tests skip automatically if Docker is unavailable (`disabledWithoutDocker`).

## ⚙️ Getting Started

### Prerequisites
- JDK 25+
- Maven (or use the included wrapper)
- Docker + Docker Compose

### 1. Start the development database

```bash
docker compose up -d
```

PostgreSQL runs on **localhost:5433** (user/password/db: `todoservice`), with pgAdmin at **http://localhost:5050** (`admin@example.com` / `admin`).

### 2. Run the application

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080/api/todos`.

### 3. Run the tests

```bash
./mvnw test
```

*(Tests do not need step 1 — they use Testcontainers.)*

## 📁 Project Structure

```
src/
├── main/java/com/todoservice/todoservice/
│   ├── controller/TodoController.java    # REST endpoints + TodoRequest record
│   ├── service/TodoService.java          # business logic
│   ├── repository/TodoRepository.java    # Spring Data JPA
│   └── model/Todo.java                   # JPA entity (todos table)
├── main/resources/application.properties
└── test/java/com/todoservice/todoservice/
    ├── service/TodoServiceTest.java               # unit (Mockito)
    ├── service/TodoServiceIntegrationTest.java    # integration (Testcontainers)
    ├── service/EndToEndTestingTest.java           # E2E (REST Assured)
    └── TodoserviceApplicationTests.java           # context smoke test
```

## 📖 Lessons Learned (worth reading)

- **Rest Assured + Spring Boot 4 / JDK 25**: versions before 6.x bundle an old Groovy runtime that crashes with a `NullPointerException` inside `ClosureMetaClass`. Rest Assured **6.0.1** (Groovy 5) fixes it.
- **Testcontainers 2.x renames**: modules are now `testcontainers-junit-jupiter` and `testcontainers-postgresql`, and `PostgreSQLContainer` is no longer generic — check the migration guide before copy-pasting older snippets.
- **Why Testcontainers beats mocks for integration tests**: mocked repositories hide mapping and schema issues; a real container surfaces them immediately.
- **E2E catches what unit tests can't**: serialization, status codes, and 404/400 paths only show up over real HTTP.

## 👤 Author

**Raed Zayoud** — [github.com/raedzayoud](https://github.com/raedzayoud)
