package com.pedidos360.auth.config;

import java.util.List;

import com.pedidos360.auth.security.MultiIssuerAuthenticationManagerResolver;
import com.pedidos360.identity.security.RoleJwtAuthenticationConverter;
import com.pedidos360.identity.service.UserDirectory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Resource server: unico proveedor de login es Microsoft (Entra External ID /
 * CIAM) - ver {@link MultiIssuerAuthenticationManagerResolver}. El Client ID
 * se completa en application.yml (app.auth.microsoft.client-id).
 *
 * El rol (ADMIN / OPERADOR / CLIENTE) se resuelve por email en
 * {@link RoleJwtAuthenticationConverter} y queda como authority ROLE_*, que
 * usan las reglas de abajo y los {@code @PreAuthorize} de los controllers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.auth.microsoft.client-id}")
    private String microsoftClientId;

    private final UserDirectory userDirectory;

    public SecurityConfig(UserDirectory userDirectory) {
        this.userDirectory = userDirectory;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Inventario: leer lo puede cualquier usuario logueado;
                        // modificar (alta/edicion/baja/movimientos) solo staff.
                        .requestMatchers(HttpMethod.GET, "/api/inventory/**").authenticated()
                        .requestMatchers("/api/inventory/**").hasAnyRole("ADMIN", "OPERADOR")

                        // Pedidos son otro servicio (orders-service, base y
                        // proceso propios). Cualquier otra ruta: sesion valida.
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(
                        new MultiIssuerAuthenticationManagerResolver(
                                microsoftClientId,
                                new RoleJwtAuthenticationConverter(userDirectory)
                        )
                ));
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Origen del frontend Angular (ng serve).
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
