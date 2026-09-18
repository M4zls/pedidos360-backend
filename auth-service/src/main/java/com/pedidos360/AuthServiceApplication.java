package com.pedidos360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de autenticacion y roles. Proceso y base de datos propios
 * (SQLite {@code pedidos360-auth.db}).
 *  - {@code com.pedidos360.auth}     -> resource server (login Microsoft / local).
 *  - {@code com.pedidos360.identity} -> roles por email (fijos, ver app.roles), tabla app_user.
 *
 * El inventario y los pedidos son otros servicios (ver ../inventory-service y
 * ../orders-service), cada uno con su base.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
