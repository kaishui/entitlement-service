package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.GroupDefaultRole;
import com.kaishui.entitlement.entity.Resource;
import com.kaishui.entitlement.entity.Role;
import com.kaishui.entitlement.entity.User;
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
        log.info("Creating user: {}", user);
        return userRepository.save(user);
    }

    /**
     * Updates an existing user's basic information based on staffId.
     * Preserves fields like ID, creation details, roles, and first login status.
     * Sets the updatedBy and lastModifiedDate fields.
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

        // Use deferContextual to get the username for the 'updatedBy' field
        return Mono.deferContextual(contextView -> {
            String updatedByUsername = authorizationUtil.extractUsernameFromContext(contextView);

            return userRepository.findByStaffId(user.getStaffId())
                    .flatMap(existingUser -> { // Use flatMap for async operations
                        if (!existingUser.isActive()) {
                            log.warn("Attempted to update inactive user with staffId: {}", user.getStaffId());
                            // Return Mono.error instead of throwing directly
                            return Mono.error(new CommonException("Cannot update inactive user with staffId: " + user.getStaffId()));
                        }

                        mergeUserInfo(user, existingUser, updatedByUsername);
                        log.info("Saving updated user data for staffId: {}", existingUser.getStaffId());
                        return userRepository.save(existingUser); // Save the modified existingUser
                    })
                    .switchIfEmpty(Mono.defer(() -> { // Use defer to create the error Mono lazily
                        // Handle case where user with staffId is not found
                        log.warn("User not found for update with staffId: {}", user.getStaffId());
                        return Mono.error(new CommonException("User not found for update with staffId: " + user.getStaffId()));
                    }));
        });
    }

    private static void mergeUserInfo(User user, User existingUser, String updatedByUsername) {
        // Update only the fields that should be updatable via this method
        if (user.getUsername() != null && !user.getUsername().equals(existingUser.getUsername())) {
            existingUser.setUsername(user.getUsername());
        }
        if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getDepartment() != null && !user.getDepartment().equals(existingUser.getDepartment())) {
            existingUser.setDepartment(user.getDepartment());
        }
        if (user.getFunctionalManager() != null && !user.getFunctionalManager().equals(existingUser.getFunctionalManager())) {
            existingUser.setFunctionalManager(user.getFunctionalManager());
        }
        if (user.getEntityManager() != null && !user.getEntityManager().equals(existingUser.getEntityManager())) {
            existingUser.setEntityManager(user.getEntityManager());
        }
        if (user.getJobTitle() != null && !user.getJobTitle().equals(existingUser.getJobTitle())) {
            existingUser.setJobTitle(user.getJobTitle());
        }
        // Decide if adGroups should be updatable via this method
//                         if (user.getAdGroups() != null && !user.getAdGroups().equals(existingUser.getAdGroups())) {
//                            existingUser.setAdGroups(user.getAdGroups());
//                            changed = true;
//                         }

        // If changes were made, update audit fields and save
        existingUser.setLastModifiedDate(new Date());
        existingUser.setLastModifiedBy(updatedByUsername); // Set the user who performed the update
    }

    /**
     * Deletes a user (soft delete by setting isActive=false).
     *
     * @param id The ID (String) of the user to delete.
     * @return Mono<Void> indicating completion or error.
     */
    @Transactional // Optional: Usually not strictly needed for single-entity soft delete
    public Mono<Void> deleteUser(String id) {
        log.info("Attempting to soft delete user with id: {}", id);
        // Use deferContextual to get the username for 'updatedBy'
        return Mono.deferContextual(contextView -> {
            String deletedByUsername = authorizationUtil.extractUsernameFromContext(contextView);

            // Find the user by ID (assuming repository uses String ID)
            return userRepository.findById(id)
                    .flatMap(user -> {
                        // Check if already inactive
                        if (!user.isActive()) {
                            log.info("User with id: {} is already inactive.", id);
                            return Mono.empty(); // Nothing to do, complete successfully
                        }
                        // Set inactive and update audit fields
                        user.setActive(false);
                        user.setLastModifiedBy(deletedByUsername);
                        user.setLastModifiedDate(new Date());
                        log.info("Setting user with id: {} to inactive.", id);
                        // Save the updated user
                        return userRepository.save(user);
                    })
                    // Handle case where user is not found
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("User not found for deletion with id: {}", id);
                        return Mono.error(new CommonException("User not found for deletion with id: " + id));
                    }))
                    // Ensure the final result is Mono<Void>
                    .then();
        });
    }

    public Mono<User> processFirstLogin(User user) {
        if (!user.isFirstLogin()) {
            return Mono.just(user); // Not first login, do nothing
        }

        return groupDefaultRoleRepository.findByGroupNameIn(user.getAdGroups())
                .map(GroupDefaultRole::getRoleIds)
                .collectList()
                .map(lists -> lists.stream().flatMap(List::stream).distinct().toList())
                .flatMap(roleIds -> {
                    user.setRoleIds(roleIds);
                    user.setFirstLogin(false);
                    return userRepository.save(user);
                });
    }

    @Transactional
    public Mono<User> insertOrUpdateUser(User user) {
        log.info("Inserting or updating user: {}", user);
        return userRepository.findByStaffId(user.getStaffId())
                .flatMap(existingUser -> {
                    if (!existingUser.isActive()) {
                        return Mono.error(new CommonException("Cannot update inactive user with staffId: " + user.getStaffId()));
                    }
                    // Update existing user's fields
                    User updatedUser = User.builder()
                            .id(existingUser.getId())
                            .username(user.getUsername())
                            .staffId(existingUser.getStaffId())
                            .email(user.getEmail())
                            .department(user.getDepartment())
                            .functionalManager(user.getFunctionalManager())
                            .entityManager(user.getEntityManager())
                            .jobTitle(user.getJobTitle())
                            .isActive(existingUser.isActive())
                            .createdBy(existingUser.getCreatedBy())
                            .lastModifiedBy(user.getLastModifiedBy())
                            .createdDate(existingUser.getCreatedDate())
                            .lastModifiedDate(user.getLastModifiedDate())
                            .adGroups(user.getAdGroups())
                            .roleIds(existingUser.getRoleIds())
                            .isFirstLogin(existingUser.isFirstLogin())
                            .build();
                    return userRepository.save(updatedUser);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Insert new user
                    return userRepository.save(user);
                }));
    }

    public Mono<UserDto> getRolesAndPermissions(String staffId) {
        return userRepository.findByStaffId(staffId)
                .flatMap(user -> {
                    return getRolesAndPermissionsByUser(user);
                })
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with staffId: " + staffId)))
                .doOnError(e -> log.error("Error fetching user details for staffId {}: {}", staffId, e.getMessage(), e));
    }

    public Mono<UserDto> getRolesAndPermissionsByUser(User user) {
        // Get user's AD groups early
        List<String> userAdGroups = user.getAdGroups();
        if (CollectionUtils.isEmpty(userAdGroups) || CollectionUtils.isEmpty(user.getRoleIds())) {
            // Optimization: If user has no AD groups, they can't match any resource AD groups.
            // Return user with roles but empty resources immediately.
            log.debug("User '{}' has no AD groups / roles, skipping resource fetch.", user.getStaffId());
            if (CollectionUtils.isEmpty(user.getRoleIds())) {
                return Mono.just(userMapper.toDto(user));
            }
            return roleRepository.findAllById(user.getRoleIds()).collectList()
                    .map(roles -> {
                        UserDto userDto = userMapper.toDto(user);
                        userDto.setRoles(roles);
                        userDto.setResources(Collections.emptyList()); // No resources possible
                        return userDto;
                    });
        }
        // 1. Fetch all active roles for the user
        Mono<List<Role>> rolesMono = roleRepository.findAllByIdAndIsActive(user.getRoleIds(), true)
                .collectList();

        // 2. Collect unique resource IDs and fetch *filtered* resources in one go
        Mono<List<Resource>> accessibleResourcesMono = rolesMono.flatMap(roles -> {
            List<String> uniqueResourceIds = roles.stream()
                    .filter(role -> !CollectionUtils.isEmpty(role.getResourceIds()))
                    .flatMap(role -> role.getResourceIds().stream())
                    .distinct()
                    .collect(Collectors.toList());

            if (uniqueResourceIds.isEmpty()) {
                log.debug("User '{}' roles have no associated resource IDs.", user.getStaffId());
                return Mono.just(Collections.<Resource>emptyList());
            }

            log.debug("Fetching accessible resources for user '{}' with IDs: {} and AD Groups: {}",
                    user.getStaffId(), uniqueResourceIds, userAdGroups);

            return resourceRepository.findAllByIdInAndIsActiveAndAdGroupsIn(
                            uniqueResourceIds,
                            true,
                            userAdGroups
                    )
                    .collectList();
        });

        // 3. Zip roles and the *already filtered* resources
        return Mono.zip(rolesMono, accessibleResourcesMono)
                .map(tuple -> {
                    UserDto userDto = userMapper.toDto(user); // Map user entity
                    List<Role> roles = tuple.getT1();
                    List<Resource> accessibleResources = tuple.getT2(); // These are already filtered by DB

                    userDto.setRoles(roles); // Set the fetched roles

                    // 4. Map the filtered Resource entities to UserResourceDto
                    List<UserResourceDto> accessibleResourceDtos = accessibleResources.stream()
                            .map(this::mapToUserResourceDto) // Use your existing mapping helper
                            .collect(Collectors.toList());

                    if (log.isDebugEnabled()) {
                        log.debug("User '{}' AD Groups: {}. Roles found: {}. Accessible Resources (from DB query): {}",
                                user.getStaffId(), userAdGroups, roles.stream().map(Role::getId).collect(Collectors.toList()),
                                accessibleResourceDtos.stream().map(UserResourceDto::getId).collect(Collectors.toList()));
                    }

                    userDto.setResources(accessibleResourceDtos); // Set the filtered & mapped resources
                    return userDto;
                });
    }

    private UserResourceDto mapToUserResourceDto(Resource resource) {
        if (resource == null) {
            return null;
        }
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
                    List<String> roleIds = user.getRoleIds();
                    log.info("staffId : {}, RoleIds: {}", staffId, roleIds);
                    if (CollectionUtils.isEmpty(roleIds)) {
                        return Flux.empty();
                    }

                    //1. if user has this userCase admin AD group, return all user case role
                    if (adGroupUtil.isAdmin(user.getAdGroups(), userCase)) {
                        log.info("User '{}' has admin AD group for userCase '{}', returning all user case roles", staffId, userCase);
                        return roleRepository.findAllByUserCaseAndIsActive(userCase, true);
                    }
                    return roleRepository.findAllByIdsAndUserCaseAndIsActive(roleIds, userCase, true);
                });
    }

    /**
     * Finds users belonging to the 'next level' AD group relative to the requesting user,
     * for a specific user case, and populates their DTOs with relevant roles for that case.
     * Optimized to fetch roles in a single batch query.
     * <p>
     * 1. If requesting user is Admin -> find Managers
     * 2. If requesting user is Manager -> find Users
     * 3. If requesting user is User -> find Managers (e.g., for applying permissions upwards)
     *
     * @param userCase The specific user case context.
     * @param staffId  The staff ID of the requesting user.
     * @return Flux emitting UserDto objects for the found 'next level' users, populated with their roles for the given userCase.
     */
    public Flux<UserDto> getNextLevelUser(String userCase, String staffId) {
        // 1. Find the requesting user
        return userRepository.findByStaffId(staffId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Requesting user not found for getNextLevelUser with staffId: {}", staffId);
                    return Mono.empty(); // Return empty if requesting user not found
                }))
                .flatMap(requestingUser -> {
                    // 2. Determine the target AD group
                    String nextLevelADGroup = adGroupUtil.getNextLevelADGroup(userCase, requestingUser.getAdGroups());

                    if (!StringUtils.hasText(nextLevelADGroup)) {
                        log.warn("Could not determine next level AD group for userCase '{}' and requesting user '{}'. Returning empty.",
                                userCase, staffId);
                        return Mono.just(Collections.<User>emptyList()); // Return empty list if no target group
                    }

                    log.info("Requesting user '{}' triggers search for users in AD group '{}' for userCase '{}'",
                            staffId, nextLevelADGroup, userCase);

                    // 3. Find all active users belonging to the target AD group and collect them
                    return userRepository.findByAdGroupAndIsActive(nextLevelADGroup, true).collectList();
                })
                .flatMapMany(targetUsers -> {
                    if (targetUsers.isEmpty()) {
                        return Flux.empty(); // No target users found
                    }

                    // 4. Extract all unique role IDs from the target users
                    Set<String> uniqueRoleIds = targetUsers.stream()
                            .filter(u -> !CollectionUtils.isEmpty(u.getRoleIds()))
                            .flatMap(u -> u.getRoleIds().stream())
                            .collect(Collectors.toSet());

                    // If no roles associated with any target user, map directly to DTOs with empty roles
                    if (uniqueRoleIds.isEmpty()) {
                        return Flux.fromIterable(targetUsers)
                                .map(user -> {
                                    UserDto dto = userMapper.toDto(user);
                                    dto.setRoles(Collections.emptyList());
                                    return dto;
                                });
                    }

                    // 5. Fetch all relevant roles for the userCase in a single query
                    Mono<Map<String, Role>> rolesMapMono = roleRepository.findAllByIdsAndUserCaseAndIsActive(
                                    List.copyOf(uniqueRoleIds), // Convert Set to List for repository method
                                    userCase,
                                    true
                            )
                            .collectMap(Role::getId, Function.identity()); // Create a Map<RoleId, Role>

                    // 6. Combine users and the roles map, then map to DTOs
                    return rolesMapMono.flatMapMany(rolesMap ->
                            Flux.fromIterable(targetUsers)
                                    .map(targetUser -> {
                                        UserDto dto = userMapper.toDto(targetUser);
                                        List<Role> userSpecificRoles = Collections.emptyList();

                                        // Filter the fetched roles based on the current user's roleIds
                                        if (!CollectionUtils.isEmpty(targetUser.getRoleIds())) {
                                            userSpecificRoles = targetUser.getRoleIds().stream()
                                                    .map(rolesMap::get) // Look up role in the map
                                                    .filter(java.util.Objects::nonNull) // Filter out roles not found (e.g., inactive or wrong userCase)
                                                    .collect(Collectors.toList());
                                        }
                                        dto.setRoles(userSpecificRoles);
                                        return dto;
                                    })
                    );
                });
    }
}