package com.pedidos360.auth.controller;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import com.pedidos360.auth.config.LocalAuthProperties;
import com.pedidos360.auth.config.LocalAuthProperties.LocalUser;
import com.pedidos360.auth.security.MultiIssuerAuthenticationManagerResolver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login de USUARIO/CONTRASEÑA de ejemplo, ademas de Microsoft.
 *
 * Valida contra la lista fija de {@link LocalAuthProperties}
 * (app.auth.local.users) y, si coincide, emite un JWT propio firmado con
 * una clave compartida (HS256) que el resto del backend valida igual que un
 * token de Microsoft (ver {@link MultiIssuerAuthenticationManagerResolver}).
 * El rol del usuario viaja en el claim {@code roles}.
 *
 * OJO: es solo un ejemplo. Sin base de datos, sin hash de contraseñas, sin
 * control de intentos fallidos - no usar tal cual en produccion.
 */
@RestController
@RequestMapping("/api/auth")
public class LocalAuthController {

    private final LocalAuthProperties localUsers;

    @Value("${app.auth.local.jwt-secret}")
    private String jwtSecret;

    @Value("${app.auth.local.audience}")
    private String audience;

    public LocalAuthController(LocalAuthProperties localUsers) {
        this.localUsers = localUsers;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        LocalUser user = localUsers.find(request.username(), request.password()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario o contraseña incorrectos"));
        }

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("local-" + user.getUsername())
                .issuer(MultiIssuerAuthenticationManagerResolver.LOCAL_ISSUER)
                .audience(audience)
                .claim("name", user.getName())
                .claim("email", user.getUsername() + "@pedidos360.local")
                .claim("roles", List.of(user.getRole().name()))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .build();

        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            signedJwt.sign(new MACSigner(jwtSecret.getBytes(StandardCharsets.UTF_8)));
        } catch (JOSEException ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo generar el token"));
        }

        return ResponseEntity.ok(Map.of("token", signedJwt.serialize()));
    }

    public record LoginRequest(String username, String password) {}
}
