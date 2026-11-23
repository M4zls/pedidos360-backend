package com.pedidos360.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de analitica. No tiene base propia: su unico estado es el que
 * mantiene Kafka Streams (RocksDB local, reconstruible desde los topicos de
 * entrada). Ver {@code messaging.kafka.topology.OrderFulfillmentTopology}.
 */
@SpringBootApplication
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
