package com.pedidos360.identity.security;

import java.util.EnumSet;
import java.util.Set;

import com.pedidos360.identity.domain.Role;
import com.pedidos360.identity.service.UserDirectory;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Convierte un JWT de Microsoft ya validado en una autenticacion con la
 * authority de su rol ({@code ROLE_ADMIN} / {@code ROLE_OPERADOR} /
 * {@code ROLE_CLIENTE}). El rol se resuelve SIEMPRE por email contra
 * {@link UserDirectory} (config {@code app.roles} / tabla {@code app_user}) —
 * el token no trae ningun claim de rol propio: el login es solo con
 * Microsoft y el rol de cada cuenta queda fijo por email.
 */
public class RoleJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserDirectory directory;

    public RoleJwtAuthenticationConverter(UserDirectory directory) {
        this.directory = directory;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = emailOf(jwt);
        Role role = directory.resolve(email, jwt.getClaimAsString("name")).getRole();
        Set<Role> roles = EnumSet.of(role);

        var authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority(r.authority()))
                .toList();
        String principal = email != null ? email : jwt.getSubject();
        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    /** Microsoft trae el email en "email" o, si no, en "preferred_username". */
    public static String emailOf(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }
        return email;
    }
}
