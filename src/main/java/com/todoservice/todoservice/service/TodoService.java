package com.todoservice.todoservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.todoservice.todoservice.model.Todo;
import com.todoservice.todoservice.repository.TodoRepository;

@Service
public class TodoService {

	private final TodoRepository todoRepository;

	public TodoService(TodoRepository todoRepository) {
		this.todoRepository = todoRepository;
	}

	public List<Todo> findAll() {
		return todoRepository.findAll();
	}

	public Optional<Todo> findById(Long id) {
		return todoRepository.findById(id);
	}

	public Todo create(String title, boolean completed) {
		return todoRepository.save(new Todo(title, completed));
	}

	public Optional<Todo> update(Long id, String title, Boolean completed) {
		Optional<Todo> existing = todoRepository.findById(id);
		if (existing.isEmpty()) {
			return Optional.empty();
		}
		Todo todo = existing.get();
		if (title != null) {
			todo.setTitle(title);
		}
		if (completed != null) {
			todo.setCompleted(completed);
		}
		return Optional.of(todoRepository.save(todo));
	}

	public boolean delete(Long id) {
		if (!todoRepository.existsById(id)) {
			return false;
		}
		todoRepository.deleteById(id);
		return true;
	}
}