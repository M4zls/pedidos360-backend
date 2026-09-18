package com.pedidos360.orders.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Notificacion in-app para el cliente sobre un hito de su pedido (listo,
 * despachado, entregado, cancelado). Se crea desde {@code OrderService} en la
 * misma transaccion que el cambio de estado; el front la trae por polling.
 */
@Entity
@Table(name = "customer_order_notification")
public class OrderNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String customerEmail;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 200)
    private String message;

    @Column(nullable = false)
    private boolean read = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderNotification() {
        // requerido por JPA
    }

    public OrderNotification(String customerEmail, Long orderId, String message) {
        this.customerEmail = customerEmail;
        this.orderId = orderId;
        this.message = message;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markRead() {
        this.read = true;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
