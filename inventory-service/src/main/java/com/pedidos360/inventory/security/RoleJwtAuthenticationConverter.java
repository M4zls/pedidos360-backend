package com.pedidos360.inventory.security;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * JWT validado -> authorities de rol. Primero el claim {@code roles} (App Roles
 * de Entra o el que emite el login local); si no viene, resuelve por email con
 * {@link RolesProperties}.
 */
public class RoleJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final RolesProperties rolesConfig;

    public RoleJwtAuthenticationConverter(RolesProperties rolesConfig) {
        this.rolesConfig = rolesConfig;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = emailOf(jwt);

        Set<Role> roles = rolesFromClaim(jwt);
        if (roles.isEmpty()) {
            roles = EnumSet.of(rolesConfig.roleFor(email));
        }

        var authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority(r.authority()))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, email != null ? email : jwt.getSubject());
    }

    static Set<Role> rolesFromClaim(Jwt jwt) {
        List<String> claim = jwt.getClaimAsStringList("roles");
        Set<Role> roles = EnumSet.noneOf(Role.class);
        if (claim == null) {
            return roles;
        }
        for (String value : claim) {
            switch (value.trim().toLowerCase()) {
                case "admin", "administrador" -> roles.add(Role.ADMIN);
                case "operador" -> roles.add(Role.OPERADOR);
                case "cliente" -> roles.add(Role.CLIENTE);
                default -> { }
            }
        }
        return roles;
    }

    static String emailOf(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }
        return email;
    }
}
