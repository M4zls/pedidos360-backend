package com.pedidos360.identity.messaging.consumer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/** Valida los DTOs recibidos por los consumidores (misma idea que @Valid en los controllers). */
@Component
public class EventValidator {

    private final Validator validator;

    public EventValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> void validate(T event) {
        Set<ConstraintViolation<T>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new MessageProcessingException("Mensaje invalido: " + details);
        }
    }
}
