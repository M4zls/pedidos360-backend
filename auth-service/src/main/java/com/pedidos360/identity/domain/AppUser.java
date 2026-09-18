package com.pedidos360.identity.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Usuario conocido por la app y su rol. Se crea la primera vez que alguien
 * inicia sesion (auto-provisioning en {@link com.pedidos360.identity.service.UserDirectory}):
 * si el email esta en {@code app.roles} toma ese rol, si no queda como CLIENTE.
 * El rol es fijo por email y se resincroniza contra {@code app.roles} en cada
 * login ({@link #syncRole}) — no hay endpoint para cambiarlo "a mano".
 */
@Entity
@Table(
        name = "app_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_app_user_email", columnNames = "email")
)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email normalizado a minusculas. Identidad del usuario entre proveedores. */
    @Column(nullable = false, length = 160)
    private String email;

    @Column(length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Role role;

    /** Si ya acepto guardar sus datos (email/nombre/rol/historial de pedidos). */
    @Column(nullable = false)
    private boolean consentGiven = false;

    @Column
    private Instant consentGivenAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AppUser() {
        // requerido por JPA
    }

    public AppUser(String email, String name, Role role) {
        this.email = email;
        this.name = name;
        this.role = role;
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

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    /**
     * Resincroniza el rol contra {@code app.roles} (llamado desde
     * {@code UserDirectory.resolve} en cada login). No es una API para que
     * alguien "elija" su rol: si cambia la config, esto lo refleja.
     */
    public void syncRole(Role role) {
        this.role = role;
    }

    public boolean isConsentGiven() {
        return consentGiven;
    }

    public Instant getConsentGivenAt() {
        return consentGivenAt;
    }

    public void giveConsent() {
        this.consentGiven = true;
        this.consentGivenAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
