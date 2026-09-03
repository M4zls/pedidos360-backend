package com.pedidos360.auth.controller;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import com.pedidos360.auth.security.MultiIssuerAuthenticationManagerResolver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login de USUARIO/CONTRASEÑA de ejemplo, ademas de Google y Microsoft.
 *
 * Usa un unico usuario fijo definido en application.yml
 * (app.auth.local.username / app.auth.local.password) y, si coincide, emite
 * un JWT propio firmado con una clave compartida (HS256) que el resto del
 * backend valida igual que un token de Google o Microsoft (ver
 * {@link MultiIssuerAuthenticationManagerResolver}).
 *
 * OJO: esto es solo un ejemplo. No hay base de datos de usuarios, no hay
 * hash de contraseñas, ni control de intentos fallidos - no usar tal cual
 * en produccion.
 */
@RestController
@RequestMapping("/api/auth")
public class LocalAuthController {

    @Value("${app.auth.local.username}")
    private String demoUsername;

    @Value("${app.auth.local.password}")
    private String demoPassword;

    @Value("${app.auth.local.jwt-secret}")
    private String jwtSecret;

    @Value("${app.auth.local.audience}")
    private String audience;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.username() == null
                || request.password() == null
                || !demoUsername.equals(request.username())
                || !demoPassword.equals(request.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario o contraseña incorrectos"));
        }

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("local-" + demoUsername)
                .issuer(MultiIssuerAuthenticationManagerResolver.LOCAL_ISSUER)
                .audience(audience)
                .claim("name", "Usuario de ejemplo")
                .claim("email", demoUsername + "@pedidos360.local")
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
