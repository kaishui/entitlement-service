package com.kaishui.entitlement.web;

import com.kaishui.entitlement.annotation.AuditLog;
import com.kaishui.entitlement.entity.Role;
import com.kaishui.entitlement.entity.User;
import com.kaishui.entitlement.entity.dto.UserDto;
import com.kaishui.entitlement.service.UserService;
import com.kaishui.entitlement.util.AuthorizationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/api/users")
@Tag(name = "User", description = "User API")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthorizationUtil authorizationUtil;

    @Operation(summary = "Get all users", responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class)))
    })
    @GetMapping
    public Flux<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @Operation(summary = "Get user by ID", responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<User>> getUserById(@Parameter(description = "ID of the user to get", required = true) @PathVariable String id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new user", responses = {
            @ApiResponse(responseCode = "201", description = "User created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class)))
    })
    @PostMapping
    public Mono<ResponseEntity<UserDto>> createUser(@Valid @RequestBody User user) { // Add @Valid
        return userService.insertOrUpdateUser(user)
                .flatMap(userService::processFirstLogin) // Process first login
                .flatMap(userService::getRolesAndPermissionsByUser)
                .map(createdUser -> ResponseEntity.status(HttpStatus.CREATED).body(createdUser))
                .onErrorResume(DuplicateKeyException.class, e -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()));
    }

    @Operation(summary = "Update an existing user", responses = {
            @ApiResponse(responseCode = "200", description = "User updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping
    @AuditLog(action = "Update User", detail = "Updated user details")
    public Mono<ResponseEntity<User>> updateUser(@Valid @RequestBody User user) { // Add @Valid
        return userService.updateUser(user)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(DuplicateKeyException.class, e -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()));
    }

    @Operation(summary = "Delete a user", responses = {
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteUser(@Parameter(description = "ID of the user to delete", required = true) @PathVariable String id) {
        return userService.deleteUser(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @Operation(summary = "Find current user roles by user case ", responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Role.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/current/roles")
    public Flux<Role> findRolesByUserCase(
            @Parameter(description = "User Case", required = true) @RequestParam String userCase, ServerHttpRequest request) {
        String staffId = authorizationUtil.getStaffIdFromToken(request);
        return userService.findRolesByUserCase(userCase, staffId);
    }

    @Operation(summary = "Find current user roles by user case ", responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Role.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/next/users")
    public Flux<UserDto> getNextLevelUser(
            @Parameter(description = "User Case", required = true) @RequestParam String userCase, ServerHttpRequest request) {
        String staffId = authorizationUtil.getStaffIdFromToken(request);
        return userService.getNextLevelUser(userCase, staffId);
    }


}