package com.pedidos360.analytics.messaging.kafka.topology;

import java.time.Duration;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.StreamJoined;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.WindowStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pedidos360.analytics.messaging.kafka.config.AnalyticsKafkaProperties;
import com.pedidos360.analytics.messaging.kafka.event.InventoryAnalyticsEvent;
import com.pedidos360.analytics.messaging.kafka.event.OrderAnalyticsEvent;
import com.pedidos360.analytics.messaging.kafka.event.OrderFulfillmentEvent;

/**
 * Topologia de Kafka Streams: esto es lo que un {@code @KafkaListener} normal
 * (como los que ya tiene orders/inventory-service para analitica) no puede
 * hacer solo, porque no guarda estado entre mensajes ni cruza dos topicos
 * entre si:
 *
 * <ol>
 *   <li>{@code orderFulfillmentPipeline}: join con ventana de tiempo entre
 *   {@code orders.events} (pedido creado) e {@code inventory.events} (stock
 *   reservado), publicando el cruce a {@code order-fulfillment.events}.</li>
 *   <li>{@code ordersPerMinute}: agregacion (conteo) en ventanas de tiempo,
 *   materializada en un state store consultable por API (ver
 *   {@code AnalyticsQueryController}).</li>
 * </ol>
 *
 * Cada {@code @Bean} que recibe un {@code StreamsBuilder} se registra contra
 * el mismo builder compartido (lo arranca {@code KafkaStreamsAppConfig} via
 * {@code @EnableKafkaStreams}): las dos juntas forman una sola topologia.
 */
@Configuration
public class OrderFulfillmentTopology {

    private static final Logger log = LoggerFactory.getLogger(OrderFulfillmentTopology.class);

    /** Nombre del state store que consulta {@code AnalyticsQueryController}. */
    public static final String ORDERS_PER_MINUTE_STORE = "orders-per-minute-store";

    private final AnalyticsKafkaProperties properties;

    public OrderFulfillmentTopology(AnalyticsKafkaProperties properties) {
        this.properties = properties;
    }

    @Bean
    public KStream<String, OrderFulfillmentEvent> orderFulfillmentPipeline(StreamsBuilder streamsBuilder) {
        ObjectMapper mapper = objectMapper();
        Serde<String> keySerde = Serdes.String();
        Serde<OrderAnalyticsEvent> orderSerde = orderEventSerde(mapper);
        Serde<InventoryAnalyticsEvent> inventorySerde = inventoryEventSerde(mapper);
        Serde<OrderFulfillmentEvent> fulfillmentSerde = fulfillmentEventSerde(mapper);

        // orders.events: orders-service publica con clave = orderId. Solo nos
        // interesa el alta del pedido, no cada cambio de estado posterior.
        KStream<String, OrderAnalyticsEvent> ordersCreated = streamsBuilder
                .stream(properties.topics().ordersEvents().name(), Consumed.with(keySerde, orderSerde))
                .filter((orderId, event) -> "CREATED".equals(event.eventType()));

        // inventory.events: inventory-service publica con clave = productId
        // (ver InventoryEventProducer), no orderId. Para poder cruzarlo con
        // orders.events hace falta reparticionarlo por orderId: selectKey marca
        // el stream como "needs repartition" y Kafka Streams crea solo un
        // topico interno (...-repartition) para reordenar los mensajes por la
        // nueva clave antes del join.
        KStream<String, InventoryAnalyticsEvent> stockReserved = streamsBuilder
                .stream(properties.topics().inventoryEvents().name(), Consumed.with(keySerde, inventorySerde))
                .filter((productId, event) -> "STOCK_RESERVED".equals(event.eventType()))
                .selectKey((productId, event) -> event.orderId().toString());

        // Si el stock se reserva mas tarde que esta ventana despues del alta
        // del pedido, Kafka Streams no los considera el mismo flujo.
        JoinWindows window = JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMillis(properties.joinWindowMs()));

        KStream<String, OrderFulfillmentEvent> fulfillment = ordersCreated.join(
                stockReserved,
                (orderEvent, inventoryEvent) -> new OrderFulfillmentEvent(
                        orderEvent.orderId(),
                        orderEvent.customerEmail(),
                        inventoryEvent.productId(),
                        inventoryEvent.quantity(),
                        orderEvent.occurredAt(),
                        inventoryEvent.occurredAt(),
                        Duration.between(orderEvent.occurredAt(), inventoryEvent.occurredAt()).toMillis()),
                window,
                StreamJoined.with(keySerde, orderSerde, inventorySerde));

        fulfillment.peek((orderId, event) -> log.info(
                "[KafkaStreams] orderId={} productId={} tardo {}ms en reservarse el stock",
                event.orderId(), event.productId(), event.millisToReserve()));

        fulfillment.to(properties.topics().orderFulfillmentEvents().name(),
                Produced.with(keySerde, fulfillmentSerde));

        return fulfillment;
    }

    @Bean
    public KTable<Windowed<String>, Long> ordersPerMinute(StreamsBuilder streamsBuilder) {
        ObjectMapper mapper = objectMapper();
        Serde<String> keySerde = Serdes.String();
        Serde<OrderAnalyticsEvent> orderSerde = orderEventSerde(mapper);

        return streamsBuilder
                .stream(properties.topics().ordersEvents().name(), Consumed.with(keySerde, orderSerde))
                .filter((orderId, event) -> "CREATED".equals(event.eventType()))
                // Agrupa todo bajo una sola clave fija: lo que importa es el conteo
                // por ventana de tiempo, no por pedido individual.
                .groupBy((orderId, event) -> "orders-created", Grouped.with(keySerde, orderSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMillis(properties.ordersPerMinuteWindowMs())))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(ORDERS_PER_MINUTE_STORE)
                        .withKeySerde(keySerde)
                        .withValueSerde(Serdes.Long()));
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // ignoreTypeHeaders(): orders-service e inventory-service publican estos
    // eventos desde SUS propias clases (paquete com.pedidos360.orders.../
    // com.pedidos360.inventory...), distintas a las de este modulo. Si se
    // dejara el header de tipo por defecto, este deserializador intentaria
    // cargar esa clase ajena y fallaria; con esto se ignora el header y se
    // deserializa siempre contra el tipo propio declarado aca.

    private static Serde<OrderAnalyticsEvent> orderEventSerde(ObjectMapper mapper) {
        return new JsonSerde<>(OrderAnalyticsEvent.class, mapper).ignoreTypeHeaders();
    }

    private static Serde<InventoryAnalyticsEvent> inventoryEventSerde(ObjectMapper mapper) {
        return new JsonSerde<>(InventoryAnalyticsEvent.class, mapper).ignoreTypeHeaders();
    }

    private static Serde<OrderFulfillmentEvent> fulfillmentEventSerde(ObjectMapper mapper) {
        return new JsonSerde<>(OrderFulfillmentEvent.class, mapper).ignoreTypeHeaders();
    }
}
