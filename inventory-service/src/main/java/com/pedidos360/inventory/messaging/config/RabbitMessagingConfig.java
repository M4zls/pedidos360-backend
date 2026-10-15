package com.pedidos360.inventory.messaging.config;

import java.util.Map;

import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pedidos360.inventory.messaging.event.OrderCreatedEvent;

/**
 * El broker se expone como un servicio: JSON adentro, nunca serializacion
 * Java. orders-service (el productor) manda el payload con un identificador
 * logico ("orderCreatedEvent") en vez del nombre de su propia clase Java —
 * aca lo mapeamos a la clase local equivalente, porque los dos servicios no
 * comparten codigo.
 */
@Configuration
public class RabbitMessagingConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setIdClassMapping(Map.of("orderCreatedEvent", OrderCreatedEvent.class));
        converter.setClassMapper(classMapper);
        return converter;
    }
}
