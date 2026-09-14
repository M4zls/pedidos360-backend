package com.pedidos360.orders.client;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente HTTP al servicio de inventario. Pedidos no comparte base ni
 * repositorios con inventario: le pregunta por el producto reenviando el
 * Bearer token del usuario.
 */
@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient inventoryRestClient) {
        this.restClient = inventoryRestClient;
    }

    /**
     * Trae un producto por id. {@code Optional.empty()} si el inventario
     * responde 404. Lanza {@link InventoryUnavailableException} ante cualquier
     * otro fallo (red, 5xx, timeout).
     */
    public Optional<ProductView> findProduct(long productId, String bearerToken) {
        try {
            ProductView product = restClient.get()
                    .uri("/api/inventory/products/{id}", productId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(ProductView.class);
            return Optional.ofNullable(product);
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        } catch (RestClientException ex) {
            throw new InventoryUnavailableException(ex);
        }
    }
}
