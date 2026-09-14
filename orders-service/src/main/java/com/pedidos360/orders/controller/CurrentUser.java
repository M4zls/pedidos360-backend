package com.pedidos360.orders.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Helpers sobre la autenticacion: email (name del token), nombre y si es staff. */
final class CurrentUser {

    private CurrentUser() {}

    static String email(Authentication auth) {
        return auth.getName();
    }

    /** Nombre para mostrar (claim "name" del JWT); si no viene, el email. */
    static String displayName(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt) {
            String name = jwt.getToken().getClaimAsString("name");
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return auth.getName();
    }

    static boolean isStaff(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_OPERADOR"));
    }
}
