package com.pedidos360.kafkaadmin.broker;

import java.util.List;

/**
 * Estado de un consumer group.
 *
 * @param members    uno por consumidor activo conectado al group
 * @param lag        suma del lag de todas sus particiones asignadas (null si no se pudo calcular)
 */
public record ConsumerGroupInfo(
        String groupId,
        String state,
        List<String> members,
        Long lag) {
}
