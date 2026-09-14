package com.pedidos360.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jwt.JWTParser;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

/**
 * Permite que el mismo backend acepte, como Bearer token, un ID token de
 * Google, uno de Microsoft (cualquier tenant, incluidas cuentas personales)
 * O un JWT propio emitido por este mismo backend (login de usuario/
 * contraseña de ejemplo, ver LocalAuthController), sin tener que elegir un
 * unico "issuer-uri" fijo en application.yml.
 *
 * Como funciona:
 * 1. Antes de validar la firma, mira el claim "iss" del token (sin verificar
 *    todavia) para decidir de que proveedor viene.
 * 2. Segun el issuer, arma (y cachea) un JwtDecoder que:
 *    - para Google/Microsoft: descarga las claves publicas (JWKS) del
 *      proveedor correspondiente,
 *    - para el login local: verifica la firma con la clave compartida
 *      (HS256) configurada en app.auth.local.jwt-secret,
 *    - en ambos casos exige que el issuer coincida exactamente y que el
 *      audience ("aud") coincida con lo esperado para ese proveedor, ver
 *      {@link AudienceValidator}.
 * 3. Si el issuer no es ninguno de los tres, se rechaza.
 */
public class MultiIssuerAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest> {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    // Los ID tokens de Microsoft (v2.0) tienen issuer
    // "https://login.microsoftonline.com/<tenant-id>/v2.0", donde <tenant-id>
    // varia segun la cuenta (organizacion, o el tenant "consumers" para
    // cuentas personales). Se acepta cualquier tenant.
    private static final Pattern MICROSOFT_ISSUER_PATTERN =
            Pattern.compile("^https://login\\.microsoftonline\\.com/[^/]+/v2\\.0$");

    /** Issuer que usa este mismo backend para el login de usuario/contraseña de ejemplo. */
    public static final String LOCAL_ISSUER = "https://pedidos360-auth.local";

    private final String googleClientId;
    private final String microsoftClientId;
    private final String localJwtSecret;
    private final String localAudience;
    private final Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter;
    private final Map<String, AuthenticationManager> managersByIssuer = new ConcurrentHashMap<>();

    public MultiIssuerAuthenticationManagerResolver(
            String googleClientId,
            String microsoftClientId,
            String localJwtSecret,
            String localAudience,
            Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter
    ) {
        this.googleClientId = googleClientId;
        this.microsoftClientId = microsoftClientId;
        this.localJwtSecret = localJwtSecret;
        this.localAudience = localAudience;
        this.authenticationConverter = authenticationConverter;
    }

    @Override
    public AuthenticationManager resolve(HttpServletRequest request) {
        String issuer = extractIssuer(request);
        if (issuer == null) {
            throw new InvalidBearerTokenException("Token invalido o ausente");
        }
        return managersByIssuer.computeIfAbsent(issuer, this::buildManagerForIssuer);
    }

    private AuthenticationManager buildManagerForIssuer(String issuer) {
        NimbusJwtDecoder jwtDecoder;
        String expectedAudience;

        if (GOOGLE_ISSUER.equals(issuer)) {
            expectedAudience = googleClientId;
            // fromIssuerLocation descarga el documento de descubrimiento OIDC
            // (.well-known/openid-configuration) y las JWKS del proveedor.
            jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuer);
        } else if (MICROSOFT_ISSUER_PATTERN.matcher(issuer).matches()) {
            expectedAudience = microsoftClientId;
            jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuer);
        } else if (LOCAL_ISSUER.equals(issuer)) {
            expectedAudience = localAudience;
            SecretKey key = new SecretKeySpec(localJwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        } else {
            throw new InvalidBearerTokenException("Issuer no confiable: " + issuer);
        }

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(
                withIssuer,
                new AudienceValidator(expectedAudience)
        );
        jwtDecoder.setJwtValidator(withAudience);

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder);
        provider.setJwtAuthenticationConverter(authenticationConverter);
        return new ProviderManager(provider);
    }

    /** Lee el claim "iss" del JWT (parseo sin verificar la firma). */
    private String extractIssuer(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        try {
            return JWTParser.parse(header.substring("Bearer ".length()))
                    .getJWTClaimsSet()
                    .getIssuer();
        } catch (Exception ex) {
            return null;
        }
    }
}
