package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.GroupDefaultRole;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*; // Keep assertions

@ExtendWith(MockitoExtension.class)
class GroupDefaultRoleRepositoryTest {

    @Mock
    private GroupDefaultRoleRepository groupDefaultRoleRepository;

    private GroupDefaultRole groupRole1;
    private GroupDefaultRole groupRole2;
    private String groupRole1Id;

    @BeforeEach
    void setUp() {
        groupRole1Id = new ObjectId().toHexString();
        groupRole1 = GroupDefaultRole.builder()
                .id(groupRole1Id)
                .groupName("Admins")
                .roleIds(List.of("ROLE_ADMIN", "ROLE_USER"))
                .build();

        groupRole2 = GroupDefaultRole.builder()
                .id(new ObjectId().toHexString())
                .groupName("Users")
                .roleIds(List.of("ROLE_USER"))
                .build();
    }

    @Test
    @DisplayName("Should simulate saving a GroupDefaultRole successfully")
    void saveGroupDefaultRole() {
        // Arrange
        when(groupDefaultRoleRepository.save(any(GroupDefaultRole.class))).thenReturn(Mono.just(groupRole1));

        // Act
        Mono<GroupDefaultRole> saveMono = groupDefaultRoleRepository.save(groupRole1);

        // Assert
        StepVerifier.create(saveMono)
                .assertNext(savedGroupRole -> {
                    assertEquals(groupRole1Id, savedGroupRole.getId());
                    assertEquals("Admins", savedGroupRole.getGroupName());
                    assertEquals(List.of("ROLE_ADMIN", "ROLE_USER"), savedGroupRole.getRoleIds());
                })
                .verifyComplete();

        // Verify
        verify(groupDefaultRoleRepository).save(any(GroupDefaultRole.class));
    }

    @Test
    @DisplayName("Should simulate finding a GroupDefaultRole by ID")
    void findById() {
        // Arrange
        when(groupDefaultRoleRepository.findById(groupRole1Id)).thenReturn(Mono.just(groupRole1));

        // Act
        Mono<GroupDefaultRole> findMono = groupDefaultRoleRepository.findById(groupRole1Id);

        // Assert
        StepVerifier.create(findMono)
                .expectNext(groupRole1)
                .verifyComplete();

        // Verify
        verify(groupDefaultRoleRepository).findById(groupRole1Id);
    }

    @Test
    @DisplayName("Should simulate returning empty Mono when finding by non-existent ID")
    void findById_NotFound() {
        // Arrange
        String nonExistentId = new ObjectId().toHexString();
        when(groupDefaultRoleRepository.findById(nonExistentId)).thenReturn(Mono.empty());

        // Act
        Mono<GroupDefaultRole> findMono = groupDefaultRoleRepository.findById(nonExistentId);

        // Assert
        StepVerifier.create(findMono)
                .expectNextCount(0)
                .verifyComplete();

        // Verify
        verify(groupDefaultRoleRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should simulate finding all GroupDefaultRoles")
    void findAllGroupDefaultRoles() {
        // Arrange
        when(groupDefaultRoleRepository.findAll()).thenReturn(Flux.just(groupRole1, groupRole2));

        // Act
        Flux<GroupDefaultRole> findAllFlux = groupDefaultRoleRepository.findAll();

        // Assert
        StepVerifier.create(findAllFlux)
                .expectNext(groupRole1)
                .expectNext(groupRole2)
                .verifyComplete();

        // Verify
        verify(groupDefaultRoleRepository).findAll();
    }

    @Test
    @DisplayName("Should simulate deleting a GroupDefaultRole by ID")
    void deleteById() {
        // Arrange
        when(groupDefaultRoleRepository.deleteById(groupRole1Id)).thenReturn(Mono.empty()); // Returns Mono<Void>

        // Act
        Mono<Void> deleteMono = groupDefaultRoleRepository.deleteById(groupRole1Id);

        // Assert
        StepVerifier.create(deleteMono)
                .verifyComplete();

        // Verify
        verify(groupDefaultRoleRepository).deleteById(groupRole1Id);
    }

    // --- Test for Custom Method ---

    @Test
    @DisplayName("Should simulate finding GroupDefaultRoles by group names")
    void findByGroupNameIn_Found() {
        // Arrange
        List<String> groupNamesToFind = Arrays.asList("Admins", "Users");
        when(groupDefaultRoleRepository.findByGroupNameIn(groupNamesToFind)).thenReturn(Flux.just(groupRole1, groupRole2));

        // Act
        Flux<GroupDefaultRole> findFlux = groupDefaultRoleRepository.findByGroupNameIn(groupNamesToFind);

        // Assert
        StepVerifier.create(findFlux)
                .expectNext(groupRole1)
                .expectNext(groupRole2)
                .verifyComplete();

        // Verify
        verify(groupDefaultRoleRepository).findByGroupNameIn(groupNamesToFind);
    }

    @Test
    @DisplayName("Should simulate finding GroupDefaultRoles by group names when only some match")
    void findByGroupNameIn_PartialMatch() {
        // Arrange
        List<String> groupNamesToFind = Arrays.asList("Admins", "NonExistentGroup");
        when(groupDefaultRoleRepository.findByGroupNameIn(groupNamesToFind)).thenReturn(Flux.just(groupRole1)); // Only groupRole1 matches

        // Act
        Flux<GroupDefaultRole> findFlux = groupDefaultRoleRepository.findByGroupNameIn(groupNamesToFind);

        // Assert
        StepVerifier.create(findFlux)
                .expectNext(groupRole1)
                .verifyComplete();

        // Verify
        verify(groupDefaultRoleRepository).findByGroupNameIn(groupNamesToFind);
    }

    @Test
    @DisplayName("Should simulate returning empty Flux when finding by non-existent group names")
    void findByGroupNameIn_NotFound() {
        // Arrange
        List<String> groupNamesToFind = Arrays.asList("NonExistentGroup1", "NonExistentGroup2");
        when(groupDefaultRoleRepository.findByGroupNameIn(groupNamesToFind)).thenReturn(Flux.empty());

        // Act
        Flux<GroupDefaultRole> findFlux = groupDefaultRoleRepository.findByGroupNameIn(groupNamesToFind);

        // Assert
        StepVerifier.create(findFlux)
                .expectNextCount(0)
                .verifyComplete();

        // Verify
        verify(groupDefaultRoleRepository).findByGroupNameIn(groupNamesToFind);
    }

    @Test
    @DisplayName("Should simulate finding by empty list of group names")
    void findByGroupNameIn_EmptyList() {
        // Arrange
        List<String> emptyList = Collections.emptyList();
        // The actual repository might return empty or throw an error depending on implementation.
        // Mocking it to return empty is a reasonable assumption for an empty 'IN' clause.
        when(groupDefaultRoleRepository.findByGroupNameIn(emptyList)).thenReturn(Flux.empty());

        // Act
        Flux<GroupDefaultRole> findFlux = groupDefaultRoleRepository.findByGroupNameIn(emptyList);

        // Assert
        StepVerifier.create(findFlux)
                .expectNextCount(0)
                .verifyComplete();

        // Verify
        verify(groupDefaultRoleRepository).findByGroupNameIn(emptyList);
    }
}