package com.kaishui.entitlement.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecurityException; // Specific exception for key issues
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value; // For injecting the key
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component; // Keep as Component for injection
import org.springframework.web.server.ServerWebExchange;
import reactor.util.context.ContextView;

import jakarta.annotation.PostConstruct; // For initializing the key
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Component
public class AuthorizationUtil {

    public static final String AUTHORIZATION_HEADER = HttpHeaders.AUTHORIZATION;
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String UNKNOWN_USER = "unknown";

    // Inject the public key string from application properties
    @Value("${jwt.public.key}")
    private String rsaPublicKeyString;

    private PublicKey publicKey; // Hold the parsed public key

    // Initialize the PublicKey after the bean is created
    @PostConstruct
    private void initPublicKey() {
        try {
            this.publicKey = loadPublicKey(rsaPublicKeyString);
            log.info("Successfully loaded JWT public key.");
        } catch (Exception e) {
            log.error("FATAL: Failed to load JWT public key. JWT validation will fail.", e);
            // You might want to prevent application startup here if the key is critical
            // throw new IllegalStateException("Failed to load JWT public key", e);
        }
    }

    public String extractUsernameFromContext(ContextView contextView) {
        if (this.publicKey == null) {
            log.error("Public key is not loaded. Cannot validate JWT.");
            return UNKNOWN_USER;
        }

        Optional<ServerWebExchange> exchangeOptional = contextView.getOrEmpty(ServerWebExchange.class);
        if (exchangeOptional.isEmpty()) {
            log.warn("ServerWebExchange not found in Reactor Context for CreatedBy/UpdatedBy field.");
            return UNKNOWN_USER;
        }

        ServerHttpRequest request = exchangeOptional.get().getRequest();
        HttpHeaders headers = request.getHeaders();
        String authHeader = headers.getFirst(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Authorization header is missing, empty, or not a Bearer token.");
            return UNKNOWN_USER;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Parse the token and verify signature using the public key
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(this.publicKey) // Specify the public key for verification
                    .build()
                    .parseSignedClaims(token); // Parse and verify

            // Extract the subject claim (usually the username)
            String username = jws.getPayload().getSubject();

            if (username == null || username.isBlank()) {
                log.warn("JWT 'sub' claim is missing or empty in the token.");
                return UNKNOWN_USER;
            }
            log.debug("Extracted username '{}' from JWT token.", username);
            return username;

        } catch (SecurityException e) {
            // Specifically catch key-related issues if needed
            log.warn("JWT validation failed due to key issue: {}", e.getMessage());
            return UNKNOWN_USER;
        } catch (JwtException e) {
            // Catches various JWT errors (expired, malformed, invalid signature etc.)
            log.warn("Failed to parse or verify JWT token: {}", e.getMessage());
            return UNKNOWN_USER;
        } catch (Exception e) {
            // Catch unexpected errors
            log.error("Unexpected error occurred while processing JWT token.", e);
            return UNKNOWN_USER;
        }
    }

    // Helper method to load the public key (same as before)
    private PublicKey loadPublicKey(String key) throws Exception {
        String publicKeyPEM = key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PUBLIC KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Assuming RSA
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * todo: replace this with real implementation
     * @param request
     * @return
     */
    public String getStaffIdFromToken(ServerHttpRequest request){
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        return "12345";
    }
}