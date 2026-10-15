package com.pedidos360.orders.security;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * JWT de Microsoft validado -> authorities de rol. El rol sale del claim
 * {@code roles} del token (App Roles asignadas en Azure: App Registration ->
 * App roles, Enterprise Application -> Users and groups). Si la cuenta no
 * tiene ninguna App Role reconocida, se rechaza el login.
 */
public class RoleJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = emailOf(jwt);
        Role role = roleFromClaim(jwt)
                .orElseThrow(() -> new InvalidBearerTokenException(
                        "La cuenta no tiene ninguna App Role asignada en Azure"));
        Set<Role> roles = EnumSet.of(role);

        var authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority(r.authority()))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, email != null ? email : jwt.getSubject());
    }

    static Optional<Role> roleFromClaim(Jwt jwt) {
        List<String> claim = jwt.getClaimAsStringList("roles");
        if (claim == null) {
            return Optional.empty();
        }
        for (String value : claim) {
            switch (value.trim().toLowerCase()) {
                case "admin", "administrador" -> {
                    return Optional.of(Role.ADMIN);
                }
                case "operador" -> {
                    return Optional.of(Role.OPERADOR);
                }
                case "cliente" -> {
                    return Optional.of(Role.CLIENTE);
                }
                default -> {
                    /* ignora valores desconocidos */
                }
            }
        }
        return Optional.empty();
    }

    static String emailOf(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }
        return email;
    }
}
