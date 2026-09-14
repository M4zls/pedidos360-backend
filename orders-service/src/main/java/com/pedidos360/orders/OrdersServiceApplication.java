package com.pedidos360.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de pedidos. Proceso y base de datos propios (SQLite
 * {@code pedidos360-orders.db}). Valida los mismos Bearer tokens que el
 * servicio de auth y consulta el catalogo al servicio de inventario por HTTP.
 */
@SpringBootApplication
public class OrdersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdersServiceApplication.class, args);
    }
}
