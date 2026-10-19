package com.pedidos360.identity.service;

import java.time.Instant;
import java.util.Optional;

import com.pedidos360.identity.domain.AppUser;
import com.pedidos360.identity.domain.Role;
import com.pedidos360.identity.messaging.event.UserEvent;
import com.pedidos360.identity.repository.AppUserRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static com.pedidos360.identity.messaging.event.UserEvent.*;

/**
 * Guarda el usuario (nombre, consentimiento) y mantiene una copia local del
 * rol que trae el token en cada login. El rol en si lo decide Azure (App
 * Roles): esta clase no lo calcula, solo lo persiste para poder mostrarlo
 * (ej. en /api/me) sin volver a parsear el JWT.
 *
 * Cada cambio real (alta, cambio de rol, consentimiento) emite un
 * {@link UserEvent}; {@code UserEventRelay} lo publica en RabbitMQ despues
 * del commit. Un /api/me sin cambios no emite nada.
 */
@Service
@Transactional
public class UserDirectory {

    private final AppUserRepository users;
    private final ApplicationEventPublisher events;

    public UserDirectory(AppUserRepository users, ApplicationEventPublisher events) {
        this.users = users;
        this.events = events;
    }

    /**
     * Devuelve el usuario para ese email, creandolo la primera vez con el rol
     * que trae el token en este login. Si ya existe: actualiza el nombre si
     * cambio, y RESINCRONIZA el rol contra el que trae el token ahora — asi,
     * si cambia la App Role asignada en Azure, se aplica en el proximo login.
     */
    public AppUser resolve(String email, String name, Role role) {
        String normalized = normalize(email);
        Optional<AppUser> existing = users.findByEmailIgnoreCase(normalized);
        if (existing.isEmpty()) {
            AppUser created = users.save(new AppUser(normalized, name, role));
            emit(USER_REGISTERED, created, null);
            return created;
        }
        AppUser user = existing.get();
        if (StringUtils.hasText(name) && !name.equals(user.getName())) {
            user.setName(name);
        }
        if (user.getRole() != role) {
            Role previous = user.getRole();
            user.syncRole(role);
            emit(USER_ROLE_CHANGED, user, previous);
        }
        return user;
    }

    /** Registra que el usuario acepto guardar sus datos (proteccion de datos). */
    public AppUser giveConsent(String email) {
        AppUser user = users.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + email));
        if (!user.isConsentGiven()) {
            user.giveConsent();
            emit(USER_CONSENT_GIVEN, user, null);
        }
        return user;
    }

    private void emit(String eventType, AppUser user, Role previousRole) {
        events.publishEvent(new UserEvent(
                eventType,
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                previousRole != null ? previousRole.name() : null,
                Instant.now()));
    }

    private static String normalize(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("El token no trae email");
        }
        return email.trim().toLowerCase();
    }
}
