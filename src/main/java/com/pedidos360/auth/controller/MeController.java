package com.pedidos360.auth.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint protegido de prueba: devuelve los datos del usuario autenticado,
 * ya sea que haya iniciado sesion con Google, con Microsoft o con el login
 * de usuario/contraseña de ejemplo.
 */
@RestController
public class MeController {

    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("sub", jwt.getSubject());
        user.put("name", jwt.getClaimAsString("name"));

        // Google entrega el correo en "email". Microsoft normalmente lo
        // entrega en "preferred_username" (y a veces tambien en "email").
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }
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

        return user;
    }
}
