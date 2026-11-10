package com.pedidos360.kafkaadmin.config;

import java.util.Map;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KafkaAdminProperties.class)
public class AppConfig {

    @Bean(destroyMethod = "close")
    public Admin kafkaAdminClient(KafkaAdminProperties properties) {
        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers(),
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, properties.requestTimeoutMs());
        return Admin.create(config);
    }
}
