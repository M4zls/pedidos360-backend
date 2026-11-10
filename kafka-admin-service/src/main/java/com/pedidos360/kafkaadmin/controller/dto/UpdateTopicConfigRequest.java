package com.pedidos360.kafkaadmin.controller.dto;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

/** Modifica (set) uno o mas parametros de configuracion de un topico existente. */
public record UpdateTopicConfigRequest(@NotEmpty Map<String, String> config) {
}
