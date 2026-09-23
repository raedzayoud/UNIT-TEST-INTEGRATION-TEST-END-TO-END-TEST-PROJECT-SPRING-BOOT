package com.todoservice.todoservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todoservice.todoservice.model.Todo;
import com.todoservice.todoservice.service.TodoService;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

	private final TodoService todoService;

	public TodoController(TodoService todoService) {
		this.todoService = todoService;
	}

	@GetMapping
	public List<Todo> getAll() {
		return todoService.findAll();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Todo> getById(@PathVariable Long id) {
		return todoService.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<Todo> create(@RequestBody TodoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(todoService.create(request.title(), request.completed()));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Todo> update(@PathVariable Long id, @RequestBody TodoRequest request) {
		return todoService.update(id, request.title(), request.completed())
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		return todoService.delete(id)
				? ResponseEntity.noContent().build()
				: ResponseEntity.notFound().build();
	}

	public record TodoRequest(String title, boolean completed) {
	}
}