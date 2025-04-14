package com.kaishui.entitlement.web;

import com.kaishui.entitlement.entity.dto.CreateGroupDefaultRoleDto;
import com.kaishui.entitlement.entity.dto.GroupDefaultRoleDto;
import com.kaishui.entitlement.entity.dto.UpdateGroupDefaultRoleDto;
import com.kaishui.entitlement.exception.ConflictException;
import com.kaishui.entitlement.exception.ResourceNotFoundException;
import com.kaishui.entitlement.service.GroupDefaultRoleService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupDefaultRoleControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private GroupDefaultRoleService groupDefaultRoleService;

    @InjectMocks
    private GroupDefaultRoleController groupDefaultRoleController;

    // Test Data
    private GroupDefaultRoleDto groupRoleDto1;
    private GroupDefaultRoleDto groupRoleDto2;
    private CreateGroupDefaultRoleDto createDto;
    private UpdateGroupDefaultRoleDto updateDto;
    private String groupRoleId1;
    private String groupName1;
    private String groupName2;

    @BeforeEach
    void setUp() {
        // Configure WebTestClient with a timeout (optional, but good practice)
        Duration timeout = Duration.ofSeconds(30);
        webTestClient = WebTestClient.bindToController(groupDefaultRoleController)
                .configureClient()
                .responseTimeout(timeout)
                .build();

        groupRoleId1 = new ObjectId().toHexString();
        groupName1 = "uk-admin";
        groupName2 = "sg-users";

        // Sample DTOs
        groupRoleDto1 = new GroupDefaultRoleDto();
        groupRoleDto1.setId(groupRoleId1);
        groupRoleDto1.setGroupName(groupName1);
        groupRoleDto1.setRoleIds(List.of("role-id-1", "role-id-2"));
        groupRoleDto1.setCreatedBy("creator1");
        groupRoleDto1.setCreatedDate(new Date());

        groupRoleDto2 = new GroupDefaultRoleDto();
        groupRoleDto2.setId(new ObjectId().toHexString());
        groupRoleDto2.setGroupName(groupName2);
        groupRoleDto2.setRoleIds(List.of("role-id-3"));
        groupRoleDto2.setCreatedBy("creator2");
        groupRoleDto2.setCreatedDate(new Date());

        // Sample Create/Update DTOs
        createDto = new CreateGroupDefaultRoleDto();
        createDto.setGroupName("us-devs");
        createDto.setRoleIds(List.of("role-id-new"));

        updateDto = new UpdateGroupDefaultRoleDto();
        updateDto.setRoleIds(List.of("role-id-1-updated", "role-id-new"));
    }

    @Test
    @DisplayName("POST /v1/api/group-default-roles - Should create mapping successfully and return 201")
    void createGroupDefaultRole_Success() {
        // Arrange
        when(groupDefaultRoleService.createGroupDefaultRole(any(Mono.class))).thenReturn(Mono.just(groupRoleDto1));

        // Act & Assert
        webTestClient.post().uri("/v1/api/group-default-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(createDto), CreateGroupDefaultRoleDto.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(GroupDefaultRoleDto.class).isEqualTo(groupRoleDto1);

        // Verify
        verify(groupDefaultRoleService).createGroupDefaultRole(any(Mono.class));
    }

    @Test
    @DisplayName("POST /v1/api/group-default-roles - Should return 409 on conflict")
    void createGroupDefaultRole_Conflict() {
        // Arrange
        when(groupDefaultRoleService.createGroupDefaultRole(any(Mono.class)))
                .thenReturn(Mono.error(new ConflictException("Mapping already exists")));

        // Act & Assert
        webTestClient.post().uri("/v1/api/group-default-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(createDto), CreateGroupDefaultRoleDto.class)
                .exchange()
                // Assuming GlobalExceptionHandler maps ConflictException to 409
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        // Verify
        verify(groupDefaultRoleService).createGroupDefaultRole(any(Mono.class));
    }


    @Test
    @DisplayName("GET /v1/api/group-default-roles - Should return all mappings when no filter")
    void getGroupDefaultRoles_All() {
        // Arrange
        when(groupDefaultRoleService.getAllGroupDefaultRoles()).thenReturn(Flux.just(groupRoleDto1, groupRoleDto2));

        // Act & Assert
        webTestClient.get().uri("/v1/api/group-default-roles")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(GroupDefaultRoleDto.class).hasSize(2).contains(groupRoleDto1, groupRoleDto2);

        // Verify
        verify(groupDefaultRoleService).getAllGroupDefaultRoles();
        verify(groupDefaultRoleService, never()).findByGroupNames(any());
    }

    @Test
    @DisplayName("GET /v1/api/group-default-roles - Should return filtered mappings")
    void getGroupDefaultRoles_Filtered() {
        // Arrange
        List<String> filterNames = List.of(groupName1);
        when(groupDefaultRoleService.findByGroupNames(filterNames)).thenReturn(Flux.just(groupRoleDto1));

        // Act & Assert
        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/v1/api/group-default-roles")
                        .queryParam("groupNames", groupName1) // Pass list elements
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(GroupDefaultRoleDto.class).hasSize(1).contains(groupRoleDto1);

        // Verify
        verify(groupDefaultRoleService).findByGroupNames(filterNames);
        verify(groupDefaultRoleService, never()).getAllGroupDefaultRoles();
    }

    @Test
    @DisplayName("GET /v1/api/group-default-roles/{id} - Should return mapping when found")
    void getGroupDefaultRoleById_Found() {
        // Arrange
        when(groupDefaultRoleService.getGroupDefaultRoleById(groupRoleId1)).thenReturn(Mono.just(groupRoleDto1));

        // Act & Assert
        webTestClient.get().uri("/v1/api/group-default-roles/{id}", groupRoleId1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(GroupDefaultRoleDto.class).isEqualTo(groupRoleDto1);

        // Verify
        verify(groupDefaultRoleService).getGroupDefaultRoleById(groupRoleId1);
    }

    @Test
    @DisplayName("GET /v1/api/group-default-roles/{id} - Should return 404 when not found")
    void getGroupDefaultRoleById_NotFound() {
        // Arrange
        String nonExistentId = "nonexistent-id";
        when(groupDefaultRoleService.getGroupDefaultRoleById(nonExistentId)).thenReturn(Mono.empty());

        // Act & Assert
        webTestClient.get().uri("/v1/api/group-default-roles/{id}", nonExistentId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        // Verify
        verify(groupDefaultRoleService).getGroupDefaultRoleById(nonExistentId);
    }

    @Test
    @DisplayName("GET /v1/api/group-default-roles/by-group/{groupName} - Should return mapping when found")
    void getGroupDefaultRoleByGroupName_Found() {
        // Arrange
        when(groupDefaultRoleService.getGroupDefaultRoleByGroupName(groupName1)).thenReturn(Mono.just(groupRoleDto1));

        // Act & Assert
        webTestClient.get().uri("/v1/api/group-default-roles/by-group/{groupName}", groupName1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(GroupDefaultRoleDto.class).isEqualTo(groupRoleDto1);

        // Verify
        verify(groupDefaultRoleService).getGroupDefaultRoleByGroupName(groupName1);
    }

    @Test
    @DisplayName("GET /v1/api/group-default-roles/by-group/{groupName} - Should return 404 when not found")
    void getGroupDefaultRoleByGroupName_NotFound() {
        // Arrange
        String nonExistentGroup = "nonexistent-group";
        when(groupDefaultRoleService.getGroupDefaultRoleByGroupName(nonExistentGroup)).thenReturn(Mono.empty());

        // Act & Assert
        webTestClient.get().uri("/v1/api/group-default-roles/by-group/{groupName}", nonExistentGroup)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        // Verify
        verify(groupDefaultRoleService).getGroupDefaultRoleByGroupName(nonExistentGroup);
    }

    @Test
    @DisplayName("PUT /v1/api/group-default-roles/{id} - Should update mapping successfully and return 200")
    void updateGroupDefaultRole_Success() {
        // Arrange
        when(groupDefaultRoleService.updateGroupDefaultRole(eq(groupRoleId1), any(Mono.class))).thenReturn(Mono.just(groupRoleDto1)); // Assume returns updated DTO

        // Act & Assert
        webTestClient.put().uri("/v1/api/group-default-roles/{id}", groupRoleId1)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateDto), UpdateGroupDefaultRoleDto.class)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(GroupDefaultRoleDto.class).isEqualTo(groupRoleDto1);

        // Verify
        verify(groupDefaultRoleService).updateGroupDefaultRole(eq(groupRoleId1), any(Mono.class));
    }

    @Test
    @DisplayName("PUT /v1/api/group-default-roles/{id} - Should return 404 when mapping not found")
    void updateGroupDefaultRole_NotFound() {
        // Arrange
        String nonExistentId = "nonexistent-id";
        // Service should ideally throw ResourceNotFoundException, which gets mapped to 404
        when(groupDefaultRoleService.updateGroupDefaultRole(eq(nonExistentId), any(Mono.class)))
                .thenReturn(Mono.error(new ResourceNotFoundException("Mapping not found")));

        // Act & Assert
        webTestClient.put().uri("/v1/api/group-default-roles/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateDto), UpdateGroupDefaultRoleDto.class)
                .exchange()
                // Assuming GlobalExceptionHandler maps ResourceNotFoundException to 404
                .expectStatus().isNotFound();

        // Verify
        verify(groupDefaultRoleService).updateGroupDefaultRole(eq(nonExistentId), any(Mono.class));
    }


    @Test
    @DisplayName("DELETE /v1/api/group-default-roles/{id} - Should delete mapping successfully and return 204")
    void deleteGroupDefaultRole_Success() {
        // Arrange
        when(groupDefaultRoleService.deleteGroupDefaultRole(groupRoleId1)).thenReturn(Mono.empty()); // Service returns Mono<Void>

        // Act & Assert
        webTestClient.delete().uri("/v1/api/group-default-roles/{id}", groupRoleId1)
                .exchange()
                .expectStatus().isNoContent();

        // Verify
        verify(groupDefaultRoleService).deleteGroupDefaultRole(groupRoleId1);
    }

    @Test
    @DisplayName("DELETE /v1/api/group-default-roles/{id} - Should return 404 when mapping not found")
    void deleteGroupDefaultRole_NotFound() {
        // Arrange
        String nonExistentId = "nonexistent-id";
        // Service should throw ResourceNotFoundException, mapped to 404
        when(groupDefaultRoleService.deleteGroupDefaultRole(nonExistentId))
                .thenReturn(Mono.error(new ResourceNotFoundException("Mapping not found")));

        // Act & Assert
        webTestClient.delete().uri("/v1/api/group-default-roles/{id}", nonExistentId)
                .exchange()
                // Assuming GlobalExceptionHandler maps ResourceNotFoundException to 404
                .expectStatus().isNotFound();

        // Verify
        verify(groupDefaultRoleService).deleteGroupDefaultRole(nonExistentId);
    }
}