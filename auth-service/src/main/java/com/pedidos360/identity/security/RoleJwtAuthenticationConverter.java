package com.pedidos360.identity.security;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.pedidos360.identity.domain.Role;
import com.pedidos360.identity.service.UserDirectory;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Convierte un JWT ya validado en una autenticacion con las authorities de sus
 * roles ({@code ROLE_ADMIN} / {@code ROLE_OPERADOR} / {@code ROLE_CLIENTE}).
 *
 * <p>Fuente del rol, en orden:
 * <ol>
 *   <li>Claim <b>{@code roles}</b> del token: son los <i>App Roles</i>
 *       asignados en Entra ID (App Registration → App roles + Enterprise
 *       application → Users and groups). Un usuario puede tener varios.</li>
 *   <li>Si el token no trae {@code roles} (login local de ejemplo, o una
 *       cuenta sin App Roles asignados): se resuelve por email contra
 *       {@link UserDirectory} (config {@code app.roles} / tabla {@code app_user}).</li>
 * </ol>
 */
public class RoleJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserDirectory directory;

    public RoleJwtAuthenticationConverter(UserDirectory directory) {
        this.directory = directory;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = emailOf(jwt);

        Set<Role> roles = rolesFromClaim(jwt);
        if (roles.isEmpty()) {
            roles = EnumSet.of(directory.resolve(email, jwt.getClaimAsString("name")).getRole());
        }

        var authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority(r.authority()))
                .toList();
        String principal = email != null ? email : jwt.getSubject();
        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    /** App Roles de Entra: claim "roles" (array de los "Value" de cada rol). */
    public static Set<Role> rolesFromClaim(Jwt jwt) {
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
                default -> { /* ignora roles desconocidos */ }
            }
        }
        return roles;
    }

    /** Google usa "email"; Microsoft normalmente "preferred_username". */
    public static String emailOf(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }
        return email;
    }
}
