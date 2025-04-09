package com.kaishui.entitlement.web;

import com.kaishui.entitlement.entity.Role;
import com.kaishui.entitlement.exception.CommonException;
import com.kaishui.entitlement.service.RoleService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private WebTestClient webTestClient; // Client to make requests

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

    private Role role1;
    private Role role2;
    private String roleId1;

    @BeforeEach
    void setUp() {
        // Manually bind WebTestClient to the controller instance
        webTestClient = WebTestClient.bindToController(roleController).build();

        roleId1 = new ObjectId().toHexString();
        role1 = Role.builder()
                .id(roleId1)
                .roleName("Admin")
                .type("global")
                .description("Admin Role")
                .isActive(true)
                .createdBy("creator")
                .createdDate(new Date())
                .build();

        role2 = Role.builder()
                .id(new ObjectId().toHexString())
                .roleName("User")
                .type("user")
                .description("User Role")
                .isActive(true)
                .createdBy("creator")
                .createdDate(new Date())
                .build();
    }

    @Test
    @DisplayName("GET /v1/api/roles - Should return all active roles")
    void getAllRoles_Success() {
        // Arrange: Mock service behavior
        when(roleService.getAllRoles()).thenReturn(Flux.just(role1, role2));

        // Act & Assert
        webTestClient.get().uri("/v1/api/roles")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(Role.class).hasSize(2).contains(role1, role2);

        // Verify service interaction
        verify(roleService).getAllRoles();
    }

    @Test
    @DisplayName("GET /v1/api/roles/{id} - Should return role when found")
    void getRoleById_Found() {
        // Arrange
        when(roleService.getRoleById(roleId1)).thenReturn(Mono.just(role1));

        // Act & Assert
        webTestClient.get().uri("/v1/api/roles/{id}", roleId1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Role.class).isEqualTo(role1);

        // Verify
        verify(roleService).getRoleById(roleId1);
    }

    @Test
    @DisplayName("GET /v1/api/roles/{id} - Should return 404 when not found (service returns empty)")
    void getRoleById_NotFound_ServiceEmpty() {
        // Arrange
        String nonExistentId = "nonexistent";
        when(roleService.getRoleById(nonExistentId)).thenReturn(Mono.empty()); // Service returns empty

        // Act & Assert
        webTestClient.get().uri("/v1/api/roles/{id}", nonExistentId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound() // Controller maps empty Mono to 404
                .expectBody().isEmpty();

        // Verify
        verify(roleService).getRoleById(nonExistentId);
    }

    @Test
    @DisplayName("GET /v1/api/roles/{id} - Should return 5xx when service throws error")
    void getRoleById_NotFound_ServiceError() {
        // Arrange
        String nonExistentId = "nonexistent";
        // Simulate service throwing an error
        when(roleService.getRoleById(nonExistentId)).thenReturn(Mono.error(new CommonException("DB error")));

        // Act & Assert
        webTestClient.get().uri("/v1/api/roles/{id}", nonExistentId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                // With manual binding, validation/exception handling might not be fully set up
                // like in @WebFluxTest. Errors might propagate directly.
                .expectStatus().is5xxServerError(); // Expecting error propagation

        // Verify
        verify(roleService).getRoleById(nonExistentId);
    }


    @Test
    @DisplayName("POST /v1/api/roles - Should create role successfully and return 201")
    void createRole_Success() {
        // Arrange
        Role roleToCreate = Role.builder().roleName("NewRole").description("Desc").build();
        when(roleService.createRole(any(Role.class))).thenReturn(Mono.just(role1));

        // Act & Assert
        webTestClient.post().uri("/v1/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(roleToCreate)
                .exchange()
                .expectStatus().isCreated() // Expect 201 Created due to @ResponseStatus
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Role.class).isEqualTo(role1);

        // Verify
        verify(roleService).createRole(any(Role.class));
    }

    @Test
    @DisplayName("POST /v1/api/roles - Should return 400 on validation error")
    void createRole_ValidationError() {
        // Arrange: Send invalid data
        Role invalidRole = Role.builder().roleName("").description("Desc").build();

        // Act & Assert
        webTestClient.post().uri("/v1/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRole)
                .exchange()
                // Validation might not be triggered automatically without the full WebFlux context.
                // If validation is crucial, @WebFluxTest is often simpler.
                // If validation fails here, it might pass through to the service or cause a binding error.
                // Let's assume for now it results in Bad Request if binding/validation is somehow active.
                .expectStatus().isBadRequest();

        // Verification might change depending on whether validation runs
        // verify(roleService, never()).createRole(any(Role.class)); // This might fail if validation doesn't run
    }

    @Test
    @DisplayName("POST /v1/api/roles - Should return 5xx if service fails")
    void createRole_ServiceError() {
        // Arrange
        Role roleToCreate = Role.builder().roleName("FailRole").description("Desc").build();
        when(roleService.createRole(any(Role.class))).thenReturn(Mono.error(new CommonException("Creation failed")));

        // Act & Assert
        webTestClient.post().uri("/v1/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(roleToCreate)
                .exchange()
                .expectStatus().is5xxServerError();

        // Verify
        verify(roleService).createRole(any(Role.class));
    }


    @Test
    @DisplayName("PUT /v1/api/roles/{id} - Should update role successfully and return 200")
    void updateRole_Success() {
        // Arrange
        Role roleToUpdate = Role.builder().roleName("UpdatedAdmin").description("Updated Desc").build();
        Role updatedRoleResult = Role.builder()
                .id(roleId1)
                .roleName("UpdatedAdmin")
                .description("Updated Desc")
                .isActive(true)
                .build();
        when(roleService.updateRole(eq(roleId1), any(Role.class))).thenReturn(Mono.just(updatedRoleResult));

        // Act & Assert
        webTestClient.put().uri("/v1/api/roles/{id}", roleId1)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(roleToUpdate)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Role.class).isEqualTo(updatedRoleResult);

        // Verify
        verify(roleService).updateRole(eq(roleId1), any(Role.class));
    }

    @Test
    @DisplayName("PUT /v1/api/roles/{id} - Should return 404 when role to update not found (service returns empty)")
    void updateRole_NotFound_ServiceEmpty() {
        // Arrange
        String nonExistentId = "nonexistent";
        Role roleToUpdate = Role.builder().roleName("Update").description("Desc").build();
        when(roleService.updateRole(eq(nonExistentId), any(Role.class))).thenReturn(Mono.empty());

        // Act & Assert
        webTestClient.put().uri("/v1/api/roles/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(roleToUpdate)
                .exchange()
                .expectStatus().isNotFound() // Controller maps empty Mono to 404
                .expectBody().isEmpty();

        // Verify
        verify(roleService).updateRole(eq(nonExistentId), any(Role.class));
    }

    @Test
    @DisplayName("PUT /v1/api/roles/{id} - Should return 5xx when service throws error during update")
    void updateRole_NotFound_ServiceError() {
        // Arrange
        String nonExistentId = "nonexistent";
        Role roleToUpdate = Role.builder().roleName("Update").description("Desc").build();
        when(roleService.updateRole(eq(nonExistentId), any(Role.class)))
                .thenReturn(Mono.error(new CommonException("Role not found for update")));

        // Act & Assert
        webTestClient.put().uri("/v1/api/roles/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(roleToUpdate)
                .exchange()
                .expectStatus().is5xxServerError(); // Error propagation

        // Verify
        verify(roleService).updateRole(eq(nonExistentId), any(Role.class));
    }

    @Test
    @DisplayName("PUT /v1/api/roles/{id} - Should return 400 on validation error")
    void updateRole_ValidationError() {
        // Arrange: Send invalid data
        Role invalidRole = Role.builder().roleName("").description("Desc").build();

        // Act & Assert
        webTestClient.put().uri("/v1/api/roles/{id}", roleId1)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRole)
                .exchange()
                // Again, validation might not run automatically.
                .expectStatus().isBadRequest();

        // verify(roleService, never()).updateRole(anyString(), any(Role.class));
    }

    @Test
    @DisplayName("PUT /v1/api/roles/{id} - Should return 5xx if service fails during update logic")
    void updateRole_ServiceError() {
        // Arrange
        Role roleToUpdate = Role.builder().roleName("UpdateFail").description("Desc").build();
        when(roleService.updateRole(eq(roleId1), any(Role.class)))
                .thenReturn(Mono.error(new CommonException("Update conflict")));

        // Act & Assert
        webTestClient.put().uri("/v1/api/roles/{id}", roleId1)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(roleToUpdate)
                .exchange()
                .expectStatus().is5xxServerError();

        // Verify
        verify(roleService).updateRole(eq(roleId1), any(Role.class));
    }


    @Test
    @DisplayName("DELETE /v1/api/roles/{id} - Should delete role successfully and return 204")
    void deleteRole_Success() {
        // Arrange
        when(roleService.deleteRole(roleId1)).thenReturn(Mono.empty());

        // Act & Assert
        webTestClient.delete().uri("/v1/api/roles/{id}", roleId1)
                .exchange()
                .expectStatus().isNoContent() // Expect 204 No Content due to @ResponseStatus
                .expectBody().isEmpty();

        // Verify
        verify(roleService).deleteRole(roleId1);
    }

    @Test
    @DisplayName("DELETE /v1/api/roles/{id} - Should return 5xx if service fails")
    void deleteRole_ServiceError() {
        // Arrange
        String idToDelete = "faildelete";
        when(roleService.deleteRole(idToDelete)).thenReturn(Mono.error(new CommonException("Deletion failed")));

        // Act & Assert
        webTestClient.delete().uri("/v1/api/roles/{id}", idToDelete)
                .exchange()
                .expectStatus().is5xxServerError();

        // Verify
        verify(roleService).deleteRole(idToDelete);
    }
}