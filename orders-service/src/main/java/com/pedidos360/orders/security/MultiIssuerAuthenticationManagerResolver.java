package com.pedidos360.orders.security;

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
 * Acepta como Bearer token un ID token de Microsoft (cualquier tenant) o un
 * JWT propio emitido por el login usuario/contraseña del servicio de auth
 * (HS256, misma clave compartida). Igual que en el servicio de auth, pero sin
 * Google.
 */
public class MultiIssuerAuthenticationManagerResolver
        implements AuthenticationManagerResolver<HttpServletRequest> {

    private static final Pattern MICROSOFT_ISSUER_PATTERN =
            Pattern.compile("^https://login\\.microsoftonline\\.com/[^/]+/v2\\.0$");

    /** Issuer que usa el servicio de auth para el login usuario/contraseña. */
    public static final String LOCAL_ISSUER = "https://pedidos360-auth.local";

    private final String microsoftClientId;
    private final String localJwtSecret;
    private final String localAudience;
    private final Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter;
    private final Map<String, AuthenticationManager> managersByIssuer = new ConcurrentHashMap<>();

    public MultiIssuerAuthenticationManagerResolver(
            String microsoftClientId,
            String localJwtSecret,
            String localAudience,
            Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter) {
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

        if (MICROSOFT_ISSUER_PATTERN.matcher(issuer).matches()) {
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
                withIssuer, new AudienceValidator(expectedAudience));
        jwtDecoder.setJwtValidator(withAudience);

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder);
        provider.setJwtAuthenticationConverter(authenticationConverter);
        return new ProviderManager(provider);
    }

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
