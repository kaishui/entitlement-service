package com.kaishui.entitlement.service;


import com.kaishui.entitlement.constant.PermissionFieldConstant;
import com.kaishui.entitlement.constant.ResourceType;
import com.kaishui.entitlement.entity.Entitlement; // Import Entitlement
import com.kaishui.entitlement.entity.Resource;
import com.kaishui.entitlement.entity.User;
import com.kaishui.entitlement.repository.ResourceRepository;
import com.kaishui.entitlement.repository.RoleRepository;
import com.kaishui.entitlement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils; // Import StringUtils
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects; // Import Objects
import java.util.Optional; // Import Optional
import java.util.stream.Collectors; // Import Collectors

@Service
@RequiredArgsConstructor
@Slf4j
public class UriPermissionCheckerService implements PermissionCheckerInterface {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ResourceRepository resourceRepository;

    // AntPathMatcher for URI pattern matching
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Checks if a user has permission for a given HTTP method and URI,
     * considering roles and AD group intersections for URI resources.
     *
     * @param staffId    The staffId to check.
     * @param httpMethod The HTTP method (e.g., "GET", "POST", "PUT", "DELETE", "*").
     * @param requestUri The requested URI path (e.g., "/users/123").
     * @return Mono<Boolean> emitting true if permission is granted, false otherwise.
     */
    @Override
    public Mono<Boolean> checkPermission(String staffId, String httpMethod, String requestUri) {
        log.debug("Checking permission for user '{}', method '{}', uri '{}'", staffId, httpMethod, requestUri);

        return userRepository.findByStaffId(staffId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User not found: {}", staffId);
                    return Mono.empty(); // No user, no permission
                }))
                .flatMapMany(user -> { // Use flatMapMany to process roles and resources
                    // Extract roleIds from entitlements
                    List<String> userRoleIds = Optional.ofNullable(user.getEntitlements()).orElse(Collections.emptyList())
                            .stream()
                            .filter(e -> !CollectionUtils.isEmpty(e.getRoleIds()))
                            .flatMap(e -> e.getRoleIds().stream())
                            .filter(StringUtils::hasText)
                            .distinct()
                            .collect(Collectors.toList());

                    // Extract adGroups from entitlements
                    List<String> userAdGroups = Optional.ofNullable(user.getEntitlements()).orElse(Collections.emptyList())
                            .stream()
                            .map(Entitlement::getAdGroup)
                            .filter(StringUtils::hasText)
                            .distinct()
                            .collect(Collectors.toList());

                    log.trace("User found: {}, Role IDs from Entitlements: {}, AD Groups from Entitlements: {}",
                            user.getUsername(), userRoleIds, userAdGroups);

                    if (CollectionUtils.isEmpty(userRoleIds)) {
                        log.debug("User '{}' has no roles assigned via entitlements.", staffId);
                        return Flux.empty(); // No roles, no permissions from roles
                    }

                    // Fetch roles associated with the user
                    return roleRepository.findAllByIdAndIsActive(userRoleIds, true)
                            .flatMap(role -> {
                                if (CollectionUtils.isEmpty(role.getResourceIds())) {
                                    return Flux.empty(); // No resources in this role
                                }
                                // Fetch only URI resources associated with this role
                                return resourceRepository.findAllByIdInAndTypeAndIsActive(role.getResourceIds(), ResourceType.API.name(), true);
                            })
                            // Filter resources: Must have at least one AD Group matching the user's AD Groups
                            .filter(resource -> checkAdGroupIntersection(userAdGroups, resource)); // Pass extracted AD groups
                })
                // Now we have a Flux<Resource> containing only URI resources the user has access to
                // via their roles AND matching AD groups.
                .filter(resource -> hasMatchingPermissionRule(resource, httpMethod, requestUri))
                .hasElements() // Check if *any* resource contained a matching permission rule after all filters
                .doOnSuccess(hasPermission -> log.info("Permission check result for user '{}', method '{}', uri '{}': {}",
                        staffId, httpMethod, requestUri, hasPermission))
                .defaultIfEmpty(false); // Default to false if the stream was empty at any critical point
    }

    /**
     * Checks if there's an intersection between the user's AD groups and the resource's AD groups.
     * @param userAdGroups The list of AD groups extracted from the user's entitlements.
     * @param resource The resource to check.
     */
    private boolean checkAdGroupIntersection(List<String> userAdGroups, Resource resource) {
        // userAdGroups is now passed as a parameter
        List<String> resourceAdGroups = resource.getAdGroups();

        if (CollectionUtils.isEmpty(userAdGroups) || CollectionUtils.isEmpty(resourceAdGroups)) {
            log.trace("AD Group check: No intersection possible for resource '{}' because user ({}) or resource ({}) AD groups are empty/null.",
                    resource.getName(), userAdGroups, resourceAdGroups);
            return false; // No intersection if either list is empty or null
        }

        // Efficient check for any common element
        boolean intersects = !Collections.disjoint(userAdGroups, resourceAdGroups);

        if (intersects) {
            log.trace("AD Group check: Intersection found for resource '{}'. User groups: {}, Resource groups: {}",
                    resource.getName(), userAdGroups, resourceAdGroups);
        } else {
            log.trace("AD Group check: No intersection found for resource '{}'. User groups: {}, Resource groups: {}",
                    resource.getName(), userAdGroups, resourceAdGroups);
        }
        return intersects;
    }


    /**
     * Checks if a specific resource contains a permission rule matching the method and URI.
     * (This method remains the same as the previous version)
     */
    private boolean hasMatchingPermissionRule(Resource resource, String httpMethod, String requestUri) {
        if (CollectionUtils.isEmpty(resource.getPermission())) {
            log.trace("Resource '{}' (ID: {}) has no permission rules defined.", resource.getName(), resource.getId());
            return false;
        }

        Document permissionDoc = resource.getPermission();
        // Safely extract method and uri patterns from the BSON Document
        // Adjust keys ("method", "uri") based on your actual structure in the permission Document
        String methodPattern = permissionDoc.getString(PermissionFieldConstant.METHOD);
        String uriPattern = permissionDoc.getString(PermissionFieldConstant.URI);

        // IMPORTANT: Only consider rules that actually define a method and URI for this check
        if (methodPattern == null || uriPattern == null) {
            log.trace("Skipping non-URI/method permission rule in resource '{}': {}", resource.getName(), permissionDoc.toJson());
            return false;
        }

        // 1. Check Method
        boolean methodMatches = methodPattern.equals("*") || methodPattern.equalsIgnoreCase(httpMethod);

        // 2. Check URI using AntPathMatcher
        boolean uriMatches = pathMatcher.match(uriPattern, requestUri);

        if (methodMatches && uriMatches) {
            log.debug("Permission match found in resource '{}' (ID: {}): Rule='{}', Requested Method='{}', Requested URI='{}'",
                    resource.getName(), resource.getId(), permissionDoc.toJson(), httpMethod, requestUri);
            return true; // Found a matching rule
        }

        log.trace("No matching URI/method permission rule found in resource '{}' (ID: {}) for method '{}', uri '{}'",
                resource.getName(), resource.getId(), httpMethod, requestUri);
        return false; // No rule in this resource matched
    }
}