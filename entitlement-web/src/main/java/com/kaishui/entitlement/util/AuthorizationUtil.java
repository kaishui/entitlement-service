package com.kaishui.entitlement.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.util.context.ContextView;

import java.util.Optional;

@Slf4j
@UtilityClass
public class AuthorizationUtil {
    // Define the header name used by the Gateway to pass the username
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String UNKNOWN_USER = "unknown"; // Default user if header is missing

     public String extractUsernameFromContext(ContextView contextView) {
        Optional<ServerWebExchange> exchangeOptional = contextView.getOrEmpty(ServerWebExchange.class);
        if (exchangeOptional.isPresent()) {
            ServerHttpRequest request = exchangeOptional.get().getRequest();
            HttpHeaders headers = request.getHeaders();
            // Extract username from the predefined header (e.g., Authorization or a custom one)
            String usernameFromHeader = headers.getFirst(AUTHORIZATION_HEADER); // Use the correct header name
            if (usernameFromHeader != null && !usernameFromHeader.isBlank()) {
                // If using Authorization header (like JWT Bearer),  might need to parse the token here
                //  for Bearer token (requires JWT library):
                // if (usernameFromHeader.startsWith("Bearer ")) {
                //     String token = usernameFromHeader.substring(7);
                //     // Decode token and get username (e.g., using a JwtDecoder bean)
                //     // return jwtDecoder.decode(token).getSubject();
                // }
                return usernameFromHeader; // Assuming header directly contains username for now
            } else {
                log.warn("Header '{}' is missing or empty in the request for CreatedBy/UpdatedBy field.", AUTHORIZATION_HEADER);
                return UNKNOWN_USER;
            }
        } else {
            log.warn("ServerWebExchange not found in Reactor Context for CreatedBy/UpdatedBy field.");
            return UNKNOWN_USER;
        }
    }
}
