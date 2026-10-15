package com.pedidos360.rabbitmqadmin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pedidos360.rabbitmqadmin.broker.RabbitMqManagementException;

@RestControllerAdvice
public class RabbitMqAdminExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqAdminExceptionHandler.class);

    @ExceptionHandler(RabbitMqManagementException.class)
    public ResponseEntity<String> handle(RabbitMqManagementException ex) {
        log.warn("Fallo administrando RabbitMQ: {}", ex.getMessage());
        HttpStatus status = ex.status() != null
                ? HttpStatus.valueOf(ex.status().value())
                : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(ex.getMessage());
    }
}
