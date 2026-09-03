package com.pedidos360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend de Pedidos360. Un solo proceso Spring Boot con
 * dos features separadas por paquete:
 *  - {@code com.pedidos360.auth}      -> resource server (login Microsoft / local).
 *  - {@code com.pedidos360.inventory} -> microservicio de inventario (JPA / Postgres).
 *
 * El @SpringBootApplication en el paquete raiz hace que el component-scan,
 * el @EntityScan y el @EnableJpaRepositories tomen ambos subpaquetes.
 */
@SpringBootApplication
public class Pedidos360Application {

    public static void main(String[] args) {
        SpringApplication.run(Pedidos360Application.class, args);
    }
}
