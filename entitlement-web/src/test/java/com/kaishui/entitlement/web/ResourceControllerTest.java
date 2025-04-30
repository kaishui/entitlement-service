package com.kaishui.entitlement.web;

import com.kaishui.entitlement.entity.dto.CreateResourceDto;
import com.kaishui.entitlement.entity.dto.ResourceDto;
import com.kaishui.entitlement.entity.dto.UpdateResourceDto;
import com.kaishui.entitlement.exception.ResourceNotFoundException; // Assuming this exception exists
import com.kaishui.entitlement.service.ResourceService;
import org.bson.Document;
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

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceControllerTest {

    private WebTestClient webTestClient; // Client to make requests

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private ResourceController resourceController;

    // Test Data
    private ResourceDto resourceDto1;
    private ResourceDto resourceDto2;
    private CreateResourceDto createDto;
    private UpdateResourceDto updateDto;
    private String resourceId1;

    @BeforeEach
    void setUp() {
        Duration timeout = Duration.ofSeconds(30);
        webTestClient = WebTestClient.bindToController(resourceController)
                .configureClient()
                .responseTimeout(timeout) // Set timeout here
                .build();

        resourceId1 = new ObjectId().toHexString();

        // Sample DTOs
        resourceDto1 = new ResourceDto();
        resourceDto1.setId(resourceId1);
        resourceDto1.setName("Resource One");
        resourceDto1.setType("API");
        resourceDto1.setDescription("First API resource");
        resourceDto1.setPermission(new Document("method", "GET"));
        resourceDto1.setAdGroups(List.of("group-a", "group-b"));
        resourceDto1.setIsActive(true);
        resourceDto1.setCreatedBy("creator");
        resourceDto1.setCreatedDate(new Date());

        resourceDto2 = new ResourceDto();
        resourceDto2.setId(new ObjectId().toHexString());
        resourceDto2.setName("Resource Two UI");
        resourceDto2.setType("PAGE");
        resourceDto2.setDescription("Second UI resource");
        resourceDto2.setPermission(new Document("component", "button"));
        resourceDto2.setAdGroups(List.of("group-b", "group-c"));
        resourceDto2.setIsActive(true);
        resourceDto2.setCreatedBy("creator");
        resourceDto2.setCreatedDate(new Date());

        // Sample Create/Update DTOs
        createDto = new CreateResourceDto();
        createDto.setName("New Resource");
        createDto.setType("API");
        createDto.setDescription("A brand new resource");
        createDto.setPermission(new Document("method", "POST"));
        createDto.setAdGroups(List.of("group-new"));
        createDto.setIsActive(true);

        updateDto = new UpdateResourceDto();
        updateDto.setName("Resource One Updated");
        updateDto.setDescription("Updated description");
        updateDto.setAdGroups(List.of("group-a", "group-updated"));
    }

    @Test
    @DisplayName("POST /v1/api/resources - Should create resource successfully and return 201")
    void createResource_Success() {
        // Arrange
        when(resourceService.createResource(any(Mono.class))).thenReturn(Mono.just(resourceDto1));

        // Act & Assert
        webTestClient.post().uri("/v1/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(createDto), CreateResourceDto.class) // Pass the Mono directly
                .exchange()
                .expectStatus().isCreated() // Expect 201 Created due to @ResponseStatus
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(ResourceDto.class).isEqualTo(resourceDto1);

        // Verify
        verify(resourceService).createResource(any(Mono.class));
    }


    @Test
    @DisplayName("POST /v1/api/resources - Should return 5xx if service fails")
    void createResource_ServiceError() {
        // Arrange
        when(resourceService.createResource(any(Mono.class))).thenReturn(Mono.error(new RuntimeException("Service failure")));

        // Act & Assert
        webTestClient.post().uri("/v1/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(createDto), CreateResourceDto.class)
                .exchange()
                .expectStatus().is5xxServerError();

        // Verify
        verify(resourceService).createResource(any(Mono.class));
    }

    @Test
    @DisplayName("GET /v1/api/resources - Should return resources based on filters")
    void findResources_Success() {
        // Arrange: Mock service behavior for findResources
        String nameFilter = "Resource";
        String typeFilter = "API";
        List<String> adGroupFilter = List.of("group-a");
        Boolean activeFilter = true;
        when(resourceService.findResources(nameFilter, typeFilter, adGroupFilter, activeFilter))
                .thenReturn(Flux.just(resourceDto1)); // Assume only resourceDto1 matches these filters

        // Act & Assert
        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/v1/api/resources")
                        .queryParam("name", nameFilter)
                        .queryParam("type", typeFilter)
                        .queryParam("adGroups", "group-a") // Pass list elements as separate params or comma-separated
                        .queryParam("isActive", activeFilter)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(ResourceDto.class).hasSize(1).contains(resourceDto1);

        // Verify service interaction
        verify(resourceService).findResources(nameFilter, typeFilter, adGroupFilter, activeFilter);
    }

    @Test
    @DisplayName("GET /v1/api/resources - Should return empty list when no resources match")
    void findResources_NoMatch() {
        // Arrange
        when(resourceService.findResources(any(), any(), any(), any())).thenReturn(Flux.empty());

        // Act & Assert
        webTestClient.get().uri("/v1/api/resources?name=nonexistent")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(ResourceDto.class).hasSize(0);

        // Verify
        verify(resourceService).findResources(eq("nonexistent"), any(), any(), any());
    }


    @Test
    @DisplayName("GET /v1/api/resources/{id} - Should return resource when found")
    void getResourceById_Found() {
        // Arrange
        when(resourceService.getResourceById(resourceId1)).thenReturn(Mono.just(resourceDto1));

        // Act & Assert
        webTestClient.get().uri("/v1/api/resources/{id}", resourceId1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(ResourceDto.class).isEqualTo(resourceDto1);

        // Verify
        verify(resourceService).getResourceById(resourceId1);
    }

    @Test
    @DisplayName("GET /v1/api/resources/{id} - Should return 404 when not found (service returns empty)")
    void getResourceById_NotFound_ServiceEmpty() {
        // Arrange
        String nonExistentId = "nonexistent";
        when(resourceService.getResourceById(nonExistentId)).thenReturn(Mono.empty()); // Service returns empty

        // Act & Assert
        webTestClient.get().uri("/v1/api/resources/{id}", nonExistentId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound() // Controller maps empty Mono to 404
                .expectBody().isEmpty();

        // Verify
        verify(resourceService).getResourceById(nonExistentId);
    }

    @Test
    @DisplayName("PUT /v1/api/resources/{id} - Should update resource successfully and return 200")
    void updateResource_Success() {
        // Arrange
        when(resourceService.updateResource(eq(resourceId1), any(Mono.class))).thenReturn(Mono.just(resourceDto1)); // Assume update returns the updated DTO

        // Act & Assert
        webTestClient.put().uri("/v1/api/resources/{id}", resourceId1)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateDto), UpdateResourceDto.class)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(ResourceDto.class).isEqualTo(resourceDto1);

        // Verify
        verify(resourceService).updateResource(eq(resourceId1), any(Mono.class));
    }

    @Test
    @DisplayName("PUT /v1/api/resources/{id} - Should return 404 when resource to update not found (service returns empty)")
    void updateResource_NotFound_ServiceEmpty() {
        // Arrange
        String nonExistentId = "nonexistent";
        when(resourceService.updateResource(eq(nonExistentId), any(Mono.class))).thenReturn(Mono.empty());

        // Act & Assert
        webTestClient.put().uri("/v1/api/resources/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateDto), UpdateResourceDto.class)
                .exchange()
                .expectStatus().isNotFound() // Controller maps empty Mono to 404
                .expectBody().isEmpty();

        // Verify
        verify(resourceService).updateResource(eq(nonExistentId), any(Mono.class));
    }


    @Test
    @DisplayName("PUT /v1/api/resources/{id} - Should return 5xx if service fails during update logic")
    void updateResource_ServiceError() {
        // Arrange
        when(resourceService.updateResource(eq(resourceId1), any(Mono.class)))
                .thenReturn(Mono.error(new RuntimeException("Update failed")));

        // Act & Assert
        webTestClient.put().uri("/v1/api/resources/{id}", resourceId1)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateDto), UpdateResourceDto.class)
                .exchange()
                .expectStatus().is5xxServerError();

        // Verify
        verify(resourceService).updateResource(eq(resourceId1), any(Mono.class));
    }


    @Test
    @DisplayName("DELETE /v1/api/resources/{id} - Should delete resource successfully and return 204")
    void deleteResource_Success() {
        // Arrange
        when(resourceService.deleteResource(resourceId1)).thenReturn(Mono.empty()); // Service returns Mono<Void>

        // Act & Assert
        webTestClient.delete().uri("/v1/api/resources/{id}", resourceId1)
                .exchange()
                .expectStatus().isNoContent() // Expect 204 No Content due to @ResponseStatus
                .expectBody().isEmpty();

        // Verify
        verify(resourceService).deleteResource(resourceId1);
    }

    @Test
    @DisplayName("DELETE /v1/api/resources/{id} - Should return 5xx if service fails (e.g., not found exception mapped to 5xx)")
    void deleteResource_ServiceError_NotFound() {
        // Arrange
        String idToDelete = "faildelete";
        // Simulate service throwing ResourceNotFoundException which might get mapped to 5xx
        // depending on exception handling setup (or test for 404 if that's the expected mapping)
        when(resourceService.deleteResource(idToDelete))
                .thenReturn(Mono.error(new ResourceNotFoundException("Not found")));

        // Act & Assert
        webTestClient.delete().uri("/v1/api/resources/{id}", idToDelete)
                .exchange()
                // If ResourceNotFoundException is handled globally to return 404, expect .isNotFound()
                // If not handled specifically, it might bubble up as 500
                .expectStatus().isNotFound(); // Adjust based on your global exception handling

        // Verify
        verify(resourceService).deleteResource(idToDelete);
    }

    @Test
    @DisplayName("DELETE /v1/api/resources/{id} - Should return 5xx if service fails with other error")
    void deleteResource_ServiceError_Other() {
        // Arrange
        String idToDelete = "faildelete";
        when(resourceService.deleteResource(idToDelete))
                .thenReturn(Mono.error(new RuntimeException("DB connection error")));

        // Act & Assert
        webTestClient.delete().uri("/v1/api/resources/{id}", idToDelete)
                .exchange()
                .expectStatus().is5xxServerError();

        // Verify
        verify(resourceService).deleteResource(idToDelete);
    }
}