package com.pedidos360.rabbitmqadmin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.pedidos360.rabbitmqadmin.controller.dto.CreateBindingRequest;
import com.pedidos360.rabbitmqadmin.controller.dto.CreateExchangeRequest;
import com.pedidos360.rabbitmqadmin.controller.dto.CreateQueueRequest;
import com.pedidos360.rabbitmqadmin.usecase.CreateBindingUseCase;
import com.pedidos360.rabbitmqadmin.usecase.CreateExchangeUseCase;
import com.pedidos360.rabbitmqadmin.usecase.CreateQueueUseCase;
import com.pedidos360.rabbitmqadmin.usecase.DeleteBindingUseCase;
import com.pedidos360.rabbitmqadmin.usecase.DeleteExchangeUseCase;
import com.pedidos360.rabbitmqadmin.usecase.DeleteQueueUseCase;

/**
 * Administracion de topologia de RabbitMQ: colas, exchanges y bindings.
 * Solo ADMIN puede tocar esto (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/rabbitmq")
public class RabbitMqAdminController {

    private final CreateExchangeUseCase createExchange;
    private final DeleteExchangeUseCase deleteExchange;
    private final CreateQueueUseCase createQueue;
    private final DeleteQueueUseCase deleteQueue;
    private final CreateBindingUseCase createBinding;
    private final DeleteBindingUseCase deleteBinding;

    public RabbitMqAdminController(
            CreateExchangeUseCase createExchange,
            DeleteExchangeUseCase deleteExchange,
            CreateQueueUseCase createQueue,
            DeleteQueueUseCase deleteQueue,
            CreateBindingUseCase createBinding,
            DeleteBindingUseCase deleteBinding) {
        this.createExchange = createExchange;
        this.deleteExchange = deleteExchange;
        this.createQueue = createQueue;
        this.deleteQueue = deleteQueue;
        this.createBinding = createBinding;
        this.deleteBinding = deleteBinding;
    }

    @PutMapping("/exchanges")
    public ResponseEntity<Void> createExchange(@Valid @RequestBody CreateExchangeRequest request) {
        createExchange.execute(request.name(), request.type().wireValue(), request.durable(), request.autoDelete());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> deleteExchange(@PathVariable String name) {
        deleteExchange.execute(name);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/queues")
    public ResponseEntity<Void> createQueue(@Valid @RequestBody CreateQueueRequest request) {
        createQueue.execute(
                request.name(), request.durable(), request.autoDelete(),
                request.deadLetterExchange(), request.deadLetterRoutingKey());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> deleteQueue(@PathVariable String name) {
        deleteQueue.execute(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Void> createBinding(@Valid @RequestBody CreateBindingRequest request) {
        createBinding.execute(request.source(), request.destinationType(), request.destination(), request.routingKey());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> deleteBinding(@Valid @RequestBody CreateBindingRequest request) {
        deleteBinding.execute(request.source(), request.destinationType(), request.destination(), request.routingKey());
        return ResponseEntity.noContent().build();
    }
}
