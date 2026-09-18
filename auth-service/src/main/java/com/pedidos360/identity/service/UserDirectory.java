package com.pedidos360.identity.service;

import com.pedidos360.identity.config.RolesProperties;
import com.pedidos360.identity.domain.AppUser;
import com.pedidos360.identity.domain.Role;
import com.pedidos360.identity.repository.AppUserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Fuente de verdad de "que rol tiene este email". Se consulta en cada request
 * al convertir el JWT en authorities. El rol es fijo por email (ver
 * {@code app.roles} en application.yml): no hay forma de cambiarlo en runtime.
 */
@Service
@Transactional
public class UserDirectory {

    private final AppUserRepository users;
    private final RolesProperties rolesConfig;

    public UserDirectory(AppUserRepository users, RolesProperties rolesConfig) {
        this.users = users;
        this.rolesConfig = rolesConfig;
    }

    /**
     * Devuelve el usuario para ese email, creandolo la primera vez con el rol
     * que indique la config (o CLIENTE). Si ya existe: actualiza el nombre si
     * cambio, y RESINCRONIZA el rol contra {@code app.roles} — asi, si se
     * agrega/saca un email de esas listas, el cambio se aplica en el proximo
     * login sin quedar pegado al rol con el que se creo la fila la primera vez.
     */
    public AppUser resolve(String email, String name) {
        String normalized = normalize(email);
        Role expectedRole = rolesConfig.roleFor(normalized);
        AppUser user = users.findByEmailIgnoreCase(normalized)
                .orElseGet(() -> users.save(new AppUser(normalized, name, expectedRole)));
        if (StringUtils.hasText(name) && !name.equals(user.getName())) {
            user.setName(name);
        }
        if (user.getRole() != expectedRole) {
            user.syncRole(expectedRole);
        }
        return user;
    }

    /** Registra que el usuario acepto guardar sus datos (proteccion de datos). */
    public AppUser giveConsent(String email) {
        AppUser user = users.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + email));
        user.giveConsent();
        return user;
    }

    private static String normalize(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("El token no trae email");
        }
        return email.trim().toLowerCase();
    }
}
