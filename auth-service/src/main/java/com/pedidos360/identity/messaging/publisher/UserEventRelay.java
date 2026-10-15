package com.pedidos360.identity.messaging.publisher;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.pedidos360.identity.messaging.event.UserEvent;

/**
 * UserDirectory emite los UserEvent como eventos de Spring dentro de su
 * transaccion; aca se reenvian al broker recien despues del commit. Asi nunca
 * sale un "user.registered" de un usuario que despues no quedo guardado.
 */
@Component
public class UserEventRelay {

    private final EventPublisher publisher;

    public UserEventRelay(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserEvent(UserEvent event) {
        publisher.publishUserEvent(event);
    }
}
