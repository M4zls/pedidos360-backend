package com.pedidos360.identity.service;

import java.util.List;

import com.pedidos360.identity.config.RolesProperties;
import com.pedidos360.identity.domain.AppUser;
import com.pedidos360.identity.domain.Role;
import com.pedidos360.identity.repository.AppUserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Fuente de verdad de "que rol tiene este email". Se consulta en cada request
 * (al convertir el JWT en authorities) y desde el panel de administracion.
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
     * que indique la config (o CLIENTE). Si ya existe y llega un nombre nuevo,
     * lo actualiza.
     */
    public AppUser resolve(String email, String name) {
        String normalized = normalize(email);
        AppUser user = users.findByEmailIgnoreCase(normalized)
                .orElseGet(() -> users.save(new AppUser(normalized, name, rolesConfig.roleFor(normalized))));
        if (StringUtils.hasText(name) && !name.equals(user.getName())) {
            user.setName(name);
        }
        return user;
    }

    @Transactional(readOnly = true)
    public List<AppUser> list() {
        return users.findAllByOrderByEmailAsc();
    }

    public AppUser updateRole(Long id, Role role) {
        AppUser user = users.findById(id).orElseThrow(() -> new AppUserNotFoundException(id));
        user.setRole(role);
        return user;
    }

    private static String normalize(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("El token no trae email");
        }
        return email.trim().toLowerCase();
    }
}
