package com.pedidos360.orders.messaging;

/**
 * Nombres de exchanges, colas y routing keys del dominio de pedidos.
 * Configuracion centralizada: todo el codigo de mensajeria referencia estas
 * constantes, nunca un literal suelto.
 */
public final class RabbitTopology {

    private RabbitTopology() {
    }

    /** Exchange direct: eventos puntuales del ciclo de vida de un pedido. */
    public static final String ORDERS_DIRECT_EXCHANGE = "orders.direct";
    /** Routing key del exchange direct: se publica una vez, al crear el pedido. */
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    /** Exchange topic: cada cambio de estado de un pedido, para quien se quiera suscribir. */
    public static final String ORDERS_STATUS_TOPIC_EXCHANGE = "orders.status.topic";

    /** Cola que consume el propio orders-service para avisarle al cliente. */
    public static final String ORDERS_NOTIFICATIONS_QUEUE = "orders.notifications";
    public static final String ORDERS_NOTIFICATIONS_DLX = "orders.notifications.dlx";
    public static final String ORDERS_NOTIFICATIONS_DLQ = "orders.notifications.dlq";

    /** Cola que consume el propio orders-service para avisarle a despacho. */
    public static final String DISPATCH_READY_QUEUE = "dispatch.ready";
    public static final String DISPATCH_READY_DLX = "dispatch.ready.dlx";
    public static final String DISPATCH_READY_DLQ = "dispatch.ready.dlq";

    /** Hitos de estado que le importan al cliente (coincide con OrderService.NOTIFIABLE_LABELS). */
    public static String statusRoutingKey(String statusName) {
        return "order.status." + statusName.toLowerCase();
    }
}
