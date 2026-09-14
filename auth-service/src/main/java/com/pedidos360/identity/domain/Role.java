package com.pedidos360.identity.domain;

/**
 * Roles de la aplicacion. El rol se asigna por email (ver
 * {@code app.roles} en application.yml y {@link com.pedidos360.identity.service.UserDirectory}).
 *
 * <ul>
 *   <li>{@code ADMIN}    - gestiona todo, incluidos los roles de otros usuarios.</li>
 *   <li>{@code OPERADOR} - gestiona inventario y estados de pedidos.</li>
 *   <li>{@code CLIENTE}  - navega el catalogo y hace/ve sus propios pedidos.</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    OPERADOR,
    CLIENTE;

    /** Authority de Spring Security (prefijo ROLE_ para que funcione hasRole(...)). */
    public String authority() {
        return "ROLE_" + name();
    }
}
