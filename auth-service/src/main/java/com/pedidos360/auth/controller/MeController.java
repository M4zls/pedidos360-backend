package com.pedidos360.auth.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pedidos360.identity.domain.Role;
import com.pedidos360.identity.security.RoleJwtAuthenticationConverter;
import com.pedidos360.identity.service.UserDirectory;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint protegido de prueba: devuelve los datos del usuario autenticado,
 * ya sea que haya iniciado sesion con Google, con Microsoft o con el login
 * de usuario/contraseña de ejemplo. Incluye `roles` (App Roles de Entra o,
 * si el token no los trae, el rol resuelto por email) que el frontend usa
 * para mostrar u ocultar secciones.
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

        // La foto de perfil solo viene en el token de Google; Microsoft y el
        // login local no la incluyen.
        user.put("picture", jwt.getClaimAsString("picture"));

        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
        String provider;
        if (issuer.contains("google")) {
            provider = "google";
        } else if (issuer.contains("pedidos360-auth")) {
            provider = "local";
        } else {
            provider = "microsoft";
        }
        user.put("provider", provider);

        Set<Role> roles = RoleJwtAuthenticationConverter.rolesFromClaim(jwt);
        if (roles.isEmpty()) {
            roles = Set.of(users.resolve(email, jwt.getClaimAsString("name")).getRole());
        }
        List<String> roleNames = roles.stream().map(Role::name).sorted().toList();
        user.put("roles", roleNames);

        return user;
    }
}
