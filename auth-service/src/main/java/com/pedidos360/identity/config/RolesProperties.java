package com.pedidos360.identity.config;

import java.util.ArrayList;
import java.util.List;

import com.pedidos360.identity.domain.Role;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Asignacion de roles por email (application.yml, {@code app.roles}).
 * Los emails que no figuren en ninguna lista quedan como CLIENTE.
 *
 * <pre>
 * app:
 *   roles:
 *     admins: [pedro.porro&#64;proyecto3602.onmicrosoft.com]
 *     operadores: [juanfaure&#64;proyecto3602.onmicrosoft.com]
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "app.roles")
public class RolesProperties {

    private List<String> admins = new ArrayList<>();
    private List<String> operadores = new ArrayList<>();

    /** Rol configurado para ese email, o CLIENTE si no esta listado. */
    public Role roleFor(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (containsIgnoreCase(admins, normalized)) {
            return Role.ADMIN;
        }
        if (containsIgnoreCase(operadores, normalized)) {
            return Role.OPERADOR;
        }
        return Role.CLIENTE;
    }

    private static boolean containsIgnoreCase(List<String> list, String value) {
        return list.stream().anyMatch(e -> e != null && e.trim().equalsIgnoreCase(value));
    }

    public List<String> getAdmins() {
        return admins;
    }

    public void setAdmins(List<String> admins) {
        this.admins = admins;
    }

    public List<String> getOperadores() {
        return operadores;
    }

    public void setOperadores(List<String> operadores) {
        this.operadores = operadores;
    }
}
