package com.todoservice.todoservice.service;

import com.todoservice.todoservice.model.Todo;
import com.todoservice.todoservice.repository.TodoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private TodoService todoService;
    @Nested
    class CreateToDoTests {

        @Test
        void shouldCreateSuccessfully() {

            // -----------------------------------------
            // 1. ARRANGE - Prepare the test data
            // -----------------------------------------

            String title = "Clean the house";
            boolean completed = false;

            // Create the Todo that we expect the repository
            // to return after saving it.
            Todo saved = new Todo(title, completed);

            // Simulate the database generating an ID.
            saved.setId(1L);

            // Tell Mockito:
            // "When todoRepository.save() is called with ANY Todo,
            // return the 'saved' Todo object."
            when(todoRepository.save(any(Todo.class)))
                    .thenReturn(saved);


            // -----------------------------------------
            // 2. ACT - Execute the method we are testing
            // -----------------------------------------

            // Call the REAL TodoService.
            Todo result = todoService.create(title, completed);


            // -----------------------------------------
            // 3. ASSERT - Verify the result
            // -----------------------------------------

            // Make sure the result is not null.
            assertNotNull(result);

            // Check that the ID returned by the service is 1.
            assertEquals(1L, result.getId());

            // Check the title.
            assertEquals(title, result.getTitle());

            // Check that completed is false.
            assertFalse(result.isCompleted());


            // -----------------------------------------
            // 4. VERIFY WHAT WAS SENT TO REPOSITORY
            // -----------------------------------------

            // Create a captor that allows us to capture
            // the Todo object passed to repository.save().
            ArgumentCaptor<Todo> captor =
                    ArgumentCaptor.forClass(Todo.class);

            // Verify that repository.save() was called.
            // capture() stores the Todo that was passed to save().
            verify(todoRepository).save(captor.capture());

            // Check the Todo that was actually sent
            // to the repository.
            assertEquals(title, captor.getValue().getTitle());

            assertFalse(captor.getValue().isCompleted());
        }


        @Test
        void shouldCreateCompletedTodo() {

            String title = "Pay the bills";
            boolean completed = true;

            // This represents what the repository will return.
            Todo saved = new Todo(title, completed);
            saved.setId(2L);

            // Mock repository behavior.
            when(todoRepository.save(any(Todo.class)))
                    .thenReturn(saved);

            // Call the service.
            Todo result = todoService.create(title, completed);

            // Verify the returned Todo.
            assertNotNull(result);
            assertEquals(2L, result.getId());
            assertEquals(title, result.getTitle());

            // Since we created it with completed = true,
            // the result should also be true.
            assertTrue(result.isCompleted());
        }


        @Test
        void shouldPassCorrectValuesToRepository() {

            String title = "Write tests";
            boolean completed = false;

            // Object returned by the mocked repository.
            Todo saved = new Todo(title, completed);
            saved.setId(3L);

            // Configure the mock.
            when(todoRepository.save(any(Todo.class)))
                    .thenReturn(saved);

            // Call the service.
            todoService.create(title, completed);


            // Capture the Todo sent to the repository.
            ArgumentCaptor<Todo> captor =
                    ArgumentCaptor.forClass(Todo.class);

            // Verify save() was called and capture its argument.
            verify(todoRepository).save(captor.capture());


            // The service should NOT manually assign an ID.
            // The database/repository normally generates the ID.
            assertNull(captor.getValue().getId());

            // Verify the title sent to the repository.
            assertEquals(title, captor.getValue().getTitle());

            // Verify the completed value sent to the repository.
            assertFalse(captor.getValue().isCompleted());
        }
    }

    @Nested
    class FindAllTests {

        @Test
        void shouldReturnAllTodos() {

            // The repository returns a list of 2 todos.
            Todo todo1 = new Todo("Buy milk", false);
            todo1.setId(1L);
            Todo todo2 = new Todo("Do homework", true);
            todo2.setId(2L);
            List<Todo> todos = new ArrayList<>(List.of(todo1, todo2));

            when(todoRepository.findAll()).thenReturn(todos);

            List<Todo> result = todoService.findAll();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("Buy milk", result.get(0).getTitle());
            assertEquals("Do homework", result.get(1).getTitle());

            verify(todoRepository).findAll();
        }

        @Test
        void shouldReturnEmptyListWhenNoTodos() {

            when(todoRepository.findAll()).thenReturn(new ArrayList<>());

            List<Todo> result = todoService.findAll();

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(todoRepository).findAll();
        }
    }

    @Nested
    class FindByIdTests {

        @Test
        void shouldReturnTodoWhenFound() {

            Todo todo = new Todo("Buy milk", false);
            todo.setId(1L);

            when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));

            Optional<Todo> result = todoService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals(1L, result.get().getId());
            assertEquals("Buy milk", result.get().getTitle());

            verify(todoRepository).findById(1L);
        }

        @Test
        void shouldReturnEmptyWhenNotFound() {

            when(todoRepository.findById(99L)).thenReturn(Optional.empty());

            Optional<Todo> result = todoService.findById(99L);

            assertTrue(result.isEmpty());

            verify(todoRepository).findById(99L);
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void shouldUpdateTitleAndCompleted() {

            Todo existing = new Todo("Old title", false);
            existing.setId(1L);

            when(todoRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(todoRepository.save(any(Todo.class))).thenReturn(existing);

            Optional<Todo> result = todoService.update(1L, "New title", true);

            assertTrue(result.isPresent());
            assertEquals("New title", result.get().getTitle());
            assertTrue(result.get().isCompleted());

            // Capture the Todo sent to save() to make sure it was updated.
            ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
            verify(todoRepository).save(captor.capture());
            assertEquals(1L, captor.getValue().getId());
            assertEquals("New title", captor.getValue().getTitle());
            assertTrue(captor.getValue().isCompleted());
        }

        @Test
        void shouldUpdateTitleOnlyWhenCompletedIsNull() {

            Todo existing = new Todo("Old title", false);
            existing.setId(1L);

            when(todoRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(todoRepository.save(any(Todo.class))).thenReturn(existing);

            Optional<Todo> result = todoService.update(1L, "New title", null);

            assertTrue(result.isPresent());
            assertEquals("New title", result.get().getTitle());
            assertFalse(result.get().isCompleted());

            verify(todoRepository).save(any(Todo.class));
        }

        @Test
        void shouldUpdateCompletedOnlyWhenTitleIsNull() {

            Todo existing = new Todo("Keep this title", false);
            existing.setId(1L);

            when(todoRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(todoRepository.save(any(Todo.class))).thenReturn(existing);

            Optional<Todo> result = todoService.update(1L, null, true);

            assertTrue(result.isPresent());
            assertEquals("Keep this title", result.get().getTitle());
            assertTrue(result.get().isCompleted());

            verify(todoRepository).save(any(Todo.class));
        }

        @Test
        void shouldReturnEmptyWhenTodoDoesNotExist() {

            when(todoRepository.findById(99L)).thenReturn(Optional.empty());

            Optional<Todo> result = todoService.update(99L, "New title", true);

            assertTrue(result.isEmpty());

            // Nothing should be saved if the todo does not exist.
            verify(todoRepository, never()).save(any(Todo.class));
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void shouldDeleteSuccessfully() {

            when(todoRepository.existsById(1L)).thenReturn(true);

            boolean result = todoService.delete(1L);

            assertTrue(result);
            verify(todoRepository).deleteById(1L);
        }

        @Test
        void shouldReturnFalseWhenTodoDoesNotExist() {

            when(todoRepository.existsById(99L)).thenReturn(false);

            boolean result = todoService.delete(99L);

            assertFalse(result);
            // deleteById() must never be called for a missing todo.
            verify(todoRepository, never()).deleteById(eq(99L));
        }
    }

}