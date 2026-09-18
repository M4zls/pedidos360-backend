package com.pedidos360.auth.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.pedidos360.identity.domain.AppUser;
import com.pedidos360.identity.security.RoleJwtAuthenticationConverter;
import com.pedidos360.identity.service.UserDirectory;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint protegido de prueba: devuelve los datos del usuario autenticado
 * con Microsoft (unico proveedor de login). Incluye `roles` (resuelto por
 * email, ver {@link UserDirectory}) que el frontend usa para mostrar u
 * ocultar secciones.
 */
@RestController
public class MeController {

    private final UserDirectory users;

    public MeController(UserDirectory users) {
        this.users = users;
    }

    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("sub", jwt.getSubject());
        user.put("name", jwt.getClaimAsString("name"));

        String email = RoleJwtAuthenticationConverter.emailOf(jwt);
        user.put("email", email);
        user.put("provider", "microsoft");

        // Se resuelve siempre para poder devolver el rol (fijo por email, ver
        // app.roles) y el estado de consentimiento de datos guardado en AppUser.
        AppUser appUser = users.resolve(email, jwt.getClaimAsString("name"));
        user.put("roles", List.of(appUser.getRole().name()));
        user.put("consentGiven", appUser.isConsentGiven());

        return user;
    }

    /**
     * Registra el consentimiento de proteccion de datos del usuario logueado
     * (que datos se guardan y para que, ver front/consent-modal). Se pide una
     * sola vez; despues /api/me ya devuelve consentGiven=true.
     */
    @PostMapping("/api/me/consent")
    public ResponseEntity<Void> giveConsent(@AuthenticationPrincipal Jwt jwt) {
        String email = RoleJwtAuthenticationConverter.emailOf(jwt);
        users.giveConsent(email);
        return ResponseEntity.noContent().build();
    }
}
