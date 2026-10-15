package com.pedidos360.identity.service;

import com.pedidos360.identity.domain.AppUser;
import com.pedidos360.identity.domain.Role;
import com.pedidos360.identity.repository.AppUserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Guarda el usuario (nombre, consentimiento) y mantiene una copia local del
 * rol que trae el token en cada login. El rol en si lo decide Azure (App
 * Roles): esta clase no lo calcula, solo lo persiste para poder mostrarlo
 * (ej. en /api/me) sin volver a parsear el JWT.
 */
@Service
@Transactional
public class UserDirectory {

    private final AppUserRepository users;

    public UserDirectory(AppUserRepository users) {
        this.users = users;
    }

    /**
     * Devuelve el usuario para ese email, creandolo la primera vez con el rol
     * que trae el token en este login. Si ya existe: actualiza el nombre si
     * cambio, y RESINCRONIZA el rol contra el que trae el token ahora — asi,
     * si cambia la App Role asignada en Azure, se aplica en el proximo login.
     */
    public AppUser resolve(String email, String name, Role role) {
        String normalized = normalize(email);
        AppUser user = users.findByEmailIgnoreCase(normalized)
                .orElseGet(() -> users.save(new AppUser(normalized, name, role)));
        if (StringUtils.hasText(name) && !name.equals(user.getName())) {
            user.setName(name);
        }
        if (user.getRole() != role) {
            user.syncRole(role);
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
