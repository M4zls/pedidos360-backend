package com.pedidos360.identity.messaging;

import java.util.Locale;

/**
 * Nombres de exchanges, colas y routing keys del dominio de usuarios.
 * Configuracion centralizada: todo el codigo de mensajeria referencia estas
 * constantes, nunca un literal suelto.
 */
public final class RabbitTopology {

    private RabbitTopology() {
    }

    /**
     * Exchange topic: eventos de usuarios. Routing key {@code user.<evento>.<rol>},
     * asi un consumidor puede filtrar por evento, por rol, o por ambos.
     */
    public static final String USERS_TOPIC_EXCHANGE = "users.topic";

    public static final String USER_REGISTERED = "registered";
    public static final String USER_ROLE_CHANGED = "role-changed";
    public static final String USER_CONSENT_GIVEN = "consent-given";

    /** Cola de auditoria: recibe todos los eventos de usuarios. */
    public static final String USERS_AUDIT_QUEUE = "users.audit";
    public static final String USERS_AUDIT_DLX = "users.audit.dlx";
    public static final String USERS_AUDIT_DLQ = "users.audit.dlq";
    public static final String USERS_AUDIT_BINDING = "user.#";

    /** Cola de alertas: solo eventos cuyo rol resultante es ADMIN. */
    public static final String USERS_ADMIN_ALERTS_QUEUE = "users.admin-alerts";
    public static final String USERS_ADMIN_ALERTS_DLX = "users.admin-alerts.dlx";
    public static final String USERS_ADMIN_ALERTS_DLQ = "users.admin-alerts.dlq";
    public static final String USERS_ADMIN_ALERTS_BINDING = "user.*.admin";

    public static String userRoutingKey(String eventType, String role) {
        return "user." + eventType + "." + role.toLowerCase(Locale.ROOT);
    }
}
