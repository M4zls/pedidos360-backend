package com.pedidos360.kafkaadmin.controller.validation;

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
 * Nombre de un topico que se va a CREAR o BORRAR: obligatorio, hasta 249
 * caracteres (limite de Kafka), solo {@code [a-zA-Z0-9._-]} y sin el prefijo
 * {@code __}, que el broker reserva para sus topicos internos (ej.
 * {@code __consumer_offsets}).
 */
@NotBlank
@Size(max = KafkaTopicName.MAX_LENGTH)
@Pattern(regexp = "^(?!__)" + KafkaTopicName.CHARS + "$")
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
public @interface KafkaTopicName {

    /** Kafka limita los nombres de topico a 249 caracteres. */
    int MAX_LENGTH = 249;

    String CHARS = "[a-zA-Z0-9._\\-]+";

    String message() default "nombre invalido: 1-249 caracteres [a-zA-Z0-9._-], sin el prefijo reservado '__'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
