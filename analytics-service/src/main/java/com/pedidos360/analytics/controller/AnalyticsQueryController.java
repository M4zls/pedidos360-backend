package com.pedidos360.analytics.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;

import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pedidos360.analytics.messaging.kafka.topology.OrderFulfillmentTopology;

/**
 * Interactive query: lee directamente el state store que mantiene Kafka
 * Streams en este proceso (RocksDB local), sin pasar por ningun topico ni
 * base de datos. Esto es lo que demuestra que el conteo no es un simple log
 * de consumidor: es estado agregado, consultable en cualquier momento.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsQueryController {

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public AnalyticsQueryController(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    @GetMapping("/orders-per-minute")
    public List<WindowCount> ordersPerMinute() {
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        if (streams == null) {
            return List.of();
        }

        ReadOnlyWindowStore<String, Long> store = streams.store(
                StoreQueryParameters.fromNameAndType(
                        OrderFulfillmentTopology.ORDERS_PER_MINUTE_STORE,
                        QueryableStoreTypes.windowStore()));

        Instant from = Instant.now().minus(Duration.ofMinutes(15));
        Instant to = Instant.now();

        List<WindowCount> result = new ArrayList<>();
        try (KeyValueIterator<Windowed<String>, Long> it = store.fetchAll(from, to)) {
            while (it.hasNext()) {
                KeyValue<Windowed<String>, Long> entry = it.next();
                result.add(new WindowCount(
                        entry.key.window().startTime(),
                        entry.key.window().endTime(),
                        entry.value));
            }
        }
        return result;
    }

    public record WindowCount(Instant windowStart, Instant windowEnd, long ordersCreated) {
    }
}
