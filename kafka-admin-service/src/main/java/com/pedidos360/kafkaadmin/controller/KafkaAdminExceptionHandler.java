package com.pedidos360.kafkaadmin.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.pedidos360.kafkaadmin.broker.KafkaAdminException;
import com.pedidos360.kafkaadmin.controller.dto.ErrorResponse;
import com.pedidos360.kafkaadmin.service.TopologyConflictException;

@RestControllerAdvice
public class KafkaAdminExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminExceptionHandler.class);

    // ---- entrada invalida: 400 con el detalle de cada campo ----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e instanceof FieldError fe
                        ? fe.getField() + ": " + fe.getDefaultMessage()
                        : e.getDefaultMessage())
                .toList();
        return badRequest(details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handle(HandlerMethodValidationException ex) {
        List<String> details = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .toList();
        return badRequest(details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handle(HttpMessageNotReadableException ex) {
        return badRequest(List.of("cuerpo invalido: " + ex.getMostSpecificCause().getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentTypeMismatchException ex) {
        return badRequest(List.of(ex.getName() + ": valor invalido '" + ex.getValue() + "'"));
    }

    // ---- reglas de topologia ----

    @ExceptionHandler(TopologyConflictException.class)
    public ResponseEntity<ErrorResponse> handle(TopologyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(409, ex.getMessage()));
    }

    // ---- errores del cluster ----

    @ExceptionHandler(KafkaAdminException.class)
    public ResponseEntity<ErrorResponse> handle(KafkaAdminException ex) {
        log.warn("Fallo administrando Kafka ({}): {}", ex.kind(), ex.getMessage());
        HttpStatus status = toClientStatus(ex.kind());
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), ex.getMessage()));
    }

    private static HttpStatus toClientStatus(KafkaAdminException.Kind kind) {
        return switch (kind) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UPSTREAM -> HttpStatus.BAD_GATEWAY;
        };
    }

    private static ResponseEntity<ErrorResponse> badRequest(List<String> details) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "Parametros invalidos", details));
    }
}
