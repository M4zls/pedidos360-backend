package com.pedidos360.identity.messaging.publisher;

import com.pedidos360.identity.messaging.event.UserEvent;

/**
 * Puerto hacia el broker de mensajeria. El resto del codigo depende de esta
 * abstraccion, nunca de RabbitTemplate directamente.
 */
public interface EventPublisher {

    void publishUserEvent(UserEvent event);
}
