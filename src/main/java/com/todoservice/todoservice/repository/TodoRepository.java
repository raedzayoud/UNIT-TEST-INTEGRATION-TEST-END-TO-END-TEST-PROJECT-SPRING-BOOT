package com.todoservice.todoservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.todoservice.todoservice.model.Todo;

public interface TodoRepository extends JpaRepository<Todo, Long> {
}