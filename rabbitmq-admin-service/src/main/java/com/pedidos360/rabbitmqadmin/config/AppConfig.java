package com.pedidos360.rabbitmqadmin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RabbitMqManagementProperties.class)
public class AppConfig {

    @Bean
    public RestClient rabbitMqManagementRestClient(RabbitMqManagementProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBasicAuth(properties.username(), properties.password());
                    return execution.execute(request, body);
                })
                .build();
    }
}
