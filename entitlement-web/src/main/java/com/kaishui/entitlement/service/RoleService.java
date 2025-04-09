package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.Role;
import com.kaishui.entitlement.exception.CommonException; // Assuming you have this
import com.kaishui.entitlement.repository.RoleRepository;
import com.kaishui.entitlement.util.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    /**
     * Get all active roles.
     */
    public Flux<Role> getAllRoles() {
        log.info("Fetching all active roles");
        return roleRepository.findAll().filter(Role::isActive); // Filter for active roles
    }

    /**
     * Get a single role by its ID.
     */
    public Mono<Role> getRoleById(String id) {
        log.info("Fetching role by id: {}", id);
        return roleRepository.findById(id)
                .filter(Role::isActive) // Ensure role is active
                .switchIfEmpty(Mono.error(new CommonException("Active role not found with id: " + id)));
    }

    /**
     * Create a new role.
     */
    public Mono<Role> createRole(Role role) {
        log.info("Attempting to create role: {}", role.getRoleName());
        // Use deferContextual to get the username for 'createdBy'
        return Mono.deferContextual(contextView -> {
            String createdByUsername = AuthorizationUtil.extractUsernameFromContext(contextView);

            // Check if role name already exists
            return roleRepository.existsByRoleName(role.getRoleName())
                    .flatMap(exists -> {
                        if (exists) {
                            log.warn("Role with name '{}' already exists.", role.getRoleName());
                            return Mono.error(new CommonException("Role with name '" + role.getRoleName() + "' already exists."));
                        }
                        // Set audit fields and defaults for new role
                        role.setId(null); // Ensure ID is null for creation
                        role.setCreatedBy(createdByUsername);
                        role.setCreatedDate(new Date());
                        role.setUpdatedBy(null);
                        role.setLastModifiedDate(null);
                        role.setActive(true); // Ensure active on creation
                        // isApprover defaults from builder/entity

                        log.info("Saving new role: {}", role.getRoleName());
                        return roleRepository.save(role);
                    });
        });
    }

    /**
     * Update an existing role.
     */
    public Mono<Role> updateRole(String id, Role roleUpdateData) {
        log.info("Attempting to update role with id: {}", id);
        // Use deferContextual to get the username for 'updatedBy'
        return Mono.deferContextual(contextView -> {
            String updatedByUsername = AuthorizationUtil.extractUsernameFromContext(contextView);

            return roleRepository.findById(id)
                    .flatMap(existingRole -> {
                        if (!existingRole.isActive()) {
                            log.warn("Attempted to update inactive role with id: {}", id);
                            return Mono.error(new CommonException("Cannot update inactive role with id: " + id));
                        }

                        // Check for role name conflict if name is being changed
                        Mono<Role> updateMono = Mono.just(existingRole);
                        if (roleUpdateData.getRoleName() != null && !roleUpdateData.getRoleName().equals(existingRole.getRoleName())) {
                            updateMono = roleRepository.existsByRoleName(roleUpdateData.getRoleName())
                                    .flatMap(exists -> {
                                        if (exists) {
                                            log.warn("Cannot update role id '{}': new name '{}' already exists.", id, roleUpdateData.getRoleName());
                                            return Mono.error(new CommonException("Role name '" + roleUpdateData.getRoleName() + "' already exists."));
                                        }
                                        existingRole.setRoleName(roleUpdateData.getRoleName());
                                        return Mono.just(existingRole);
                                    });
                        }

                        return updateMono.flatMap(roleToUpdate -> {
                            mergeRoleInfo(roleUpdateData, roleToUpdate, updatedByUsername);
                            log.info("Saving updated role data for id: {}", id);
                            return roleRepository.save(roleToUpdate);
                        });
                    })
                    .switchIfEmpty(Mono.error(new CommonException("Role not found for update with id: " + id)));
        });
    }

    private static void mergeRoleInfo(Role roleUpdateData, Role roleToUpdate, String updatedByUsername) {
        // Update fields from roleUpdateData onto existingRole
        if (roleUpdateData.getDescription() != null && !roleUpdateData.getDescription().equals(roleToUpdate.getDescription())) {
            roleToUpdate.setDescription(roleUpdateData.getDescription());
        }
        if (roleUpdateData.getType() != null && !roleUpdateData.getType().equals(roleToUpdate.getType())) {
            roleToUpdate.setType(roleUpdateData.getType());
        }
        if (roleUpdateData.getIsApprover() != null && !roleUpdateData.getIsApprover().equals(roleToUpdate.getIsApprover())) {
            roleToUpdate.setIsApprover(roleUpdateData.getIsApprover());
        }
        if (roleUpdateData.getResourceIds() != null && !roleUpdateData.getResourceIds().equals(roleToUpdate.getResourceIds())) {
            // Consider how to handle list updates (replace, merge?) - this replaces
            roleToUpdate.setResourceIds(roleUpdateData.getResourceIds());
        }
        // Do NOT update: id, createdBy, createdDate, isActive (handled by delete)

        roleToUpdate.setUpdatedBy(updatedByUsername);
        roleToUpdate.setLastModifiedDate(new Date());
    }

    /**
     * Delete a role (soft delete by setting isActive=false).
     */
    public Mono<Void> deleteRole(String id) {
        log.info("Attempting to soft delete role with id: {}", id);
        // Use deferContextual to get the username for 'updatedBy'
        return Mono.deferContextual(contextView -> {
            String deletedByUsername = AuthorizationUtil.extractUsernameFromContext(contextView);

            return roleRepository.findById(id)
                    .switchIfEmpty(Mono.error(new CommonException("Role not found for deletion with id: " + id)))
                    .flatMap(role -> {
                        if (!role.isActive()) {
                            log.info("Role with id: {} is already inactive.", id);
                            return Mono.empty(); // Already inactive, nothing to do
                        }
                        // ---> This line sets isActive to false <---
                        role.setActive(false);
                        role.setUpdatedBy(deletedByUsername); // Mark who deactivated it
                        role.setLastModifiedDate(new Date());
                        log.info("Setting role with id: {} to inactive.", id);
                        // ---> This line saves the updated role (with isActive=false) <---
                        return roleRepository.save(role);
                    })
                    .then(); // Convert Mono<Role> to Mono<Void>
        });
    }
}