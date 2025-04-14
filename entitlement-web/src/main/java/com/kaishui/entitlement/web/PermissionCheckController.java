package com.kaishui.entitlement.web; // Adjust package name as needed

import com.kaishui.entitlement.service.PermissionCheckerInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/v1/api/permissions") // Define a base path for permission-related endpoints
@RequiredArgsConstructor
@Slf4j
public class PermissionCheckController {

    private final PermissionCheckerInterface permissionChecker; // Inject the service interface

    /**
     * Checks if a user has permission for a specific HTTP method and URI.
     *
     * @param staffId    The staff ID of the user to check.
     * @param httpMethod The HTTP method (e.g., GET, POST).
     * @param requestUri The URI path to check (e.g., /api/users/123).
     * @return A Mono containing a ResponseEntity. The body will be a JSON object
     *         like {"allowed": true} or {"allowed": false}.
     */
    @GetMapping("/check")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkUriPermission(
            @RequestParam String staffId,
            @RequestParam String httpMethod,
            @RequestParam String requestUri) { // Use @RequestParam to get query parameters

        log.info("Received permission check request: staffId='{}', method='{}', uri='{}'",
                staffId, httpMethod, requestUri);

        // Validate input parameters (basic example)
        if (staffId == null || staffId.isBlank() || httpMethod == null || httpMethod.isBlank() || requestUri == null || requestUri.isBlank()) {
            log.warn("Invalid input parameters for permission check.");
            // Consider returning a 400 Bad Request more formally
            return Mono.just(ResponseEntity.badRequest().body(Map.of("allowed", false)));
        }

        return permissionChecker.checkPermission(staffId, httpMethod, requestUri)
                .map(allowed -> {
                    log.info("Permission check result for staffId='{}', method='{}', uri='{}': {}",
                            staffId, httpMethod, requestUri, allowed);
                    // Return a simple JSON response: {"allowed": true/false}
                    return ResponseEntity.ok(Map.of("allowed", allowed));
                })
                .onErrorResume(ex -> {
                    // Handle potential errors during the permission check
                    log.warn("Error during permission check for staffId='{}', method='{}', uri='{}': {}",
                            staffId, httpMethod, requestUri, ex.getMessage(), ex);
                    // Return a 500 Internal Server Error or a specific error response
                    // For simplicity, returning "allowed: false" on error might be acceptable in some cases,
                    // but ideally, you'd signal an error state.
                    return Mono.just(ResponseEntity.internalServerError().body(Map.of("allowed", false)));
                    // Or: return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Permission check failed")));
                });
    }
}