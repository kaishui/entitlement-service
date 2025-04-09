package com.kaishui.entitlement.web;

import com.kaishui.entitlement.annotation.AuditLog;
import com.kaishui.entitlement.entity.Role;
import com.kaishui.entitlement.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid; // For request body validation
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/api/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "APIs for managing roles")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Get all active roles")
    @AuditLog(action = "GET_ALL_ROLES")
    public Flux<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a role by ID")
    @AuditLog(action = "GET_ROLE_BY_ID")
    public Mono<ResponseEntity<Role>> getRoleById(@PathVariable String id) {
        return roleService.getRoleById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Set default success status to 201
    @Operation(summary = "Create a new role")
    @AuditLog(action = "CREATE_ROLE")
    public Mono<Role> createRole(@Valid @RequestBody Role role) { // Add @Valid
        return roleService.createRole(role);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing role")
    @AuditLog(action = "UPDATE_ROLE")
    public Mono<ResponseEntity<Role>> updateRole(@PathVariable String id, @Valid @RequestBody Role role) { // Add @Valid
        return roleService.updateRole(id, role)
                .map(ResponseEntity::ok)
                // If updateRole returns error for not found, this won't be hit,
                // but good practice for findById scenarios.
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Set default success status to 204
    @Operation(summary = "Delete a role (soft delete)")
    @AuditLog(action = "DELETE_ROLE")
    public Mono<Void> deleteRole(@PathVariable String id) {
        return roleService.deleteRole(id);
    }
}
