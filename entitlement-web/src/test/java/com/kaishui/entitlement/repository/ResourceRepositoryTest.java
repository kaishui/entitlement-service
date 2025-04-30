package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.Resource;
import org.bson.Document;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceRepositoryTest {

    // Mock the repository
    @Mock
    private ResourceRepository resourceRepository;

    private Resource resource1;
    private Resource resource2;
    private Resource inactiveResource;
    private String resource1Id;
    private String resource2Id;
    private String inactiveResourceId;

    @BeforeEach
    void setUp() {
        // Create sample Resource objects
        resource1Id = new ObjectId().toHexString();
        resource2Id = new ObjectId().toHexString();
        inactiveResourceId = new ObjectId().toHexString();

        resource1 = Resource.builder()
                .id(resource1Id)
                .name("Resource One")
                .type("page")
                .description("First Resource")
                .permission(new Document("action", "read"))
                .adGroups(List.of("group-a", "group-b"))
                .isActive(true)
                .build();

        resource2 = Resource.builder()
                .id(resource2Id)
                .name("Resource Two")
                .type("api")
                .description("Second Resource")
                .permission(new Document("method", "GET"))
                .adGroups(List.of("group-b", "group-c"))
                .isActive(true)
                .build();

        inactiveResource = Resource.builder()
                .id(inactiveResourceId)
                .name("Inactive Resource")
                .type("page")
                .description("This is inactive")
                .permission(new Document("action", "none"))
                .adGroups(List.of("group-d"))
                .isActive(false)
                .build();
    }

    @Test
    @DisplayName("Should simulate saving a resource successfully")
    void saveResource() {
        // Define mock behavior: when save is called, return the passed resource (or a copy)
        when(resourceRepository.save(any(Resource.class))).thenReturn(Mono.just(resource1));

        // Call the mocked repository method
        Mono<Resource> savedResourceMono = resourceRepository.save(resource1);

        // Verify the result
        StepVerifier.create(savedResourceMono)
                .expectNextMatches(saved -> saved.getId().equals(resource1Id) && saved.getName().equals("Resource One"))
                .verifyComplete();

        // Verify that save was called
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    @DisplayName("Should simulate finding a resource by ID")
    void findById_Found() {
        when(resourceRepository.findById(resource1Id)).thenReturn(Mono.just(resource1));

        Mono<Resource> foundResourceMono = resourceRepository.findById(resource1Id);

        StepVerifier.create(foundResourceMono)
                .expectNext(resource1)
                .verifyComplete();

        verify(resourceRepository).findById(resource1Id);
    }

    @Test
    @DisplayName("Should simulate returning empty Mono when finding by non-existent ID")
    void findById_NotFound() {
        when(resourceRepository.findById(anyString())).thenReturn(Mono.empty());

        Mono<Resource> foundResourceMono = resourceRepository.findById("nonexistentid");

        StepVerifier.create(foundResourceMono)
                .expectNextCount(0)
                .verifyComplete();

        verify(resourceRepository).findById("nonexistentid");
    }

    @Test
    @DisplayName("Should simulate finding all resources")
    void findAllResources() {
        when(resourceRepository.findAll()).thenReturn(Flux.just(resource1, resource2, inactiveResource));

        Flux<Resource> allResourcesFlux = resourceRepository.findAll();

        StepVerifier.create(allResourcesFlux)
                .expectNext(resource1)
                .expectNext(resource2)
                .expectNext(inactiveResource)
                .verifyComplete();

        verify(resourceRepository).findAll();
    }

    @Test
    @DisplayName("Should simulate deleting a resource by ID")
    void deleteById() {
        when(resourceRepository.deleteById(resource1Id)).thenReturn(Mono.empty());

        Mono<Void> deleteMono = resourceRepository.deleteById(resource1Id);

        StepVerifier.create(deleteMono)
                .verifyComplete();

        verify(resourceRepository).deleteById(resource1Id);
    }

    @Test
    @DisplayName("Should simulate finding resources by isActive status")
    void findByIsActive() {
        when(resourceRepository.findByIsActive(true)).thenReturn(Flux.just(resource1, resource2));
        when(resourceRepository.findByIsActive(false)).thenReturn(Flux.just(inactiveResource));

        // Test finding active
        Flux<Resource> activeResourcesFlux = resourceRepository.findByIsActive(true);
        StepVerifier.create(activeResourcesFlux)
                .expectNext(resource1)
                .expectNext(resource2)
                .verifyComplete();

        // Test finding inactive
        Flux<Resource> inactiveResourcesFlux = resourceRepository.findByIsActive(false);
        StepVerifier.create(inactiveResourcesFlux)
                .expectNext(inactiveResource)
                .verifyComplete();

        verify(resourceRepository).findByIsActive(true);
        verify(resourceRepository).findByIsActive(false);
    }

    @Test
    @DisplayName("Should simulate finding resources by type and active status")
    void findByTypeAndIsActive() {
        when(resourceRepository.findByTypeAndIsActive("page", true)).thenReturn(Flux.just(resource1));

        Flux<Resource> foundResourcesFlux = resourceRepository.findByTypeAndIsActive("page", true);

        StepVerifier.create(foundResourcesFlux)
                .expectNext(resource1)
                .verifyComplete();

        verify(resourceRepository).findByTypeAndIsActive("page", true);
    }

    // Note: findByRegion and findByRegionAndIsActive might have been replaced by findByAdGroupsIn...
    // Include tests based on the *current* state of your ResourceRepository interface.
    // Assuming findByAdGroupsIn exists:
    @Test
    @DisplayName("Should simulate finding resources by adGroups")
    void findByAdGroupsIn() {
        List<String> searchGroups = List.of("group-b");
        when(resourceRepository.findByAdGroupsIn(searchGroups)).thenReturn(Flux.just(resource1, resource2));

        Flux<Resource> foundResourcesFlux = resourceRepository.findByAdGroupsIn(searchGroups);

        StepVerifier.create(foundResourcesFlux)
                .expectNext(resource1)
                .expectNext(resource2)
                .verifyComplete();

        verify(resourceRepository).findByAdGroupsIn(searchGroups);
    }

    // Assuming findByAdGroupsInAndIsActive exists:
    @Test
    @DisplayName("Should simulate finding resources by adGroups and active status")
    void findByAdGroupsInAndIsActive() {
        List<String> searchGroups = List.of("group-b");
        when(resourceRepository.findByAdGroupsInAndIsActive(searchGroups, true)).thenReturn(Flux.just(resource1, resource2));

        Flux<Resource> foundResourcesFlux = resourceRepository.findByAdGroupsInAndIsActive(searchGroups, true);

        StepVerifier.create(foundResourcesFlux)
                .expectNext(resource1)
                .expectNext(resource2)
                .verifyComplete();

        verify(resourceRepository).findByAdGroupsInAndIsActive(searchGroups, true);
    }

    @Test
    @DisplayName("Should simulate finding resources by name containing ignore case and active status")
    void findByNameContainingIgnoreCaseAndIsActive() {
        when(resourceRepository.findByNameContainingIgnoreCaseAndIsActive("resource", true)).thenReturn(Flux.just(resource1, resource2));

        Flux<Resource> foundResourcesFlux = resourceRepository.findByNameContainingIgnoreCaseAndIsActive("resource", true);

        StepVerifier.create(foundResourcesFlux)
                .expectNext(resource1)
                .expectNext(resource2)
                .verifyComplete();

        verify(resourceRepository).findByNameContainingIgnoreCaseAndIsActive("resource", true);
    }

    @Test
    @DisplayName("Should simulate finding resources by name containing ignore case")
    void findByNameContainingIgnoreCase() {
        when(resourceRepository.findByNameContainingIgnoreCase("resource")).thenReturn(Flux.just(resource1, resource2, inactiveResource));

        Flux<Resource> foundResourcesFlux = resourceRepository.findByNameContainingIgnoreCase("resource");

        StepVerifier.create(foundResourcesFlux)
                .expectNext(resource1)
                .expectNext(resource2)
                .expectNext(inactiveResource)
                .verifyComplete();

        verify(resourceRepository).findByNameContainingIgnoreCase("resource");
    }

    @Test
    @DisplayName("Should simulate finding resources by type")
    void findByType() {
        when(resourceRepository.findByType("page")).thenReturn(Flux.just(resource1, inactiveResource));

        Flux<Resource> foundResourcesFlux = resourceRepository.findByType("page");

        StepVerifier.create(foundResourcesFlux)
                .expectNext(resource1)
                .expectNext(inactiveResource)
                .verifyComplete();

        verify(resourceRepository).findByType("page");
    }
}