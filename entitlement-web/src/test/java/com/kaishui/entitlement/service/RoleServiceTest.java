package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.Role;
import com.kaishui.entitlement.exception.CommonException;
import com.kaishui.entitlement.repository.RoleRepository;
import com.kaishui.entitlement.util.AuthorizationUtil;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    private MockedStatic<AuthorizationUtil> mockedAuthorizationUtil;

    private Role activeRole1;
    private Role activeRole2;
    private Role inactiveRole;
    private final String testUsername = "testUser";
    private final String roleId1 = new ObjectId().toHexString();
    private final String roleId2 = new ObjectId().toHexString();
    private final String inactiveRoleId = new ObjectId().toHexString();


    @BeforeEach
    void setUp() {
        activeRole1 = Role.builder()
                .id(roleId1)
                .roleName("Admin")
                .type("global")
                .description("Admin Role")
                .isActive(true)
                .createdBy("creator")
                .createdDate(new Date())
                .build();

        activeRole2 = Role.builder()
                .id(roleId2)
                .roleName("User")
                .type("user")
                .description("User Role")
                .isActive(true)
                .createdBy("creator")
                .createdDate(new Date())
                .build();

        inactiveRole = Role.builder()
                .id(inactiveRoleId)
                .roleName("Inactive")
                .type("system")
                .description("Inactive Role")
                .isActive(false)
                .createdBy("creator")
                .createdDate(new Date())
                .build();

        // Mock the static method AuthorizationUtil.extractUsernameFromContext
        mockedAuthorizationUtil = Mockito.mockStatic(AuthorizationUtil.class);
        mockedAuthorizationUtil.when(() -> AuthorizationUtil.extractUsernameFromContext(any(Context.class)))
                .thenReturn(testUsername);
        // Also mock the ContextView variant if used internally by your actual util or tests
        mockedAuthorizationUtil.when(() -> AuthorizationUtil.extractUsernameFromContext(any(reactor.util.context.ContextView.class)))
                .thenReturn(testUsername);
    }

    @AfterEach
    void tearDown() {
        // Close the static mock
        mockedAuthorizationUtil.close();
    }

    @Test
    @DisplayName("getAllRoles should return only active roles")
    void getAllRoles_ReturnsActiveOnly() {
        when(roleRepository.findAll()).thenReturn(Flux.just(activeRole1, activeRole2, inactiveRole));

        StepVerifier.create(roleService.getAllRoles())
                .expectNext(activeRole1)
                .expectNext(activeRole2)
                .verifyComplete();

        verify(roleRepository).findAll();
    }

    @Test
    @DisplayName("getRoleById should return active role when found")
    void getRoleById_FoundActive() {
        when(roleRepository.findById(roleId1)).thenReturn(Mono.just(activeRole1));

        StepVerifier.create(roleService.getRoleById(roleId1))
                .expectNext(activeRole1)
                .verifyComplete();

        verify(roleRepository).findById(roleId1);
    }

    @Test
    @DisplayName("getRoleById should return error when role is inactive")
    void getRoleById_FoundInactive() {
        when(roleRepository.findById(inactiveRoleId)).thenReturn(Mono.just(inactiveRole));

        StepVerifier.create(roleService.getRoleById(inactiveRoleId))
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("Active role not found with id: " + inactiveRoleId))
                .verify();

        verify(roleRepository).findById(inactiveRoleId);
    }

    @Test
    @DisplayName("getRoleById should return error when role not found")
    void getRoleById_NotFound() {
        when(roleRepository.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(roleService.getRoleById("nonexistent"))
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("Active role not found with id: nonexistent"))
                .verify();

        verify(roleRepository).findById("nonexistent");
    }

    @Test
    @DisplayName("createRole should succeed when role name is unique")
    void createRole_Success() {
        Role newRole = Role.builder().roleName("NewRole").description("Desc").build();
        Role savedRole = Role.builder()
                .id(new ObjectId().toHexString())
                .roleName("NewRole")
                .description("Desc")
                .createdBy(testUsername)
                .createdDate(new Date()) // Date will differ slightly, check existence
                .isActive(true)
                .build();

        when(roleRepository.existsByRoleName("NewRole")).thenReturn(Mono.just(false));
        // Use argThat to capture the role being saved and verify its fields
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role roleToSave = invocation.getArgument(0);
            // Simulate saving by assigning an ID and returning
            roleToSave.setId(savedRole.getId()); // Use the predefined ID for assertion
            roleToSave.setCreatedDate(savedRole.getCreatedDate()); // Use predefined date
            return Mono.just(roleToSave);
        });


        // Provide context for deferContextual
        Mono<Role> result = roleService.createRole(newRole)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername)); // Simulate context population

        StepVerifier.create(result)
                .expectNextMatches(r -> r.getId() != null &&
                        r.getRoleName().equals("NewRole") &&
                        r.getCreatedBy().equals(testUsername) &&
                        r.getCreatedDate() != null &&
                        r.isActive() &&
                        r.getUpdatedBy() == null &&
                        r.getLastModifiedDate() == null)
                .verifyComplete();

        verify(roleRepository).existsByRoleName("NewRole");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("createRole should fail when role name already exists")
    void createRole_NameConflict() {
        Role newRole = Role.builder().roleName("Admin").description("Desc").build();

        when(roleRepository.existsByRoleName("Admin")).thenReturn(Mono.just(true));

        // Provide context for deferContextual
        Mono<Role> result = roleService.createRole(newRole)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("Role with name 'Admin' already exists."))
                .verify();

        verify(roleRepository).existsByRoleName("Admin");
        verify(roleRepository, never()).save(any(Role.class));
    }


    @Test
    @DisplayName("updateRole should succeed for active role with valid data")
    void updateRole_Success() {
        Role updateData = Role.builder()
                .roleName("AdminUpdated") // Change name
                .description("Updated Desc")
                .isApprover(true)
                .resourceIds(List.of("res1", "res2"))
                .build();

        // Make a copy to avoid modifying the original mock object state
        Role existingRoleCopy = Role.builder()
                .id(activeRole1.getId())
                .roleName(activeRole1.getRoleName())
                .type(activeRole1.getType())
                .description(activeRole1.getDescription())
                .isActive(activeRole1.isActive())
                .createdBy(activeRole1.getCreatedBy())
                .createdDate(activeRole1.getCreatedDate())
                .isApprover(activeRole1.getIsApprover())
                .resourceIds(activeRole1.getResourceIds())
                .build();


        when(roleRepository.findById(roleId1)).thenReturn(Mono.just(existingRoleCopy));
        when(roleRepository.existsByRoleName("AdminUpdated")).thenReturn(Mono.just(false)); // New name is unique
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0))); // Return saved entity

        Mono<Role> result = roleService.updateRole(roleId1, updateData)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        StepVerifier.create(result)
                .expectNextMatches(updatedRole ->
                        updatedRole.getId().equals(roleId1) &&
                                updatedRole.getRoleName().equals("AdminUpdated") && // Name updated
                                updatedRole.getDescription().equals("Updated Desc") && // Desc updated
                                updatedRole.getIsApprover().equals(true) && // Approver updated
                                updatedRole.getResourceIds().equals(List.of("res1", "res2")) && // Resources updated
                                updatedRole.getUpdatedBy().equals(testUsername) &&
                                updatedRole.getLastModifiedDate() != null &&
                                updatedRole.getCreatedBy().equals(activeRole1.getCreatedBy()) // CreatedBy unchanged
                )
                .verifyComplete();

        verify(roleRepository).findById(roleId1);
        verify(roleRepository).existsByRoleName("AdminUpdated");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("updateRole should succeed when only non-name fields change")
    void updateRole_Success_NoNameChange() {
        Role updateData = Role.builder()
                .roleName("Admin") // Same name
                .description("Updated Desc Only")
                .build();

        // Make a copy to avoid modifying the original mock object state
        Role existingRoleCopy = Role.builder()
                .id(activeRole1.getId())
                .roleName(activeRole1.getRoleName())
                .type(activeRole1.getType())
                .description(activeRole1.getDescription())
                .isActive(activeRole1.isActive())
                .createdBy(activeRole1.getCreatedBy())
                .createdDate(activeRole1.getCreatedDate())
                .build();

        when(roleRepository.findById(roleId1)).thenReturn(Mono.just(existingRoleCopy));
        // existsByRoleName should NOT be called if name doesn't change
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<Role> result = roleService.updateRole(roleId1, updateData)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        StepVerifier.create(result)
                .expectNextMatches(updatedRole ->
                        updatedRole.getDescription().equals("Updated Desc Only") &&
                                updatedRole.getUpdatedBy().equals(testUsername) &&
                                updatedRole.getLastModifiedDate() != null
                )
                .verifyComplete();

        verify(roleRepository).findById(roleId1);
        verify(roleRepository, never()).existsByRoleName(anyString()); // Should not be called
        verify(roleRepository).save(any(Role.class));
    }


    @Test
    @DisplayName("updateRole should fail when role not found")
    void updateRole_NotFound() {
        Role updateData = Role.builder().description("Update").build();
        when(roleRepository.findById("nonexistent")).thenReturn(Mono.empty());

        Mono<Role> result = roleService.updateRole("nonexistent", updateData)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("Role not found for update with id: nonexistent"))
                .verify();

        verify(roleRepository).findById("nonexistent");
        verify(roleRepository, never()).existsByRoleName(anyString());
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("updateRole should fail when role is inactive")
    void updateRole_Inactive() {
        Role updateData = Role.builder().description("Update").build();
        when(roleRepository.findById(inactiveRoleId)).thenReturn(Mono.just(inactiveRole));

        Mono<Role> result = roleService.updateRole(inactiveRoleId, updateData)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("Cannot update inactive role with id: " + inactiveRoleId))
                .verify();

        verify(roleRepository).findById(inactiveRoleId);
        verify(roleRepository, never()).existsByRoleName(anyString());
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("updateRole should fail when new role name conflicts")
    void updateRole_NameConflict() {
        Role updateData = Role.builder().roleName("User").description("Update").build(); // Try to use existing 'User' name

        // Make a copy to avoid modifying the original mock object state
        Role existingRoleCopy = Role.builder()
                .id(activeRole1.getId())
                .roleName(activeRole1.getRoleName()) // Original name is 'Admin'
                .type(activeRole1.getType())
                .description(activeRole1.getDescription())
                .isActive(activeRole1.isActive())
                .createdBy(activeRole1.getCreatedBy())
                .createdDate(activeRole1.getCreatedDate())
                .build();

        when(roleRepository.findById(roleId1)).thenReturn(Mono.just(existingRoleCopy));
        when(roleRepository.existsByRoleName("User")).thenReturn(Mono.just(true)); // The target name 'User' exists

        Mono<Role> result = roleService.updateRole(roleId1, updateData)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("Role name 'User' already exists."))
                .verify();

        verify(roleRepository).findById(roleId1);
        verify(roleRepository).existsByRoleName("User");
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("deleteRole should soft delete active role")
    void deleteRole_SuccessActive() {
        // Make a copy to avoid modifying the original mock object state
        Role existingRoleCopy = Role.builder()
                .id(activeRole1.getId())
                .roleName(activeRole1.getRoleName())
                .type(activeRole1.getType())
                .description(activeRole1.getDescription())
                .isActive(true) // Ensure it starts active
                .createdBy(activeRole1.getCreatedBy())
                .createdDate(activeRole1.getCreatedDate())
                .build();

        when(roleRepository.findById(roleId1)).thenReturn(Mono.just(existingRoleCopy));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<Void> result = roleService.deleteRole(roleId1)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        StepVerifier.create(result)
                .verifyComplete();

        verify(roleRepository).findById(roleId1);
        verify(roleRepository).save(argThat(role ->
                !role.isActive() && // Check if inactive
                        role.getUpdatedBy().equals(testUsername) &&
                        role.getLastModifiedDate() != null
        ));
    }

    @Test
    @DisplayName("deleteRole should do nothing for already inactive role")
    void deleteRole_AlreadyInactive() {
        when(roleRepository.findById(inactiveRoleId)).thenReturn(Mono.just(inactiveRole)); // Return the inactive role

        Mono<Void> result = roleService.deleteRole(inactiveRoleId)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        StepVerifier.create(result)
                .verifyComplete(); // Should complete without error and without saving

        verify(roleRepository).findById(inactiveRoleId);
        verify(roleRepository, never()).save(any(Role.class)); // Save should not be called
    }

    @Test
    @DisplayName("deleteRole should fail when role not found")
    void deleteRole_NotFound() {
        when(roleRepository.findById("nonexistent")).thenReturn(Mono.empty());

        Mono<Void> result = roleService.deleteRole("nonexistent")
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("Role not found for deletion with id: nonexistent"))
                .verify();

        verify(roleRepository).findById("nonexistent");
        verify(roleRepository, never()).save(any(Role.class));
    }
}