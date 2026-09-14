package com.pedidos360.orders.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import com.pedidos360.orders.client.InventoryUnavailableException;
import com.pedidos360.orders.service.InvalidOrderException;
import com.pedidos360.orders.service.OrderAccessDeniedException;
import com.pedidos360.orders.service.OrderNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores de pedidos -> JSON (RFC 7807). Acotado al paquete web de pedidos. */
@RestControllerAdvice(basePackages = "com.pedidos360.orders.controller")
public class OrderExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleNotFound(OrderNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    public ProblemDetail handleForbidden(OrderAccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ProblemDetail handleUnprocessable(InvalidOrderException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(InventoryUnavailableException.class)
    public ProblemDetail handleInventoryDown(InventoryUnavailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "El servicio de inventario no está disponible. Intentá de nuevo.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "La solicitud tiene campos invalidos");
        problem.setProperty("errors", errors);
        return problem;
    }
}
