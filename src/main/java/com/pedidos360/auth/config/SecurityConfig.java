package com.pedidos360.auth.config;

import java.util.List;

import com.pedidos360.auth.security.MultiIssuerAuthenticationManagerResolver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Resource server que acepta como Bearer token un ID token de Google, uno de
 * Microsoft, o un JWT propio emitido por POST /api/auth/login (login de
 * usuario/contraseña de ejemplo) - ver
 * {@link MultiIssuerAuthenticationManagerResolver}. Los Client IDs / secreto
 * local se completan en application.yml (app.auth.*).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.auth.google.client-id}")
    private String googleClientId;

    @Value("${app.auth.microsoft.client-id}")
    private String microsoftClientId;

    @Value("${app.auth.local.jwt-secret}")
    private String localJwtSecret;

    @Value("${app.auth.local.audience}")
    private String localAudience;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // El login de usuario/contraseña de ejemplo tiene que ser publico:
                        // todavia no hay token cuando se llama a este endpoint.
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(
                        new MultiIssuerAuthenticationManagerResolver(
                                googleClientId, microsoftClientId, localJwtSecret, localAudience
                        )
                ));
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Origen del frontend Angular (ng serve).
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
