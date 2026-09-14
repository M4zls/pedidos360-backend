package com.pedidos360.inventory.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import com.pedidos360.inventory.service.DuplicateSkuException;
import com.pedidos360.inventory.service.InsufficientStockException;
import com.pedidos360.inventory.service.ProductNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce los errores del inventario a respuestas JSON (RFC 7807 / ProblemDetail).
 * Acotado al paquete web del inventario para no pisar el manejo de errores del
 * resource server de auth.
 */
@RestControllerAdvice(basePackages = "com.pedidos360.inventory.controller")
public class InventoryExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(ProductNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateSkuException.class)
    public ProblemDetail handleDuplicate(DuplicateSkuException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({ InsufficientStockException.class, IllegalArgumentException.class })
    public ProblemDetail handleUnprocessable(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
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
