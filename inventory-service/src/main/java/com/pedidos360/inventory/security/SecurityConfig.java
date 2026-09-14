package com.pedidos360.inventory.security;

import java.util.List;

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
 * Resource server del servicio de inventario. Valida los mismos tokens que el
 * servicio de auth (Microsoft + login local HS256) y arma las authorities de
 * rol. Leer el catalogo requiere sesion; alta/edicion/baja/movimientos
 * requieren ADMIN u OPERADOR.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.auth.microsoft.client-id}")
    private String microsoftClientId;

    @Value("${app.auth.local.jwt-secret}")
    private String localJwtSecret;

    @Value("${app.auth.local.audience}")
    private String localAudience;

    @Value("${app.cors.allowed-origin:http://localhost:4200}")
    private String allowedOrigin;

    private final RolesProperties rolesConfig;

    public SecurityConfig(RolesProperties rolesConfig) {
        this.rolesConfig = rolesConfig;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/inventory/**").authenticated()
                        .requestMatchers("/api/inventory/**").hasAnyRole("ADMIN", "OPERADOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(
                        new MultiIssuerAuthenticationManagerResolver(
                                microsoftClientId, localJwtSecret, localAudience,
                                new RoleJwtAuthenticationConverter(rolesConfig))));
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
