package com.pedidos360.rabbitmqadmin.controller.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Nombre de una cola o exchange que se va a CREAR o BORRAR: obligatorio, hasta
 * 255 caracteres (limite de RabbitMQ), solo {@code [a-zA-Z0-9_.-]} y sin el
 * prefijo {@code amq.}, que el broker reserva para sus recursos internos.
 */
@NotBlank
@Size(max = RabbitResourceRef.MAX_LENGTH)
@Pattern(regexp = "^(?!amq\\.)" + RabbitResourceRef.CHARS + "$")
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
public @interface RabbitResourceName {

    String message() default "nombre invalido: 1-255 caracteres [a-zA-Z0-9_.-], sin el prefijo reservado 'amq.'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
