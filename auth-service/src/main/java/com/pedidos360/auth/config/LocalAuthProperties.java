package com.pedidos360.auth.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.pedidos360.identity.domain.Role;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Usuarios del login usuario/contraseña de ejemplo ({@code app.auth.local.users}).
 * Lista fija en application.yml, sin base de datos ni hash: solo para la demo.
 * Cada usuario define su rol, que el {@code LocalAuthController} pone en el
 * claim {@code roles} del JWT.
 */
@Component
@ConfigurationProperties(prefix = "app.auth.local")
public class LocalAuthProperties {

    private List<LocalUser> users = new ArrayList<>();

    /** Busca un usuario por credenciales exactas. */
    public Optional<LocalUser> find(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        return users.stream()
                .filter(u -> username.equals(u.getUsername()) && password.equals(u.getPassword()))
                .findFirst();
    }

    public List<LocalUser> getUsers() {
        return users;
    }

    public void setUsers(List<LocalUser> users) {
        this.users = users;
    }

    public static class LocalUser {
        private String username;
        private String password;
        private String name;
        private Role role = Role.CLIENTE;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }
    }
}
