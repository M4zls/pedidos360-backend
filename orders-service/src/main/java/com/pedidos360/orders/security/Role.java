package com.pedidos360.orders.security;

/** Roles de la aplicacion (mismos nombres que el servicio de auth). */
public enum Role {
    ADMIN,
    OPERADOR,
    CLIENTE;

    public String authority() {
        return "ROLE_" + name();
    }
}
