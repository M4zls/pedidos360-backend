package com.pedidos360.rabbitmqadmin.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.pedidos360.rabbitmqadmin.broker.BindingDestinationType;
import com.pedidos360.rabbitmqadmin.broker.BindingInfo;
import com.pedidos360.rabbitmqadmin.broker.ExchangeInfo;
import com.pedidos360.rabbitmqadmin.broker.QueueInfo;
import com.pedidos360.rabbitmqadmin.controller.dto.CreateBindingRequest;
import com.pedidos360.rabbitmqadmin.controller.dto.CreateExchangeRequest;
import com.pedidos360.rabbitmqadmin.controller.dto.CreateQueueRequest;
import com.pedidos360.rabbitmqadmin.controller.validation.RabbitResourceName;
import com.pedidos360.rabbitmqadmin.controller.validation.RabbitResourceRef;
import com.pedidos360.rabbitmqadmin.service.RabbitAdminService;

/**
 * Administracion de topologia de RabbitMQ: colas, exchanges y bindings.
 * Solo traduce HTTP y valida la entrada; la logica esta en
 * {@link RabbitAdminService}. Solo ADMIN puede tocar esto (ver SecurityConfig).
 *
 * <pre>
 * GET    /exchanges            GET    /queues            GET    /bindings
 * GET    /exchanges/{name}     GET    /queues/{name}     POST   /bindings
 * POST   /exchanges            POST   /queues            DELETE /bindings?source=..&amp;destinationType=..&amp;destination=..&amp;routingKey=..
 * DELETE /exchanges/{name}     DELETE /queues/{name}
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/rabbitmq")
public class RabbitMqAdminController {

    private final RabbitAdminService rabbitAdmin;

    public RabbitMqAdminController(RabbitAdminService rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    // ---- exchanges ----

    @GetMapping("/exchanges")
    public List<ExchangeInfo> listExchanges() {
        return rabbitAdmin.listExchanges();
    }

    @GetMapping("/exchanges/{name}")
    public ExchangeInfo getExchange(@PathVariable @RabbitResourceRef String name) {
        return rabbitAdmin.getExchange(name);
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Void> createExchange(@Valid @RequestBody CreateExchangeRequest request) {
        rabbitAdmin.createExchange(request.name(), request.type().wireValue(), request.durable(), request.autoDelete());
        return ResponseEntity.created(locationOf(request.name())).build();
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> deleteExchange(@PathVariable @RabbitResourceName String name) {
        rabbitAdmin.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }

    // ---- colas ----

    @GetMapping("/queues")
    public List<QueueInfo> listQueues() {
        return rabbitAdmin.listQueues();
    }

    @GetMapping("/queues/{name}")
    public QueueInfo getQueue(@PathVariable @RabbitResourceRef String name) {
        return rabbitAdmin.getQueue(name);
    }

    @PostMapping("/queues")
    public ResponseEntity<Void> createQueue(@Valid @RequestBody CreateQueueRequest request) {
        rabbitAdmin.createQueue(
                request.name(), request.durable(), request.autoDelete(),
                request.deadLetterExchange(), request.deadLetterRoutingKey());
        return ResponseEntity.created(locationOf(request.name())).build();
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> deleteQueue(@PathVariable @RabbitResourceName String name) {
        rabbitAdmin.deleteQueue(name);
        return ResponseEntity.noContent().build();
    }

    // ---- bindings ----

    @GetMapping("/bindings")
    public List<BindingInfo> listBindings() {
        return rabbitAdmin.listBindings();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Void> createBinding(@Valid @RequestBody CreateBindingRequest request) {
        rabbitAdmin.createBinding(request.source(), request.destinationType(), request.destination(), request.routingKey());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Por query params: un DELETE con body lo descartan muchos clientes y proxies. */
    @DeleteMapping("/bindings")
    public ResponseEntity<Void> deleteBinding(
            @RequestParam @NotBlank @RabbitResourceRef String source,
            @RequestParam BindingDestinationType destinationType,
            @RequestParam @NotBlank @RabbitResourceRef String destination,
            @RequestParam(required = false) @Size(max = RabbitResourceRef.MAX_LENGTH) String routingKey) {
        rabbitAdmin.deleteBinding(source, destinationType, destination, routingKey);
        return ResponseEntity.noContent().build();
    }

    private static URI locationOf(String name) {
        return ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{name}")
                .buildAndExpand(name)
                .toUri();
    }
}
