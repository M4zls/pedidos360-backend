package com.pedidos360.rabbitmqadmin.controller.dto;

import java.util.List;

/**
 * Cuerpo de todas las respuestas de error del administrador.
 *
 * @param details un elemento por campo invalido (vacio si el error no es de validacion)
 */
public record ErrorResponse(int status, String error, List<String> details) {

    public static ErrorResponse of(int status, String error) {
        return new ErrorResponse(status, error, List.of());
    }
}
