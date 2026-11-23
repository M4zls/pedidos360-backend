package com.pedidos360.analytics.messaging.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

/**
 * Arranque del motor de Kafka Streams. {@code @EnableKafkaStreams} busca un
 * bean con exactamente este nombre ({@code defaultKafkaStreamsConfig}) y lo
 * usa para levantar el {@code StreamsBuilderFactoryBean} que construye la
 * topologia declarada en {@code OrderFulfillmentTopology} y la arranca junto
 * con el contexto de Spring.
 */
@Configuration
@EnableKafkaStreams
public class KafkaStreamsAppConfig {

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfiguration(AnalyticsKafkaProperties properties) {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "pedidos360-analytics");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");
        // AT_LEAST_ONCE alcanza para analitica/auditoria (puede haber algun
        // reproceso duplicado tras una falla); no hace falta exactly-once.
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.AT_LEAST_ONCE);
        return new KafkaStreamsConfiguration(props);
    }
}
