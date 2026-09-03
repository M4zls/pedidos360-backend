package com.pedidos360.inventory.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Registro inmutable de un cambio de stock sobre un {@link Product}. Se crea
 * desde {@code InventoryService} y no se edita ni borra: es el historial.
 */
@Entity
@Table(name = "inventory_stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MovementType type;

    /** Cantidad del movimiento (siempre > 0). El signo lo define {@link #type}. */
    @Column(nullable = false)
    private int quantity;

    /** Stock del producto despues de aplicar este movimiento. */
    @Column(nullable = false)
    private int resultingStock;

    @Column(length = 200)
    private String reason;

    /** "sub" del JWT de quien registro el movimiento. */
    @Column(length = 100)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected StockMovement() {
        // requerido por JPA
    }

    public StockMovement(Product product, MovementType type, int quantity, int resultingStock,
                         String reason, String createdBy) {
        this.product = product;
        this.type = type;
        this.quantity = quantity;
        this.resultingStock = resultingStock;
        this.reason = reason;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public MovementType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getResultingStock() {
        return resultingStock;
    }

    public String getReason() {
        return reason;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
