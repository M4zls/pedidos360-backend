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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Referencia a una cola o exchange que YA existe (origen o destino de un
 * binding, dead-letter-exchange). Admite los {@code amq.*} del broker. Null
 * es valido: combinar con {@code @NotBlank} cuando sea obligatoria.
 */
@Size(min = 1, max = RabbitResourceRef.MAX_LENGTH)
@Pattern(regexp = RabbitResourceRef.CHARS)
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
public @interface RabbitResourceRef {

    /** RabbitMQ limita los nombres a 255 bytes. */
    int MAX_LENGTH = 255;

    String CHARS = "[a-zA-Z0-9_.\\-]+";

    String message() default "nombre invalido: 1-255 caracteres [a-zA-Z0-9_.-]";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
