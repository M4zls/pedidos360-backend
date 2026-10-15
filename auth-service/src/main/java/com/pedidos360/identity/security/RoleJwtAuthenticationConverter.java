package com.pedidos360.identity.security;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.pedidos360.identity.domain.Role;
import com.pedidos360.identity.service.UserDirectory;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Convierte un JWT de Microsoft ya validado en una autenticacion con la
 * authority de su rol ({@code ROLE_ADMIN} / {@code ROLE_OPERADOR} /
 * {@code ROLE_CLIENTE}). El rol sale del claim <b>{@code roles}</b> del token
 * (App Roles asignadas en el Portal de Azure: App Registration -> App roles,
 * y Enterprise Application -> Users and groups). Si la cuenta no tiene
 * ninguna App Role reconocida asignada, se RECHAZA el login (no hay rol por
 * defecto): hay que asignarsela en Azure.
 */
public class RoleJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserDirectory directory;

    public RoleJwtAuthenticationConverter(UserDirectory directory) {
        this.directory = directory;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = emailOf(jwt);
        Role role = roleFromClaim(jwt)
                .orElseThrow(() -> new InvalidBearerTokenException(
                        "La cuenta no tiene ninguna App Role asignada en Azure"));

        directory.resolve(email, jwt.getClaimAsString("name"), role);
        Set<Role> roles = EnumSet.of(role);

        var authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority(r.authority()))
                .toList();
        String principal = email != null ? email : jwt.getSubject();
        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    /**
     * App Roles de Entra: claim "roles" (array de los "Value" de cada rol
     * asignado). Usa la primera que reconozca; ignora valores desconocidos.
     */
    public static Optional<Role> roleFromClaim(Jwt jwt) {
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

    /** Microsoft trae el email en "email" o, si no, en "preferred_username". */
    public static String emailOf(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }
        return email;
    }
}
