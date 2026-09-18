package com.pedidos360.inventory.security;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * JWT de Microsoft validado -> authorities de rol. El rol se resuelve SIEMPRE
 * por email con {@link RolesProperties} — el login es solo con Microsoft y el
 * token no trae ningun claim de rol propio.
 */
public class RoleJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final RolesProperties rolesConfig;

    public RoleJwtAuthenticationConverter(RolesProperties rolesConfig) {
        this.rolesConfig = rolesConfig;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = emailOf(jwt);
        Set<Role> roles = EnumSet.of(rolesConfig.roleFor(email));

        var authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority(r.authority()))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, email != null ? email : jwt.getSubject());
    }

    static String emailOf(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }
        return email;
    }
}
