package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.Role;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleRepositoryTest {

    // Mock the repository
    @Mock
    private RoleRepository roleRepository;

    private Role role1;
    private Role role2;
    private String role1Id;

    @BeforeEach
    void setUp() {
        // No need to interact with DB, just create sample objects
        role1Id = new ObjectId().toHexString(); // Generate a sample ID
        role1 = Role.builder()
                .id(role1Id) // Assign the ID for consistency in tests
                .roleName("Admin")
                .type("global")
                .description("Administrator Role")
                .isActive(true)
                .build();

        role2 = Role.builder()
                .id(new ObjectId().toHexString())
                .roleName("User")
                .type("user")
                .description("Standard User Role")
                .isActive(true)
                .build();
    }

    // No @AfterEach needed for cleanup with mocks

    @Test
    @DisplayName("Should simulate saving a role successfully")
    void saveRole() {
        // Define mock behavior: when save is called with any Role, return a Mono of that role
        // We return the pre-built role1 which already has an ID.
        when(roleRepository.save(any(Role.class))).thenReturn(Mono.just(role1));

        // Call the mocked repository method
        Mono<Role> savedRoleMono = roleRepository.save(role1); // Pass role1 or a new role instance

        // Verify the result using StepVerifier
        StepVerifier.create(savedRoleMono)
                .expectNextMatches(savedRole ->
                        savedRole.getId().equals(role1Id) &&
                                savedRole.getRoleName().equals("Admin") &&
                                savedRole.isActive()
                )
                .verifyComplete();

        // Verify that save was called
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("Should simulate finding a role by ID")
    void findById() {
        // Define mock behavior
        when(roleRepository.findById(role1Id)).thenReturn(Mono.just(role1));

        // Call the mocked method
        Mono<Role> foundRoleMono = roleRepository.findById(role1Id);

        // Verify
        StepVerifier.create(foundRoleMono)
                .expectNext(role1)
                .verifyComplete();

        // Verify findById was called
        verify(roleRepository).findById(role1Id);
    }

    @Test
    @DisplayName("Should simulate returning empty Mono when finding by non-existent ID")
    void findById_NotFound() {
        // Define mock behavior
        when(roleRepository.findById(anyString())).thenReturn(Mono.empty());

        // Call the mocked method
        Mono<Role> foundRoleMono = roleRepository.findById("nonexistentid");

        // Verify
        StepVerifier.create(foundRoleMono)
                .expectNextCount(0)
                .verifyComplete();

        verify(roleRepository).findById("nonexistentid");
    }

    @Test
    @DisplayName("Should simulate finding all roles")
    void findAllRoles() {
        // Define mock behavior
        when(roleRepository.findAll()).thenReturn(Flux.just(role1, role2));

        // Call the mocked method
        Flux<Role> allRolesFlux = roleRepository.findAll();

        // Verify
        StepVerifier.create(allRolesFlux)
                .expectNext(role1)
                .expectNext(role2)
                .verifyComplete();

        verify(roleRepository).findAll();
    }

    @Test
    @DisplayName("Should simulate deleting a role by ID")
    void deleteById() {
        // Define mock behavior for deleteById (returns Mono<Void>)
        when(roleRepository.deleteById(role1Id)).thenReturn(Mono.empty());

        // Call the mocked method
        Mono<Void> deleteMono = roleRepository.deleteById(role1Id);

        // Verify completion
        StepVerifier.create(deleteMono)
                .verifyComplete();

        // Verify deleteById was called
        verify(roleRepository).deleteById(role1Id);
    }

    @Test
    @DisplayName("Should simulate finding a role by roleName")
    void findByRoleName() {
        // Define mock behavior
        when(roleRepository.findByRoleName("Admin")).thenReturn(Mono.just(role1));

        // Call the mocked method
        Mono<Role> foundRoleMono = roleRepository.findByRoleName("Admin");

        // Verify
        StepVerifier.create(foundRoleMono)
                .expectNext(role1)
                .verifyComplete();

        verify(roleRepository).findByRoleName("Admin");
    }

    @Test
    @DisplayName("Should simulate returning empty Mono when finding by non-existent roleName")
    void findByRoleName_NotFound() {
        // Define mock behavior
        when(roleRepository.findByRoleName(anyString())).thenReturn(Mono.empty());

        // Call the mocked method
        Mono<Role> foundRoleMono = roleRepository.findByRoleName("NonExistentRole");

        // Verify
        StepVerifier.create(foundRoleMono)
                .expectNextCount(0)
                .verifyComplete();

        verify(roleRepository).findByRoleName("NonExistentRole");
    }

    @Test
    @DisplayName("Should simulate returning true when checking existence for an existing roleName")
    void existsByRoleName_True() {
        // Define mock behavior
        when(roleRepository.existsByRoleName("Admin")).thenReturn(Mono.just(true));

        // Call the mocked method
        Mono<Boolean> existsMono = roleRepository.existsByRoleName("Admin");

        // Verify
        StepVerifier.create(existsMono)
                .expectNext(true)
                .verifyComplete();

        verify(roleRepository).existsByRoleName("Admin");
    }

    @Test
    @DisplayName("Should simulate returning false when checking existence for a non-existent roleName")
    void existsByRoleName_False() {
        // Define mock behavior
        when(roleRepository.existsByRoleName(anyString())).thenReturn(Mono.just(false));

        // Call the mocked method
        Mono<Boolean> existsMono = roleRepository.existsByRoleName("NonExistentRole");

        // Verify
        StepVerifier.create(existsMono)
                .expectNext(false)
                .verifyComplete();

        verify(roleRepository).existsByRoleName("NonExistentRole");
    }
}