package com.pedidos360.inventory.messaging.consumer;

import java.io.IOException;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.pedidos360.inventory.messaging.event.OrderCreatedEvent;
import com.pedidos360.inventory.messaging.usecase.ReserveStockForOrder;

/**
 * Consume {@code inventory.stock-reserve}: cada pedido nuevo descuenta su
 * stock. Clase separada del caso de uso de negocio ({@link ReserveStockForOrder}):
 * esta solo se encarga del protocolo (ack/nack, logging), no de la regla de
 * inventario.
 */
@Component
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final EventValidator validator;
    private final ReserveStockForOrder reserveStockForOrder;

    public OrderCreatedConsumer(EventValidator validator, ReserveStockForOrder reserveStockForOrder) {
        this.validator = validator;
        this.reserveStockForOrder = reserveStockForOrder;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.stock-reserve.name}")
    public void onMessage(@Payload OrderCreatedEvent event, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            validator.validate(event);
            reserveStockForOrder.execute(event);
            log.info("Stock reservado para el pedido #{}", event.orderId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("No se pudo reservar stock para el pedido #{}: {} -> mensaje a DLQ",
                    event != null ? event.orderId() : "?", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
