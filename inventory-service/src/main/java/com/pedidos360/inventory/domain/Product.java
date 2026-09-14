package com.pedidos360.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * Producto / insumo del inventario. El stock nunca se modifica "a mano" desde
 * afuera: se registra un {@link StockMovement} y el servicio recalcula
 * {@link #stock}. Asi queda trazabilidad de cada entrada y salida.
 */
@Entity
@Table(
        name = "inventory_product",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_sku", columnNames = "sku")
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Codigo interno del producto. Unico. */
    @Column(nullable = false, length = 40)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 60)
    private String category;

    /** URL de la foto del producto (para la carta). Opcional. */
    @Column(length = 500)
    private String imageUrl;

    /** Unidad de medida: "unidad", "kg", "litro", ... */
    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Stock actual. Derivado de los movimientos; no setear desde el controller. */
    @Column(nullable = false)
    private int stock;

    /** Umbral de reposicion: por debajo de esto el producto aparece en /low-stock. */
    @Column(nullable = false)
    private int minStock;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /** Bloqueo optimista: evita que dos movimientos concurrentes pisen el stock. */
    @Version
    private long version;

    protected Product() {
        // requerido por JPA
    }

    public Product(String sku, String name, String category, String unit, BigDecimal unitPrice, int minStock) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.minStock = minStock;
        this.stock = 0;
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** Aplica un delta al stock. Lo usa solo el servicio de inventario. */
    public void changeStock(int delta) {
        int result = this.stock + delta;
        if (result < 0) {
            throw new IllegalArgumentException("El stock no puede quedar negativo");
        }
        this.stock = result;
    }

    public void setStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        this.stock = stock;
    }

    public boolean isLowStock() {
        return active && stock <= minStock;
    }

    // --- getters / setters de campos editables ---

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getStock() {
        return stock;
    }

    public int getMinStock() {
        return minStock;
    }

    public void setMinStock(int minStock) {
        this.minStock = minStock;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
