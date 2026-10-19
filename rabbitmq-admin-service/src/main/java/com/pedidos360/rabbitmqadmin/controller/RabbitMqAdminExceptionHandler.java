package com.pedidos360.rabbitmqadmin.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.pedidos360.rabbitmqadmin.broker.RabbitMqManagementException;
import com.pedidos360.rabbitmqadmin.controller.dto.ErrorResponse;
import com.pedidos360.rabbitmqadmin.service.InvalidTopologyException;
import com.pedidos360.rabbitmqadmin.service.TopologyConflictException;

@RestControllerAdvice
public class RabbitMqAdminExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqAdminExceptionHandler.class);

    // ---- entrada invalida: 400 con el detalle de cada campo ----

    /** {@code @Valid @RequestBody}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e instanceof FieldError fe
                        ? fe.getField() + ": " + fe.getDefaultMessage()
                        : e.getDefaultMessage())
                .toList();
        return badRequest(details);
    }

    /** Restricciones sobre {@code @PathVariable} / {@code @RequestParam}. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handle(HandlerMethodValidationException ex) {
        List<String> details = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .toList();
        return badRequest(details);
    }

    /** JSON mal formado o un enum desconocido (ej. type: "otro"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handle(HttpMessageNotReadableException ex) {
        return badRequest(List.of("cuerpo invalido: " + ex.getMostSpecificCause().getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentTypeMismatchException ex) {
        return badRequest(List.of(ex.getName() + ": valor invalido '" + ex.getValue() + "'"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handle(MissingServletRequestParameterException ex) {
        return badRequest(List.of(ex.getParameterName() + ": es obligatorio"));
    }

    // ---- reglas de topologia ----

    @ExceptionHandler(InvalidTopologyException.class)
    public ResponseEntity<ErrorResponse> handle(InvalidTopologyException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(400, ex.getMessage()));
    }

    @ExceptionHandler(TopologyConflictException.class)
    public ResponseEntity<ErrorResponse> handle(TopologyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(409, ex.getMessage()));
    }

    // ---- errores del broker ----

    @ExceptionHandler(RabbitMqManagementException.class)
    public ResponseEntity<ErrorResponse> handle(RabbitMqManagementException ex) {
        log.warn("Fallo administrando RabbitMQ: {}", ex.getMessage());
        HttpStatus status = toClientStatus(ex);
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), ex.getMessage()));
    }

    /**
     * 404 y 400 del broker se trasladan tal cual. 406 (PRECONDITION_FAILED:
     * el recurso ya existe con otras propiedades) es un conflicto. El resto
     * (401/403 por credenciales de este servicio, 5xx, broker caido) es un
     * problema del lado nuestro: 502.
     */
    private static HttpStatus toClientStatus(RabbitMqManagementException ex) {
        if (ex.status() == null) {
            return HttpStatus.BAD_GATEWAY;
        }
        return switch (ex.status().value()) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 404 -> HttpStatus.NOT_FOUND;
            case 406 -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }

    private static ResponseEntity<ErrorResponse> badRequest(List<String> details) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "Parametros invalidos", details));
    }
}
