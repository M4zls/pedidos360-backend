package com.pedidos360.auth.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

/**
 * Resource server: unico proveedor de login es Microsoft (Entra External ID /
 * CIAM). Antes de validar la firma, mira el claim "iss" del token para
 * confirmar que es un tenant CIAM valido y arma (y cachea) el JwtDecoder
 * correspondiente (descarga JWKS via el documento de descubrimiento OIDC),
 * exigiendo que el issuer y el audience ("aud") coincidan con lo esperado
 * (ver {@link AudienceValidator}). Cualquier otro issuer se rechaza.
 */
public class MultiIssuerAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest> {

    // El tenant de Microsoft de este proyecto es un tenant Entra External ID
    // (CIAM), no un tenant Entra ID "workforce" clasico: sus ID tokens (v2.0)
    // tienen issuer "https://<tenant-id>.ciamlogin.com/<tenant-id>/v2.0", no
    // "https://login.microsoftonline.com/...". Se acepta cualquier tenant
    // bajo ciamlogin.com.
    private static final Pattern MICROSOFT_ISSUER_PATTERN =
            Pattern.compile("^https://[^./]+\\.ciamlogin\\.com/[^/]+/v2\\.0$");

    private final String microsoftClientId;
    private final Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter;
    private final Map<String, AuthenticationManager> managersByIssuer = new ConcurrentHashMap<>();

    public MultiIssuerAuthenticationManagerResolver(
            String microsoftClientId,
            Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter
    ) {
        this.microsoftClientId = microsoftClientId;
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
        if (!MICROSOFT_ISSUER_PATTERN.matcher(issuer).matches()) {
            throw new InvalidBearerTokenException("Issuer no confiable: " + issuer);
        }

        // fromIssuerLocation descarga el documento de descubrimiento OIDC
        // (.well-known/openid-configuration) y las JWKS del proveedor.
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(
                withIssuer,
                new AudienceValidator(microsoftClientId)
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
