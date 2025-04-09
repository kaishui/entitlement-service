package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.GroupDefaultRole;
import com.kaishui.entitlement.entity.User;
import com.kaishui.entitlement.exception.CommonException;
import com.kaishui.entitlement.repository.GroupDefaultRoleRepository;
import com.kaishui.entitlement.repository.UserRepository;
import com.kaishui.entitlement.util.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
            String updatedByUsername = AuthorizationUtil.extractUsernameFromContext(contextView);

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
        }); // End of deferContextual
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
        existingUser.setUpdatedBy(updatedByUsername); // Set the user who performed the update
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
            String deletedByUsername = AuthorizationUtil.extractUsernameFromContext(contextView);

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
                        user.setUpdatedBy(deletedByUsername);
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
                            .updatedBy(user.getUpdatedBy())
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
}