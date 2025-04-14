package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.GroupDefaultRole;
import com.kaishui.entitlement.entity.dto.CreateGroupDefaultRoleDto;
import com.kaishui.entitlement.entity.dto.GroupDefaultRoleDto;
import com.kaishui.entitlement.entity.dto.UpdateGroupDefaultRoleDto;
import com.kaishui.entitlement.exception.ConflictException;
import com.kaishui.entitlement.exception.ResourceNotFoundException;
import com.kaishui.entitlement.repository.GroupDefaultRoleRepository;
import com.kaishui.entitlement.util.GroupDefaultRoleMapper;
// No need for AuthorizationUtil mock here unless explicitly used by aspects being tested separately
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupDefaultRoleServiceTest {

    @Mock
    private GroupDefaultRoleRepository groupDefaultRoleRepository;

    @Mock
    private GroupDefaultRoleMapper groupDefaultRoleMapper;

    @InjectMocks
    private GroupDefaultRoleService groupDefaultRoleService;

    // Test Data
    private GroupDefaultRole groupRole1;
    private GroupDefaultRole groupRole2;
    private GroupDefaultRoleDto groupRoleDto1;
    private GroupDefaultRoleDto groupRoleDto2;
    private CreateGroupDefaultRoleDto createDto;
    private UpdateGroupDefaultRoleDto updateDto;

    private final String groupRoleId1 = new ObjectId().toHexString();
    private final String groupRoleId2 = new ObjectId().toHexString();
    private final String groupName1 = "uk-admin";
    private final String groupName2 = "sg-users";
    private final String newGroupName = "us-devs";
    private final String testUsername = "test-auditor"; // Example username for auditing fields

    @BeforeEach
    void setUp() {
        // Sample Entities
        groupRole1 = GroupDefaultRole.builder()
                .id(groupRoleId1)
                .groupName(groupName1)
                .roleIds(List.of("role-id-1", "role-id-2"))
                .createdBy("creator1")
                .createdDate(new Date())
                .lastModifiedBy("modifier1")
                .lastModifiedDate(new Date())
                .build();

        groupRole2 = GroupDefaultRole.builder()
                .id(groupRoleId2)
                .groupName(groupName2)
                .roleIds(List.of("role-id-3"))
                .createdBy("creator2")
                .createdDate(new Date())
                .lastModifiedBy("modifier2")
                .lastModifiedDate(new Date())
                .build();

        // Sample DTOs
        groupRoleDto1 = new GroupDefaultRoleDto();
        groupRoleDto1.setId(groupRoleId1);
        groupRoleDto1.setGroupName(groupName1);
        groupRoleDto1.setRoleIds(List.of("role-id-1", "role-id-2"));
        groupRoleDto1.setCreatedBy("creator1");
        groupRoleDto1.setCreatedDate(groupRole1.getCreatedDate());
        groupRoleDto1.setLastModifiedBy("modifier1");
        groupRoleDto1.setLastModifiedDate(groupRole1.getLastModifiedDate());

        groupRoleDto2 = new GroupDefaultRoleDto();
        groupRoleDto2.setId(groupRoleId2);
        groupRoleDto2.setGroupName(groupName2);
        groupRoleDto2.setRoleIds(List.of("role-id-3"));
        groupRoleDto2.setCreatedBy("creator2");
        groupRoleDto2.setCreatedDate(groupRole2.getCreatedDate());
        groupRoleDto2.setLastModifiedBy("modifier2");
        groupRoleDto2.setLastModifiedDate(groupRole2.getLastModifiedDate());

        // Sample Create/Update DTOs
        createDto = new CreateGroupDefaultRoleDto();
        createDto.setGroupName(newGroupName);
        createDto.setRoleIds(List.of("role-id-new"));

        updateDto = new UpdateGroupDefaultRoleDto();
        updateDto.setRoleIds(List.of("role-id-1-updated", "role-id-new"));
    }

    @Test
    @DisplayName("createGroupDefaultRole should succeed when group name does not exist")
    void createGroupDefaultRole_Success() {
        GroupDefaultRole newEntity = GroupDefaultRole.builder()
                .groupName(createDto.getGroupName())
                .roleIds(createDto.getRoleIds())
                .build();

        GroupDefaultRole savedEntity = GroupDefaultRole.builder()
                .id(new ObjectId().toHexString())
                .groupName(createDto.getGroupName())
                .roleIds(createDto.getRoleIds())
                .createdBy(testUsername) // Assume auditing sets this
                .createdDate(new Date())
                .lastModifiedBy(testUsername)
                .lastModifiedDate(new Date())
                .build();

        GroupDefaultRoleDto finalDto = new GroupDefaultRoleDto();
        finalDto.setId(savedEntity.getId());
        finalDto.setGroupName(savedEntity.getGroupName());
        finalDto.setRoleIds(savedEntity.getRoleIds());
        finalDto.setCreatedBy(savedEntity.getCreatedBy());
        finalDto.setCreatedDate(savedEntity.getCreatedDate());
        finalDto.setLastModifiedBy(savedEntity.getLastModifiedBy());
        finalDto.setLastModifiedDate(savedEntity.getLastModifiedDate());

        // Mocking
        when(groupDefaultRoleRepository.existsByGroupName(newGroupName)).thenReturn(Mono.just(false));
        when(groupDefaultRoleMapper.toEntity(any(CreateGroupDefaultRoleDto.class))).thenReturn(newEntity);
        when(groupDefaultRoleRepository.save(any(GroupDefaultRole.class))).thenReturn(Mono.just(savedEntity));
        when(groupDefaultRoleMapper.toDto(any(GroupDefaultRole.class))).thenReturn(finalDto);

        // Execution
        Mono<GroupDefaultRoleDto> result = groupDefaultRoleService.createGroupDefaultRole(Mono.just(createDto));

        // Verification
        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.getId().equals(finalDto.getId()) &&
                        dto.getGroupName().equals(newGroupName))
                .verifyComplete();

        verify(groupDefaultRoleRepository).existsByGroupName(newGroupName);
        verify(groupDefaultRoleMapper).toEntity(createDto);
        verify(groupDefaultRoleRepository).save(newEntity);
        verify(groupDefaultRoleMapper).toDto(savedEntity);
    }

    @Test
    @DisplayName("createGroupDefaultRole should fail with ConflictException when group name exists")
    void createGroupDefaultRole_Conflict() {
        // Mocking
        when(groupDefaultRoleRepository.existsByGroupName(createDto.getGroupName())).thenReturn(Mono.just(true));

        // Execution
        Mono<GroupDefaultRoleDto> result = groupDefaultRoleService.createGroupDefaultRole(Mono.just(createDto));

        // Verification
        StepVerifier.create(result)
                .expectError(ConflictException.class)
                .verify();

        verify(groupDefaultRoleRepository).existsByGroupName(createDto.getGroupName());
        verify(groupDefaultRoleMapper, never()).toEntity(any());
        verify(groupDefaultRoleRepository, never()).save(any());
        verify(groupDefaultRoleMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("getAllGroupDefaultRoles should return all mapped DTOs")
    void getAllGroupDefaultRoles_Success() {
        when(groupDefaultRoleRepository.findAll()).thenReturn(Flux.just(groupRole1, groupRole2));
        when(groupDefaultRoleMapper.toDto(groupRole1)).thenReturn(groupRoleDto1);
        when(groupDefaultRoleMapper.toDto(groupRole2)).thenReturn(groupRoleDto2);

        Flux<GroupDefaultRoleDto> result = groupDefaultRoleService.getAllGroupDefaultRoles();

        StepVerifier.create(result)
                .expectNext(groupRoleDto1)
                .expectNext(groupRoleDto2)
                .verifyComplete();

        verify(groupDefaultRoleRepository).findAll();
        verify(groupDefaultRoleMapper, times(2)).toDto(any(GroupDefaultRole.class));
    }

    @Test
    @DisplayName("getGroupDefaultRoleById should return DTO when found")
    void getGroupDefaultRoleById_Found() {
        when(groupDefaultRoleRepository.findById(groupRoleId1)).thenReturn(Mono.just(groupRole1));
        when(groupDefaultRoleMapper.toDto(groupRole1)).thenReturn(groupRoleDto1);

        Mono<GroupDefaultRoleDto> result = groupDefaultRoleService.getGroupDefaultRoleById(groupRoleId1);

        StepVerifier.create(result)
                .expectNext(groupRoleDto1)
                .verifyComplete();

        verify(groupDefaultRoleRepository).findById(groupRoleId1);
        verify(groupDefaultRoleMapper).toDto(groupRole1);
    }

    @Test
    @DisplayName("getGroupDefaultRoleById should return ResourceNotFoundException when not found")
    void getGroupDefaultRoleById_NotFound() {
        when(groupDefaultRoleRepository.findById("nonexistent-id")).thenReturn(Mono.empty());

        Mono<GroupDefaultRoleDto> result = groupDefaultRoleService.getGroupDefaultRoleById("nonexistent-id");

        StepVerifier.create(result)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(groupDefaultRoleRepository).findById("nonexistent-id");
        verify(groupDefaultRoleMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("getGroupDefaultRoleByGroupName should return DTO when found")
    void getGroupDefaultRoleByGroupName_Found() {
        when(groupDefaultRoleRepository.findByGroupName(groupName1)).thenReturn(Mono.just(groupRole1));
        when(groupDefaultRoleMapper.toDto(groupRole1)).thenReturn(groupRoleDto1);

        Mono<GroupDefaultRoleDto> result = groupDefaultRoleService.getGroupDefaultRoleByGroupName(groupName1);

        StepVerifier.create(result)
                .expectNext(groupRoleDto1)
                .verifyComplete();

        verify(groupDefaultRoleRepository).findByGroupName(groupName1);
        verify(groupDefaultRoleMapper).toDto(groupRole1);
    }

    @Test
    @DisplayName("getGroupDefaultRoleByGroupName should return ResourceNotFoundException when not found")
    void getGroupDefaultRoleByGroupName_NotFound() {
        when(groupDefaultRoleRepository.findByGroupName("nonexistent-group")).thenReturn(Mono.empty());

        Mono<GroupDefaultRoleDto> result = groupDefaultRoleService.getGroupDefaultRoleByGroupName("nonexistent-group");

        StepVerifier.create(result)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(groupDefaultRoleRepository).findByGroupName("nonexistent-group");
        verify(groupDefaultRoleMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("findByGroupNames should return matching DTOs")
    void findByGroupNames_Success() {
        List<String> searchNames = Arrays.asList(groupName1, "nonexistent", groupName2);
        when(groupDefaultRoleRepository.findByGroupNameIn(searchNames)).thenReturn(Flux.just(groupRole1, groupRole2));
        when(groupDefaultRoleMapper.toDto(groupRole1)).thenReturn(groupRoleDto1);
        when(groupDefaultRoleMapper.toDto(groupRole2)).thenReturn(groupRoleDto2);

        Flux<GroupDefaultRoleDto> result = groupDefaultRoleService.findByGroupNames(searchNames);

        StepVerifier.create(result)
                .expectNext(groupRoleDto1)
                .expectNext(groupRoleDto2)
                .verifyComplete();

        verify(groupDefaultRoleRepository).findByGroupNameIn(searchNames);
        verify(groupDefaultRoleMapper, times(2)).toDto(any(GroupDefaultRole.class));
    }

    @Test
    @DisplayName("updateGroupDefaultRole should update roleIds and return DTO when found")
    void updateGroupDefaultRole_Success() {
        // Simulate the state after mapper updates the entity
        GroupDefaultRole updatedEntityState = GroupDefaultRole.builder()
                .id(groupRole1.getId())
                .groupName(groupRole1.getGroupName()) // groupName not updated
                .roleIds(updateDto.getRoleIds()) // Updated
                .createdBy(groupRole1.getCreatedBy())
                .createdDate(groupRole1.getCreatedDate())
                // Assume auditing updates these
                .lastModifiedBy(testUsername)
                .lastModifiedDate(new Date())
                .build();

        GroupDefaultRoleDto finalDto = new GroupDefaultRoleDto(); // DTO mapped from saved updated entity
        finalDto.setId(updatedEntityState.getId());
        finalDto.setGroupName(updatedEntityState.getGroupName());
        finalDto.setRoleIds(updatedEntityState.getRoleIds());
        finalDto.setCreatedBy(updatedEntityState.getCreatedBy());
        finalDto.setCreatedDate(updatedEntityState.getCreatedDate());
        finalDto.setLastModifiedBy(updatedEntityState.getLastModifiedBy());
        finalDto.setLastModifiedDate(updatedEntityState.getLastModifiedDate());

        when(groupDefaultRoleRepository.findById(groupRoleId1)).thenReturn(Mono.just(groupRole1));
        // Mock the void mapper method
        doNothing().when(groupDefaultRoleMapper).updateEntityFromDto(any(UpdateGroupDefaultRoleDto.class), any(GroupDefaultRole.class));
        when(groupDefaultRoleRepository.save(any(GroupDefaultRole.class))).thenReturn(Mono.just(updatedEntityState));
        when(groupDefaultRoleMapper.toDto(any(GroupDefaultRole.class))).thenReturn(finalDto);

        Mono<GroupDefaultRoleDto> result = groupDefaultRoleService.updateGroupDefaultRole(groupRoleId1, Mono.just(updateDto));

        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.getId().equals(groupRoleId1) &&
                        dto.getRoleIds().equals(updateDto.getRoleIds()))
                .verifyComplete();

        verify(groupDefaultRoleRepository).findById(groupRoleId1);
        verify(groupDefaultRoleMapper).updateEntityFromDto(updateDto, groupRole1);
        verify(groupDefaultRoleRepository).save(groupRole1); // The object passed to save is the one modified by the mapper
        verify(groupDefaultRoleMapper).toDto(updatedEntityState);
    }

    @Test
    @DisplayName("updateGroupDefaultRole should return ResourceNotFoundException when not found")
    void updateGroupDefaultRole_NotFound() {
        when(groupDefaultRoleRepository.findById("nonexistent-id")).thenReturn(Mono.empty());

        Mono<GroupDefaultRoleDto> result = groupDefaultRoleService.updateGroupDefaultRole("nonexistent-id", Mono.just(updateDto));

        StepVerifier.create(result)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(groupDefaultRoleRepository).findById("nonexistent-id");
        verify(groupDefaultRoleMapper, never()).updateEntityFromDto(any(), any());
        verify(groupDefaultRoleRepository, never()).save(any());
        verify(groupDefaultRoleMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("deleteGroupDefaultRole should complete when found")
    void deleteGroupDefaultRole_Success() {
        when(groupDefaultRoleRepository.findById(groupRoleId1)).thenReturn(Mono.just(groupRole1));
        when(groupDefaultRoleRepository.delete(groupRole1)).thenReturn(Mono.empty()); // delete returns Mono<Void>

        Mono<Void> result = groupDefaultRoleService.deleteGroupDefaultRole(groupRoleId1);

        StepVerifier.create(result)
                .verifyComplete();

        verify(groupDefaultRoleRepository).findById(groupRoleId1);
        verify(groupDefaultRoleRepository).delete(groupRole1);
    }

    @Test
    @DisplayName("deleteGroupDefaultRole should return ResourceNotFoundException when not found")
    void deleteGroupDefaultRole_NotFound() {
        when(groupDefaultRoleRepository.findById("nonexistent-id")).thenReturn(Mono.empty());

        Mono<Void> result = groupDefaultRoleService.deleteGroupDefaultRole("nonexistent-id");

        StepVerifier.create(result)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(groupDefaultRoleRepository).findById("nonexistent-id");
        verify(groupDefaultRoleRepository, never()).delete(any());
    }
}