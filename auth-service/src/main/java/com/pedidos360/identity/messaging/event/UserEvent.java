package com.pedidos360.identity.messaging.event;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Se publica al exchange topic {@code users.topic}.
 *
 * @param eventType    {@code registered}, {@code role-changed} o {@code consent-given}
 * @param role         rol actual del usuario (el que va en la routing key)
 * @param previousRole solo en {@code role-changed}; null en el resto
 */
public record UserEvent(
        @NotBlank String eventType,
        @NotBlank String email,
        String name,
        @NotBlank String role,
        String previousRole,
        @NotNull Instant occurredAt) {
}
