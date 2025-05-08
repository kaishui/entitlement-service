package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.*;
import com.kaishui.entitlement.entity.dto.EntitlementDto; // Ensure EntitlementDto is imported
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
        log.info("Creating user: {}", user.getStaffId());
        user.setCreatedDate(new Date());
        user.setActive(true);
        user.setFirstLogin(true);
        return Mono.deferContextual(contextView -> {
            String createdByUsername = authorizationUtil.extractUsernameFromContext(contextView);
            user.setCreatedBy(createdByUsername);
            return userRepository.save(user);
        });
    }

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
        // Corrected: Check if staffId is NOT blank
        if (!StringUtils.hasText(userFromRequest.getStaffId())) {
            return Mono.error(new CommonException("StaffId cannot be null or blank for insert/update."));
        }

        return Mono.deferContextual(contextView -> { // Defer for contextual access
            String operatorUsername = authorizationUtil.extractUsernameFromContext(contextView);

            return userRepository.findByStaffId(userFromRequest.getStaffId())
                    .flatMap(existingUser -> { // User exists, update it
                        if (!existingUser.isActive() && userFromRequest.isActive()) {
                            log.info("Reactivating user with staffId: {}", existingUser.getStaffId());
                            existingUser.setActive(true);
                        } else if (!existingUser.isActive() && !userFromRequest.isActive()) {
                            log.warn("Attempted to update inactive user via insertOrUpdate with staffId: {}. User remains inactive.", userFromRequest.getStaffId());
                        }
                        log.info("Updating existing user with staffId: {}", existingUser.getStaffId());

                        existingUser.setUsername(userFromRequest.getUsername());
                        existingUser.setEmail(userFromRequest.getEmail());
                        existingUser.setDepartment(userFromRequest.getDepartment());
                        existingUser.setFunctionalManager(userFromRequest.getFunctionalManager());
                        existingUser.setEntityManager(userFromRequest.getEntityManager());
                        existingUser.setJobTitle(userFromRequest.getJobTitle());

                        List<Entitlement> mergedEntitlements = mergeEntitlements(
                                Optional.ofNullable(existingUser.getEntitlements()).orElse(Collections.emptyList()),
                                Optional.ofNullable(userFromRequest.getEntitlements()).orElse(Collections.emptyList())
                        );
                        existingUser.setEntitlements(mergedEntitlements);
                        existingUser.setActive(userFromRequest.isActive());

                        existingUser.setLastModifiedDate(new Date());
                        existingUser.setLastModifiedBy(operatorUsername); // Use operator from context

                        return userRepository.save(existingUser);
                    })
                    .switchIfEmpty(Mono.defer(() -> { // User does not exist, insert new
                        log.info("Inserting new user with staffId: {}", userFromRequest.getStaffId());
                        userFromRequest.setCreatedDate(new Date());
                        userFromRequest.setCreatedBy(operatorUsername); // Use operator from context
                        if (userFromRequest.getEntitlements() == null) {
                            userFromRequest.setEntitlements(new ArrayList<>());
                        }
                        // isActive and isFirstLogin should be set on userFromRequest by caller or default
                        // For a new user created via this method, processFirstLogin will be called by UserController
                        return userRepository.save(userFromRequest);
                    }));
        });
    }

    private List<Entitlement> mergeEntitlements(List<Entitlement> existingEntitlements, List<Entitlement> requestEntitlements) {
        // If request has no entitlements, it implies all existing ones should be removed.
        // This makes the request the source of truth for AD group memberships.
        if (CollectionUtils.isEmpty(requestEntitlements)) {
            log.debug("Request entitlements are empty. Resulting entitlements will be empty.");
            return Collections.emptyList();
        }

        Map<String, List<String>> existingAdGroupToRolesMap = existingEntitlements.stream()
                .filter(e -> StringUtils.hasText(e.getAdGroup()))
                .collect(Collectors.toMap(
                        Entitlement::getAdGroup,
                        e -> Optional.ofNullable(e.getRoleIds()).orElse(Collections.emptyList()),
                        (roles1, roles2) -> roles1 // Handle potential duplicates in existing data
                ));

        List<Entitlement> finalEntitlements = new ArrayList<>();

        for (Entitlement requestedEntitlement : requestEntitlements) {
            if (requestedEntitlement == null || !StringUtils.hasText(requestedEntitlement.getAdGroup())) {
                log.warn("Skipping invalid entitlement in request: {}", requestedEntitlement);
                continue;
            }
            String adGroupInRequest = requestedEntitlement.getAdGroup();

            // If the AD group from the request already exists, preserve its current roles.
            // New AD groups from the request will have empty roles (as set by UserMapper or if request has them empty).
            // Roles are typically assigned by processFirstLogin or other dedicated role management.
            List<String> rolesToSet = existingAdGroupToRolesMap.getOrDefault(adGroupInRequest,
                    Optional.ofNullable(requestedEntitlement.getRoleIds()).orElse(Collections.emptyList()));


            finalEntitlements.add(Entitlement.builder()
                    .adGroup(adGroupInRequest)
                    .roleIds(new ArrayList<>(rolesToSet)) // Ensure a mutable list
                    .build());
        }
        log.debug("Merged entitlements: {}", finalEntitlements);
        return finalEntitlements;
    }


    public Mono<UserDto> getRolesAndPermissions(String staffId) {
        return userRepository.findByStaffId(staffId)
                .flatMap(this::getRolesAndPermissionsByUser)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with staffId: " + staffId)))
                .doOnError(e -> log.error("Error fetching user details for staffId {}: {}", staffId, e.getMessage(), e));
    }

    public Mono<UserDto> getRolesAndPermissionsByUser(User user) {
        UserDto userDto = userMapper.toDto(user); // Maps basic fields

        if (CollectionUtils.isEmpty(user.getEntitlements())) {
            userDto.setEntitlements(Collections.emptyList());
            userDto.setResources(Collections.emptyList());
            return Mono.just(userDto);
        }

        Flux<EntitlementDto> entitlementDtoFlux = Flux.fromIterable(user.getEntitlements())
                .publishOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .concatMap(entityEntitlement -> { // Use concatMap to preserve order and handle async calls sequentially per entitlement
                    String adGroup = entityEntitlement.getAdGroup();
                    List<String> roleIds = Optional.ofNullable(entityEntitlement.getRoleIds()).orElse(Collections.emptyList());

                    if (!StringUtils.hasText(adGroup)) {
                        return Mono.empty(); // Skip if AD group is blank
                    }

                    if (CollectionUtils.isEmpty(roleIds)) {
                        return Mono.just(EntitlementDto.builder()
                                .adGroup(adGroup)
                                .roles(Collections.emptyList())
                                .build());
                    }
                    return roleRepository.findAllByIdAndIsActive(roleIds, true)
                            .collectList()
                            .map(roles -> EntitlementDto.builder()
                                    .adGroup(adGroup)
                                    .roles(roles)
                                    .build());
                });

        return entitlementDtoFlux.collectList()
                .flatMap(entitlementDtos -> {
                    userDto.setEntitlements(entitlementDtos);

                    List<String> allUserAdGroupsFromEntity = user.getEntitlements().stream()
                            .map(Entitlement::getAdGroup)
                            .filter(StringUtils::hasText)
                            .distinct()
                            .collect(Collectors.toList());

                    List<Role> allRolesFromDto = entitlementDtos.stream()
                            .filter(eDto -> eDto.getRoles() != null) // Ensure roles list is not null
                            .flatMap(eDto -> eDto.getRoles().stream())
                            .filter(Objects::nonNull) // Ensure individual role is not null
                            .distinct() // Get distinct Role objects
                            .collect(Collectors.toList());

                    if (CollectionUtils.isEmpty(allRolesFromDto) || CollectionUtils.isEmpty(allUserAdGroupsFromEntity)) {
                        userDto.setResources(Collections.emptyList());
                        return Mono.just(userDto);
                    }

                    List<String> uniqueResourceIdsFromRoles = allRolesFromDto.stream()
                            .filter(role -> !CollectionUtils.isEmpty(role.getResourceIds()))
                            .flatMap(role -> role.getResourceIds().stream())
                            .filter(StringUtils::hasText) // Ensure resource ID is not blank
                            .distinct()
                            .collect(Collectors.toList());

                    if (uniqueResourceIdsFromRoles.isEmpty()) {
                        userDto.setResources(Collections.emptyList());
                        return Mono.just(userDto);
                    }

                    return resourceRepository.findAllByIdInAndIsActiveAndAdGroupsIn(
                                    uniqueResourceIdsFromRoles, true, allUserAdGroupsFromEntity)
                            .collectList()
                            .map(accessibleResources -> {
                                List<UserResourceDto> accessibleResourceDtos = accessibleResources.stream()
                                        .map(this::mapToUserResourceDto)
                                        .collect(Collectors.toList());
                                userDto.setResources(accessibleResourceDtos);
                                return userDto;
                            });
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
                    // Use the updated findByAdGroupAndIsActive from UserRepository
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
                            UserDto dto = userMapper.toDto(targetUser); // Basic mapping
                            // Populate EntitlementDtos with AD groups and empty roles
                            dto.setEntitlements(
                                    Optional.ofNullable(targetUser.getEntitlements()).orElse(Collections.emptyList())
                                            .stream()
                                            .filter(e -> StringUtils.hasText(e.getAdGroup()))
                                            .map(e -> EntitlementDto.builder().adGroup(e.getAdGroup()).roles(Collections.emptyList()).build())
                                            .collect(Collectors.toList())
                            );
                            dto.setResources(Collections.emptyList());
                            return dto;
                        });
                    }

                    Mono<Map<String, Role>> rolesMapMono = roleRepository.findAllByIdsAndUserCaseAndIsActive(
                                    new ArrayList<>(allTargetUsersRoleIds), userCase, true)
                            .collectMap(Role::getId, Function.identity());

                    return rolesMapMono.flatMapMany(rolesMap ->
                            Flux.fromIterable(targetUsers).map(targetUser -> {
                                UserDto dto = userMapper.toDto(targetUser); // Basic mapping

                                if (CollectionUtils.isEmpty(targetUser.getEntitlements())) {
                                    dto.setEntitlements(Collections.emptyList());
                                } else {
                                    List<EntitlementDto> entitlementDtos = targetUser.getEntitlements().stream()
                                            .filter(entityEntitlement -> StringUtils.hasText(entityEntitlement.getAdGroup()))
                                            .map(entityEntitlement -> {
                                                String adGroup = entityEntitlement.getAdGroup();
                                                List<String> entityRoleIds = Optional.ofNullable(entityEntitlement.getRoleIds()).orElse(Collections.emptyList());

                                                List<Role> rolesForThisEntitlementDto = entityRoleIds.stream()
                                                        .map(rolesMap::get)
                                                        .filter(Objects::nonNull)
                                                        .collect(Collectors.toList());

                                                return EntitlementDto.builder()
                                                        .adGroup(adGroup)
                                                        .roles(rolesForThisEntitlementDto)
                                                        .build();
                                            })
                                            .collect(Collectors.toList());
                                    dto.setEntitlements(entitlementDtos);
                                }
                                dto.setResources(Collections.emptyList()); // Resources not typically populated here
                                return dto;
                            })
                    );
                });
    }
}