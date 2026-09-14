package com.pedidos360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de inventario. Proceso y base de datos propios (SQLite
 * {@code pedidos360-inventory.db}). Valida los mismos Bearer tokens que el
 * servicio de auth de forma independiente (ver {@code inventory.security}).
 */
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
