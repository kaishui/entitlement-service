package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.Resource;
import com.kaishui.entitlement.entity.dto.CreateResourceDto;
import com.kaishui.entitlement.entity.dto.ResourceDto;
import com.kaishui.entitlement.entity.dto.UpdateResourceDto;
import com.kaishui.entitlement.exception.ResourceNotFoundException;
import com.kaishui.entitlement.repository.ResourceRepository;
import com.kaishui.entitlement.util.AuthorizationUtil;
import com.kaishui.entitlement.util.ResourceMapper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ResourceMapper resourceMapper;

    // Mock AuthorizationUtil if needed for context propagation tests, though auditing might handle it
    @Mock
    private AuthorizationUtil authorizationUtil;

    @InjectMocks
    private ResourceService resourceService;

    private MockedStatic<AuthorizationUtil> mockedAuthorizationUtil; // If mocking static methods

    // Test Data
    private Resource resource1;
    private Resource resource2;
    private ResourceDto resourceDto1;
    private ResourceDto resourceDto2;
    private CreateResourceDto createDto;
    private UpdateResourceDto updateDto;

    private final String resourceId1 = new ObjectId().toHexString();
    private final String resourceId2 = new ObjectId().toHexString();
    private final String testUsername = "test-user"; // Example username

    @BeforeEach
    void setUp() {
        // Sample Entities
        resource1 = Resource.builder()
                .id(resourceId1)
                .name("Resource One")
                .type("API")
                .description("First API resource")
                .permission(List.of(new Document("method", "GET")))
                .adGroups(List.of("group-a", "group-b"))
                .isActive(true)
                .createdBy("creator")
                .createdDate(new Date())
                .build();

        resource2 = Resource.builder()
                .id(resourceId2)
                .name("Resource Two UI")
                .type("PAGE")
                .description("Second UI resource")
                .permission(List.of(new Document("component", "button")))
                .adGroups(List.of("group-b", "group-c"))
                .isActive(true)
                .createdBy("creator")
                .createdDate(new Date())
                .build();

        // Sample DTOs (mirroring entities for simplicity in tests)
        resourceDto1 = new ResourceDto();
        resourceDto1.setId(resourceId1);
        resourceDto1.setName("Resource One");
        resourceDto1.setType("API");
        resourceDto1.setDescription("First API resource");
        resourceDto1.setPermission(List.of(new Document("method", "GET")));
        resourceDto1.setAdGroups(List.of("group-a", "group-b"));
        resourceDto1.setIsActive(true);
        resourceDto1.setCreatedBy("creator");
        resourceDto1.setCreatedDate(resource1.getCreatedDate());

        resourceDto2 = new ResourceDto();
        resourceDto2.setId(resourceId2);
        resourceDto2.setName("Resource Two UI");
        resourceDto2.setType("PAGE");
        resourceDto2.setDescription("Second UI resource");
        resourceDto2.setPermission(List.of(new Document("component", "button")));
        resourceDto2.setAdGroups(List.of("group-b", "group-c"));
        resourceDto2.setIsActive(true);
        resourceDto2.setCreatedBy("creator");
        resourceDto2.setCreatedDate(resource2.getCreatedDate());


        // Sample Create/Update DTOs
        createDto = new CreateResourceDto();
        createDto.setName("New Resource");
        createDto.setType("API");
        createDto.setDescription("A brand new resource");
        createDto.setPermission(List.of(new Document("method", "POST")));
        createDto.setAdGroups(List.of("group-new"));
        createDto.setIsActive(true);

        updateDto = new UpdateResourceDto();
        updateDto.setName("Resource One Updated");
        updateDto.setDescription("Updated description");
        updateDto.setAdGroups(List.of("group-a", "group-updated"));

        // --- Mock static AuthorizationUtil if needed ---
        // Uncomment if your service or aspects rely on this being mocked for tests
        // mockedAuthorizationUtil = Mockito.mockStatic(AuthorizationUtil.class);
        // mockedAuthorizationUtil.when(() -> authorizationUtil.extractUsernameFromContext(any(Context.class)))
        //        .thenReturn(testUsername);
        // mockedAuthorizationUtil.when(() -> authorizationUtil.extractUsernameFromContext(any(reactor.util.context.ContextView.class)))
        //        .thenReturn(testUsername);
        // --- End Mock static ---
    }

    @AfterEach
    void tearDown() {
        // Close static mock if it was created
        // if (mockedAuthorizationUtil != null) {
        //     mockedAuthorizationUtil.close();
        // }
    }

    @Test
    @DisplayName("createResource should map DTO, save, and return mapped DTO")
    void createResource_Success() {
        Resource newResourceEntity = Resource.builder() // Entity mapped from createDto
                .name(createDto.getName())
                .type(createDto.getType())
                .description(createDto.getDescription())
                .permission(createDto.getPermission())
                .adGroups(createDto.getAdGroups())
                .isActive(createDto.getIsActive())
                .build();

        Resource savedResourceEntity = Resource.builder() // Entity returned after save
                .id(new ObjectId().toHexString()) // Simulate ID generation
                .name(createDto.getName())
                .type(createDto.getType())
                .description(createDto.getDescription())
                .permission(createDto.getPermission())
                .adGroups(createDto.getAdGroups())
                .isActive(createDto.getIsActive())
                .createdBy(testUsername) // Assume auditing sets this
                .createdDate(new Date())
                .build();

        ResourceDto finalDto = new ResourceDto(); // DTO mapped from savedEntity
        finalDto.setId(savedResourceEntity.getId());
        finalDto.setName(savedResourceEntity.getName());
        finalDto.setType(savedResourceEntity.getType());
        finalDto.setDescription(savedResourceEntity.getDescription());
        finalDto.setPermission(savedResourceEntity.getPermission());
        finalDto.setAdGroups(savedResourceEntity.getAdGroups());
        finalDto.setIsActive(savedResourceEntity.getIsActive());
        finalDto.setCreatedBy(savedResourceEntity.getCreatedBy());
        finalDto.setCreatedDate(savedResourceEntity.getCreatedDate());


        // Mocking behavior
        when(resourceMapper.toEntity(any(CreateResourceDto.class))).thenReturn(newResourceEntity);
        when(resourceRepository.save(any(Resource.class))).thenReturn(Mono.just(savedResourceEntity));
        when(resourceMapper.toDto(any(Resource.class))).thenReturn(finalDto);

        // Execution
        Mono<ResourceDto> result = resourceService.createResource(Mono.just(createDto));

        // Verification
        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.getId().equals(finalDto.getId()) &&
                        dto.getName().equals(createDto.getName()))
                .verifyComplete();

        verify(resourceMapper).toEntity(createDto);
        verify(resourceRepository).save(newResourceEntity);
        verify(resourceMapper).toDto(savedResourceEntity);
    }

    @Test
    @DisplayName("getResourceById should return DTO when found")
    void getResourceById_Found() {
        when(resourceRepository.findById(resourceId1)).thenReturn(Mono.just(resource1));
        when(resourceMapper.toDto(resource1)).thenReturn(resourceDto1);

        Mono<ResourceDto> result = resourceService.getResourceById(resourceId1);

        StepVerifier.create(result)
                .expectNext(resourceDto1)
                .verifyComplete();

        verify(resourceRepository).findById(resourceId1);
        verify(resourceMapper).toDto(resource1);
    }

    @Test
    @DisplayName("getResourceById should return ResourceNotFoundException when not found")
    void getResourceById_NotFound() {
        when(resourceRepository.findById("nonexistent")).thenReturn(Mono.empty());

        Mono<ResourceDto> result = resourceService.getResourceById("nonexistent");

        StepVerifier.create(result)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(resourceRepository).findById("nonexistent");
        verify(resourceMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("updateResource should update and return DTO when found")
    void updateResource_Success() {
        // Simulate the state after mapper updates the entity
        Resource updatedEntityState = Resource.builder()
                .id(resource1.getId())
                .name(updateDto.getName()) // Updated
                .type(resource1.getType()) // Not in update DTO
                .description(updateDto.getDescription()) // Updated
                .permission(resource1.getPermission()) // Not in update DTO
                .adGroups(updateDto.getAdGroups()) // Updated
                .isActive(resource1.getIsActive()) // Not in update DTO
                .createdBy(resource1.getCreatedBy())
                .createdDate(resource1.getCreatedDate())
                // Assume auditing would set these on save
                .updatedBy(testUsername)
                .lastModifiedDate(new Date())
                .build();

        ResourceDto finalDto = new ResourceDto(); // DTO mapped from saved updated entity
        finalDto.setId(updatedEntityState.getId());
        finalDto.setName(updatedEntityState.getName());
        finalDto.setType(updatedEntityState.getType());
        finalDto.setDescription(updatedEntityState.getDescription());
        finalDto.setPermission(updatedEntityState.getPermission());
        finalDto.setAdGroups(updatedEntityState.getAdGroups());
        finalDto.setIsActive(updatedEntityState.getIsActive());
        finalDto.setCreatedBy(updatedEntityState.getCreatedBy());
        finalDto.setCreatedDate(updatedEntityState.getCreatedDate());
        finalDto.setUpdatedBy(updatedEntityState.getUpdatedBy());
        finalDto.setLastModifiedDate(updatedEntityState.getLastModifiedDate());


        when(resourceRepository.findById(resourceId1)).thenReturn(Mono.just(resource1));
        // Mock the void mapper method - it doesn't return anything, just modifies the target
        doNothing().when(resourceMapper).updateEntityFromDto(any(UpdateResourceDto.class), any(Resource.class));
        when(resourceRepository.save(any(Resource.class))).thenReturn(Mono.just(updatedEntityState)); // Return the state *after* save
        when(resourceMapper.toDto(any(Resource.class))).thenReturn(finalDto);

        Mono<ResourceDto> result = resourceService.updateResource(resourceId1, Mono.just(updateDto));

        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.getId().equals(resourceId1) &&
                        dto.getName().equals(updateDto.getName()) &&
                        dto.getDescription().equals(updateDto.getDescription()) &&
                        dto.getAdGroups().equals(updateDto.getAdGroups()))
                .verifyComplete();

        verify(resourceRepository).findById(resourceId1);
        // Verify mapper was called with the correct objects
        verify(resourceMapper).updateEntityFromDto(updateDto, resource1);
        // Verify save was called with the (mocked) updated entity state
        verify(resourceRepository).save(resource1); // The object passed to save is the one modified by the mapper
        verify(resourceMapper).toDto(updatedEntityState);
    }


    @Test
    @DisplayName("updateResource should return ResourceNotFoundException when not found")
    void updateResource_NotFound() {
        when(resourceRepository.findById("nonexistent")).thenReturn(Mono.empty());

        Mono<ResourceDto> result = resourceService.updateResource("nonexistent", Mono.just(updateDto));

        StepVerifier.create(result)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(resourceRepository).findById("nonexistent");
        verify(resourceMapper, never()).updateEntityFromDto(any(), any());
        verify(resourceRepository, never()).save(any());
        verify(resourceMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("deleteResource should complete when found")
    void deleteResource_Success() {
        when(resourceRepository.findById(resourceId1)).thenReturn(Mono.just(resource1));
        when(resourceRepository.delete(resource1)).thenReturn(Mono.empty()); // delete returns Mono<Void>

        Mono<Void> result = resourceService.deleteResource(resourceId1);

        StepVerifier.create(result)
                .verifyComplete();

        verify(resourceRepository).findById(resourceId1);
        verify(resourceRepository).delete(resource1);
    }

    @Test
    @DisplayName("deleteResource should return ResourceNotFoundException when not found")
    void deleteResource_NotFound() {
        when(resourceRepository.findById("nonexistent")).thenReturn(Mono.empty());

        Mono<Void> result = resourceService.deleteResource("nonexistent");

        StepVerifier.create(result)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(resourceRepository).findById("nonexistent");
        verify(resourceRepository, never()).delete(any());
    }

    // --- Tests for findResources ---

    @Test
    @DisplayName("findResources should return all active when no filters")
    void findResources_NoFilters_ActiveOnly() {
        Resource inactive = Resource.builder().id("res3").name("Inactive").isActive(false).build();
        ResourceDto inactiveDto = new ResourceDto(); // Assume mapper maps this
        inactiveDto.setId("res3");
        inactiveDto.setName("Inactive");
        inactiveDto.setIsActive(false);

        when(resourceRepository.findByIsActive(true)).thenReturn(Flux.just(resource1, resource2));
        when(resourceMapper.toDto(resource1)).thenReturn(resourceDto1);
        when(resourceMapper.toDto(resource2)).thenReturn(resourceDto2);

        Flux<ResourceDto> result = resourceService.findResources(null, null, null, true);

        StepVerifier.create(result)
                .expectNext(resourceDto1)
                .expectNext(resourceDto2)
                .verifyComplete();

        verify(resourceRepository).findByIsActive(true);
        verify(resourceRepository, never()).findAll();
        verify(resourceMapper, times(2)).toDto(any(Resource.class));
    }

    @Test
    @DisplayName("findResources should return all when isActive is null")
    void findResources_NoFilters_All() {
        Resource inactive = Resource.builder().id("res3").name("Inactive").isActive(false).build();
        ResourceDto inactiveDto = new ResourceDto();
        inactiveDto.setId("res3");
        inactiveDto.setName("Inactive");
        inactiveDto.setIsActive(false);

        when(resourceRepository.findAll()).thenReturn(Flux.just(resource1, resource2, inactive));
        when(resourceMapper.toDto(resource1)).thenReturn(resourceDto1);
        when(resourceMapper.toDto(resource2)).thenReturn(resourceDto2);
        when(resourceMapper.toDto(inactive)).thenReturn(inactiveDto);


        Flux<ResourceDto> result = resourceService.findResources(null, null, null, null);

        StepVerifier.create(result)
                .expectNext(resourceDto1)
                .expectNext(resourceDto2)
                .expectNext(inactiveDto)
                .verifyComplete();

        verify(resourceRepository, never()).findByIsActive(anyBoolean());
        verify(resourceRepository).findAll();
        verify(resourceMapper, times(3)).toDto(any(Resource.class));
    }

    @Test
    @DisplayName("findResources should filter by name (case-insensitive)")
    void findResources_FilterByName() {
        when(resourceRepository.findAll()).thenReturn(Flux.just(resource1, resource2)); // Assume isActive=null
        when(resourceMapper.toDto(resource1)).thenReturn(resourceDto1);
        // resourceDto2 is NOT expected

        Flux<ResourceDto> result = resourceService.findResources("one", null, null, null); // Filter for "one"

        StepVerifier.create(result)
                .expectNext(resourceDto1) // Only resource1 matches "one"
                .verifyComplete();

        verify(resourceRepository).findAll();
        verify(resourceMapper).toDto(resource1);
        verify(resourceMapper, never()).toDto(resource2); // Verify resource2 wasn't mapped
    }

    @Test
    @DisplayName("findResources should filter by type (case-insensitive)")
    void findResources_FilterByType() {
        when(resourceRepository.findAll()).thenReturn(Flux.just(resource1, resource2)); // Assume isActive=null
        when(resourceMapper.toDto(resource1)).thenReturn(resourceDto1);
        // resourceDto2 is NOT expected

        Flux<ResourceDto> result = resourceService.findResources(null, "api", null, null); // Filter for "api" (case-insensitive)

        StepVerifier.create(result)
                .expectNext(resourceDto1) // Only resource1 has type "API"
                .verifyComplete();

        verify(resourceRepository).findAll();
        verify(resourceMapper).toDto(resource1);
        verify(resourceMapper, never()).toDto(resource2);
    }

    @Test
    @DisplayName("findResources should filter by adGroups")
    void findResources_FilterByAdGroups() {
        when(resourceRepository.findAll()).thenReturn(Flux.just(resource1, resource2)); // Assume isActive=null
        when(resourceMapper.toDto(resource2)).thenReturn(resourceDto2);

        List<String> searchGroups = List.of("group-c"); // Search for group-c

        Flux<ResourceDto> result = resourceService.findResources(null, null, searchGroups, null);

        StepVerifier.create(result)
                .expectNext(resourceDto2) // Only resource2 has "group-c"
                .verifyComplete();

        verify(resourceRepository).findAll();
        verify(resourceMapper, never()).toDto(resource1);
        verify(resourceMapper).toDto(resource2);
    }

    @Test
    @DisplayName("findResources should combine filters (e.g., isActive and type)")
    void findResources_CombineFilters() {
        // resource1: type=API, active=true
        // resource2: type=PAGE, active=true
        Resource apiInactive = Resource.builder().id("res4").name("API Inactive").type("API").isActive(false).build();
        ResourceDto apiInactiveDto = new ResourceDto();
        apiInactiveDto.setId("res4");
        apiInactiveDto.setName("API Inactive");
        apiInactiveDto.setType("API");
        apiInactiveDto.setIsActive(false);


        when(resourceRepository.findByIsActive(true)).thenReturn(Flux.just(resource1, resource2));
        when(resourceMapper.toDto(resource1)).thenReturn(resourceDto1);
        // resourceDto2 is filtered out by type

        Flux<ResourceDto> result = resourceService.findResources(null, "API", null, true); // Active=true, Type=API

        StepVerifier.create(result)
                .expectNext(resourceDto1) // Only resource1 matches both
                .verifyComplete();

        verify(resourceRepository).findByIsActive(true);
        verify(resourceMapper).toDto(resource1);
        verify(resourceMapper, never()).toDto(resource2);
    }
}