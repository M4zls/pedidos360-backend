package com.pedidos360.kafkaadmin.broker;

import java.util.Map;

/** Descripcion de un topico: particiones, factor de replicacion y su configuracion efectiva. */
public record TopicInfo(
        String name,
        int partitions,
        short replicationFactor,
        Map<String, String> config) {
}
