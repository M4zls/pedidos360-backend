package com.pedidos360.identity;

import com.pedidos360.identity.config.RolesProperties;
import com.pedidos360.identity.service.UserDirectory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Deja creados los usuarios con rol configurado ({@code app.roles}) al
 * arrancar, para que el rol quede resuelto en la base desde el primer login.
 * El resto de los usuarios se crea solo al primer login (CLIENTE). El rol es
 * fijo por email (ver {@code app.roles} en application.yml): no hay endpoint
 * para cambiarlo en runtime.
 */
@Component
class RolesDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RolesDataInitializer.class);

    private final RolesProperties rolesConfig;
    private final UserDirectory directory;

    RolesDataInitializer(RolesProperties rolesConfig, UserDirectory directory) {
        this.rolesConfig = rolesConfig;
        this.directory = directory;
    }

    @Override
    public void run(String... args) {
        rolesConfig.getAdmins().forEach(this::seed);
        rolesConfig.getOperadores().forEach(this::seed);
    }

    private void seed(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        directory.resolve(email.trim().toLowerCase(), null);
        log.info("Roles: usuario preconfigurado {}", email);
    }
}
