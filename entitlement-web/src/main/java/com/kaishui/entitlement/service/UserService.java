package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.*;
import com.kaishui.entitlement.entity.dto.UserDto;
import com.kaishui.entitlement.entity.dto.UserResourceDto;
import com.kaishui.entitlement.exception.CommonException;
import com.kaishui.entitlement.exception.ResourceNotFoundException;
import com.kaishui.entitlement.repository.GroupDefaultRoleRepository;
import com.kaishui.entitlement.repository.ResourceRepository;
import com.kaishui.entitlement.repository.RoleRepository;
import com.kaishui.entitlement.repository.UserRepository;
import com.kaishui.entitlement.util.AdGroupUtil;
import com.kaishui.entitlement.util.AuthorizationUtil;
import com.kaishui.entitlement.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ResourceRepository resourceRepository;
    private final AuthorizationUtil authorizationUtil;
    private final UserMapper userMapper;
    private final AdGroupUtil adGroupUtil;
    private final GroupDefaultRoleRepository groupDefaultRoleRepository;

    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Mono<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    @Transactional
    public Mono<User> createUser(User user) {
        // Assuming 'user' comes with entitlements pre-populated if known
        log.info("Creating user: {}", user.getStaffId());
        user.setCreatedDate(new Date());
        user.setActive(true);
        user.setFirstLogin(true); // New users should go through first login process
        // Consider setting createdBy from context if available
        return Mono.deferContextual(contextView -> {
            String createdByUsername = authorizationUtil.extractUsernameFromContext(contextView);
            user.setCreatedBy(createdByUsername);
            return userRepository.save(user); // Process first login for the newly saved user
        });
    }

    /**
     * Updates an existing user's basic information based on staffId.
     * Preserves fields like ID, creation details, entitlements, and first login status.
     * Sets the updatedBy and lastModifiedDate fields.
     * Entitlements are NOT updated by this method; use insertOrUpdateUser or a dedicated method for that.
     *
     * @param user User object containing the updated information and the staffId to match.
     * @return Mono emitting the updated User or an error if not found or inactive.
     */
    @Transactional
    public Mono<User> updateUser(User user) {
        if (user.getStaffId() == null || user.getStaffId().isBlank()) {
            return Mono.error(new CommonException("StaffId is required for updating user."));
        }
        log.info("Attempting to update user with staffId: {}", user.getStaffId());

        return Mono.deferContextual(contextView -> {
            String updatedByUsername = authorizationUtil.extractUsernameFromContext(contextView);

            return userRepository.findByStaffId(user.getStaffId())
                    .flatMap(existingUser -> {
                        if (!existingUser.isActive()) {
                            log.warn("Attempted to update inactive user with staffId: {}", user.getStaffId());
                            return Mono.error(new CommonException("Cannot update inactive user with staffId: " + user.getStaffId()));
                        }
                        // mergeUserInfo only updates basic fields, not entitlements
                        mergeUserInfo(user, existingUser, updatedByUsername);
                        log.info("Saving updated user data for staffId: {}", existingUser.getStaffId());
                        return userRepository.save(existingUser);
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("User not found for update with staffId: {}", user.getStaffId());
                        return Mono.error(new CommonException("User not found for update with staffId: " + user.getStaffId()));
                    }));
        });
    }

    private static void mergeUserInfo(User sourceUser, User targetUser, String updatedByUsername) {
        if (StringUtils.hasText(sourceUser.getUsername()) && !sourceUser.getUsername().equals(targetUser.getUsername())) {
            targetUser.setUsername(sourceUser.getUsername());
        }
        if (StringUtils.hasText(sourceUser.getEmail()) && !sourceUser.getEmail().equals(targetUser.getEmail())) {
            targetUser.setEmail(sourceUser.getEmail());
        }
        if (StringUtils.hasText(sourceUser.getDepartment()) && !sourceUser.getDepartment().equals(targetUser.getDepartment())) {
            targetUser.setDepartment(sourceUser.getDepartment());
        }
        if (StringUtils.hasText(sourceUser.getFunctionalManager()) && !sourceUser.getFunctionalManager().equals(targetUser.getFunctionalManager())) {
            targetUser.setFunctionalManager(sourceUser.getFunctionalManager());
        }
        if (StringUtils.hasText(sourceUser.getEntityManager()) && !sourceUser.getEntityManager().equals(targetUser.getEntityManager())) {
            targetUser.setEntityManager(sourceUser.getEntityManager());
        }
        if (StringUtils.hasText(sourceUser.getJobTitle()) && !sourceUser.getJobTitle().equals(targetUser.getJobTitle())) {
            targetUser.setJobTitle(sourceUser.getJobTitle());
        }
        // Note: Entitlements are not merged here.
        targetUser.setLastModifiedDate(new Date());
        targetUser.setLastModifiedBy(updatedByUsername);
    }

    @Transactional
    public Mono<Void> deleteUser(String id) {
        log.info("Attempting to soft delete user with id: {}", id);
        return Mono.deferContextual(contextView -> {
            String deletedByUsername = authorizationUtil.extractUsernameFromContext(contextView);
            return userRepository.findById(id)
                    .flatMap(user -> {
                        if (!user.isActive()) {
                            log.info("User with id: {} is already inactive.", id);
                            return Mono.empty();
                        }
                        user.setActive(false);
                        user.setLastModifiedBy(deletedByUsername);
                        user.setLastModifiedDate(new Date());
                        log.info("Setting user with id: {} to inactive.", id);
                        return userRepository.save(user);
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("User not found for deletion with id: {}", id);
                        return Mono.error(new CommonException("User not found for deletion with id: " + id));
                    }))
                    .then();
        });
    }

    public Mono<User> processFirstLogin(User user) {
        if (!user.isFirstLogin() || CollectionUtils.isEmpty(user.getEntitlements())) {
            if (user.isFirstLogin() && CollectionUtils.isEmpty(user.getEntitlements())) {
                log.info("User {} is on first login but has no entitlements. Marking first login as false.", user.getStaffId());
                user.setFirstLogin(false);
                return userRepository.save(user);
            }
            return Mono.just(user);
        }

        List<String> adGroupsFromUser = user.getEntitlements().stream()
                .map(Entitlement::getAdGroup)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(adGroupsFromUser)) {
            log.info("User {} has entitlements, but no AD groups found within them. Marking first login as false.", user.getStaffId());
            user.setFirstLogin(false);
            return userRepository.save(user);
        }

        return groupDefaultRoleRepository.findByGroupNameIn(adGroupsFromUser)
                .collectMap(GroupDefaultRole::getGroupName, GroupDefaultRole::getRoleIds)
                .flatMap(defaultRolesMap -> {
                    boolean entitlementsChanged = false;
                    for (Entitlement entitlement : user.getEntitlements()) {
                        if (StringUtils.hasText(entitlement.getAdGroup())) {
                            List<String> defaultRoleIdsForGroup = defaultRolesMap.get(entitlement.getAdGroup());
                            if (!CollectionUtils.isEmpty(defaultRoleIdsForGroup)) {
                                if (entitlement.getRoleIds() == null) {
                                    entitlement.setRoleIds(new ArrayList<>());
                                }
                                // Add new default roles, ensuring uniqueness
                                Set<String> currentRoleIds = new HashSet<>(entitlement.getRoleIds());
                                if (currentRoleIds.addAll(defaultRoleIdsForGroup)) {
                                    entitlementsChanged = true;
                                }
                                entitlement.setRoleIds(new ArrayList<>(currentRoleIds));
                            }
                        }
                    }
                    user.setFirstLogin(false);
                    if (entitlementsChanged) {
                        log.info("Processed first login for user {}. Entitlements updated with default roles.", user.getStaffId());
                    } else {
                        log.info("Processed first login for user {}. No new default roles added to entitlements.", user.getStaffId());
                    }
                    return userRepository.save(user);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("No default roles found for any AD groups of user {}. Marking first login as false.", user.getStaffId());
                    user.setFirstLogin(false);
                    return userRepository.save(user);
                }));
    }

    @Transactional
    public Mono<User> insertOrUpdateUser(User userFromRequest) {
        log.info("Inserting or updating user with staffId: {}", userFromRequest.getStaffId());
        if (!StringUtils.hasText(userFromRequest.getStaffId())) {
            return Mono.error(new CommonException("StaffId cannot be null or blank for insert/update."));
        }

        return Mono.deferContextual(contextView -> {
            String operatorUsername = authorizationUtil.extractUsernameFromContext(contextView);

            return userRepository.findByStaffId(userFromRequest.getStaffId())
                    .flatMap(existingUser -> { // User exists, update it
                        if (!existingUser.isActive() && userFromRequest.isActive()) {
                            // Logic for reactivating a user if needed
                            log.info("Reactivating user with staffId: {}", existingUser.getStaffId());
                            existingUser.setActive(true);
                            // Potentially reset firstLogin if reactivation implies re-evaluation of roles
                            // existingUser.setFirstLogin(true);
                        } else if (!existingUser.isActive() && !userFromRequest.isActive()) {
                            log.warn("Attempted to update inactive user via insertOrUpdate with staffId: {}. User remains inactive.", userFromRequest.getStaffId());
                            // Optionally, still update other fields if allowed for inactive users
                        }
                        log.info("Updating existing user with staffId: {}", existingUser.getStaffId());

                        existingUser.setUsername(userFromRequest.getUsername());
                        existingUser.setEmail(userFromRequest.getEmail());
                        existingUser.setDepartment(userFromRequest.getDepartment());
                        existingUser.setFunctionalManager(userFromRequest.getFunctionalManager());
                        existingUser.setEntityManager(userFromRequest.getEntityManager());
                        existingUser.setJobTitle(userFromRequest.getJobTitle());
                        existingUser.setEntitlements(userFromRequest.getEntitlements()); // Update entitlements
                        existingUser.setActive(userFromRequest.isActive()); // Update active status

                        existingUser.setLastModifiedDate(new Date());
                        existingUser.setLastModifiedBy(operatorUsername);
                        // If entitlements changed significantly, or user reactivated, consider if firstLogin needs reprocessing.
                        // For now, we assume processFirstLogin is mainly for brand new users or explicit re-trigger.
                        // If userFromRequest.isFirstLogin() is meaningful, use it:
                        // existingUser.setFirstLogin(userFromRequest.isFirstLogin());

                        return userRepository.save(existingUser);
                    })
                    .switchIfEmpty(Mono.defer(() -> { // User does not exist, insert new
                        log.info("Inserting new user with staffId: {}", userFromRequest.getStaffId());
                        userFromRequest.setCreatedDate(new Date());
                        userFromRequest.setCreatedBy(operatorUsername);
                        if (userFromRequest.getEntitlements() == null) { // Ensure entitlements list exists
                            userFromRequest.setEntitlements(new ArrayList<>());
                        }
                        // isActive and isFirstLogin should be set on userFromRequest by caller or default to true
                        // userFromRequest.setActive(true); // Default for new user
                        // userFromRequest.setFirstLogin(true); // Default for new user

                        return userRepository.save(userFromRequest)
                                .flatMap(this::processFirstLogin);
                    }));
        });
    }

    public Mono<UserDto> getRolesAndPermissions(String staffId) {
        return userRepository.findByStaffId(staffId)
                .flatMap(this::getRolesAndPermissionsByUser)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with staffId: " + staffId)))
                .doOnError(e -> log.error("Error fetching user details for staffId {}: {}", staffId, e.getMessage(), e));
    }

    public Mono<UserDto> getRolesAndPermissionsByUser(User user) {
        List<String> userAdGroups = Optional.ofNullable(user.getEntitlements()).orElse(Collections.emptyList()).stream()
                .map(Entitlement::getAdGroup)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        List<String> userRoleIds = Optional.ofNullable(user.getEntitlements()).orElse(Collections.emptyList()).stream()
                .filter(e -> !CollectionUtils.isEmpty(e.getRoleIds()))
                .flatMap(e -> e.getRoleIds().stream())
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        UserDto userDto = userMapper.toDto(user);

        if (CollectionUtils.isEmpty(userRoleIds)) {
            userDto.setRoles(Collections.emptyList());
            userDto.setResources(Collections.emptyList());
            return Mono.just(userDto);
        }

        Mono<List<Role>> rolesMono = roleRepository.findAllByIdAndIsActive(userRoleIds, true)
                .collectList()
                .doOnNext(userDto::setRoles); // Set roles on DTO once fetched

        // If no AD groups, resources dependent on AD group matching will be empty.
        if (CollectionUtils.isEmpty(userAdGroups)) {
            userDto.setResources(Collections.emptyList());
            return rolesMono.thenReturn(userDto); // Return DTO after rolesMono completes
        }

        Mono<List<Resource>> accessibleResourcesMono = rolesMono.flatMap(roles -> {
            // If rolesMono resulted in empty list (e.g. all userRoleIds were inactive)
            if (CollectionUtils.isEmpty(roles)) {
                return Mono.just(Collections.<Resource>emptyList());
            }
            List<String> uniqueResourceIdsFromRoles = roles.stream()
                    .filter(role -> !CollectionUtils.isEmpty(role.getResourceIds()))
                    .flatMap(role -> role.getResourceIds().stream())
                    .distinct()
                    .collect(Collectors.toList());

            if (uniqueResourceIdsFromRoles.isEmpty()) {
                return Mono.just(Collections.<Resource>emptyList());
            }
            return resourceRepository.findAllByIdInAndIsActiveAndAdGroupsIn(
                            uniqueResourceIdsFromRoles, true, userAdGroups)
                    .collectList();
        });

        return Mono.zip(rolesMono, accessibleResourcesMono) // rolesMono is already setting roles on userDto via doOnNext
                .map(tuple -> {
                    // List<Role> roles = tuple.getT1(); // Already set by doOnNext
                    List<Resource> accessibleResources = tuple.getT2();
                    List<UserResourceDto> accessibleResourceDtos = accessibleResources.stream()
                            .map(this::mapToUserResourceDto)
                            .collect(Collectors.toList());
                    userDto.setResources(accessibleResourceDtos);
                    return userDto;
                });
    }

    private UserResourceDto mapToUserResourceDto(Resource resource) {
        if (resource == null) return null;
        UserResourceDto dto = new UserResourceDto();
        dto.setId(resource.getId());
        dto.setName(resource.getName());
        dto.setPermission(resource.getPermission());
        dto.setType(resource.getType());
        dto.setDescription(resource.getDescription());
        return dto;
    }

    public Flux<Role> findRolesByUserCase(String userCase, String staffId) {
        return userRepository.findByStaffId(staffId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User not found for findRolesByUserCase with staffId: {}", staffId);
                    return Mono.empty();
                }))
                .flatMapMany(user -> {
                    List<String> userRoleIds = Optional.ofNullable(user.getEntitlements()).orElse(Collections.emptyList()).stream()
                            .filter(e -> !CollectionUtils.isEmpty(e.getRoleIds()))
                            .flatMap(e -> e.getRoleIds().stream())
                            .filter(StringUtils::hasText).distinct().collect(Collectors.toList());

                    if (CollectionUtils.isEmpty(userRoleIds)) {
                        return Flux.empty();
                    }

                    List<String> userAdGroups = Optional.ofNullable(user.getEntitlements()).orElse(Collections.emptyList()).stream()
                            .map(Entitlement::getAdGroup)
                            .filter(StringUtils::hasText).distinct().collect(Collectors.toList());

                    if (adGroupUtil.isAdmin(userAdGroups, userCase)) {
                        log.info("User '{}' is admin for userCase '{}', returning all active roles for this userCase", staffId, userCase);
                        return roleRepository.findAllByUserCaseAndIsActive(userCase, true);
                    }
                    return roleRepository.findAllByIdsAndUserCaseAndIsActive(userRoleIds, userCase, true);
                });
    }

    public Flux<UserDto> getNextLevelUser(String userCase, String staffId) {
        return userRepository.findByStaffId(staffId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Requesting user not found for getNextLevelUser with staffId: {}", staffId);
                    return Mono.<User>empty();
                }))
                .flatMap(requestingUser -> {
                    List<String> requestingUserAdGroups = Optional.ofNullable(requestingUser.getEntitlements()).orElse(Collections.emptyList()).stream()
                            .map(Entitlement::getAdGroup)
                            .filter(StringUtils::hasText).distinct().collect(Collectors.toList());

                    String nextLevelADGroup = adGroupUtil.getNextLevelADGroup(userCase, requestingUserAdGroups);

                    if (!StringUtils.hasText(nextLevelADGroup)) {
                        log.warn("Could not determine next level AD group for userCase '{}' and requesting user '{}'. Returning empty.", userCase, staffId);
                        return Mono.just(Collections.<User>emptyList());
                    }
                    return userRepository.findByAdGroupAndIsActive(nextLevelADGroup, true).collectList();
                })
                .flatMapMany(targetUsers -> {
                    if (targetUsers.isEmpty()) return Flux.empty();

                    Set<String> allTargetUsersRoleIds = targetUsers.stream()
                            .filter(u -> !CollectionUtils.isEmpty(u.getEntitlements()))
                            .flatMap(u -> u.getEntitlements().stream())
                            .filter(e -> !CollectionUtils.isEmpty(e.getRoleIds()))
                            .flatMap(e -> e.getRoleIds().stream())
                            .filter(StringUtils::hasText).collect(Collectors.toSet());

                    if (allTargetUsersRoleIds.isEmpty()) {
                        return Flux.fromIterable(targetUsers).map(targetUser -> {
                            UserDto dto = userMapper.toDto(targetUser);
                            dto.setRoles(Collections.emptyList());
                            return dto;
                        });
                    }

                    Mono<Map<String, Role>> rolesMapMono = roleRepository.findAllByIdsAndUserCaseAndIsActive(
                                    List.copyOf(allTargetUsersRoleIds), userCase, true)
                            .collectMap(Role::getId, Function.identity());

                    return rolesMapMono.flatMapMany(rolesMap ->
                            Flux.fromIterable(targetUsers).map(targetUser -> {
                                UserDto dto = userMapper.toDto(targetUser);
                                List<String> currentTargetUserRoleIds = Optional.ofNullable(targetUser.getEntitlements()).orElse(Collections.emptyList()).stream()
                                        .filter(e -> !CollectionUtils.isEmpty(e.getRoleIds()))
                                        .flatMap(e -> e.getRoleIds().stream())
                                        .filter(StringUtils::hasText).distinct().collect(Collectors.toList());

                                List<Role> userSpecificRoles = currentTargetUserRoleIds.stream()
                                        .map(rolesMap::get)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());
                                dto.setRoles(userSpecificRoles);
                                return dto;
                            })
                    );
                });
    }
}