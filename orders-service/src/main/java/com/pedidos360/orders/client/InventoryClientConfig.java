package com.pedidos360.orders.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClient} apuntando al servicio de inventario. La URL base sale de
 * {@code inventory.base-url} (env {@code INVENTORY_BASE_URL}).
 */
@Configuration
public class InventoryClientConfig {

    @Bean
    RestClient inventoryRestClient(@Value("${inventory.base-url:http://localhost:8080}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
