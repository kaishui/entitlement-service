package com.kaishui.entitlement.service;


import com.kaishui.entitlement.annotation.AuditLog; // Assuming you have this
import com.kaishui.entitlement.entity.dto.CreateGroupDefaultRoleDto;
import com.kaishui.entitlement.entity.dto.GroupDefaultRoleDto;
import com.kaishui.entitlement.entity.dto.UpdateGroupDefaultRoleDto;
import com.kaishui.entitlement.entity.GroupDefaultRole;
import com.kaishui.entitlement.exception.ConflictException;
import com.kaishui.entitlement.exception.ResourceNotFoundException; // Re-use or create specific one
import com.kaishui.entitlement.repository.GroupDefaultRoleRepository;
import com.kaishui.entitlement.util.GroupDefaultRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupDefaultRoleService {

    private final GroupDefaultRoleRepository groupDefaultRoleRepository;
    private final GroupDefaultRoleMapper groupDefaultRoleMapper;

    @AuditLog(action = "CREATE_GROUP_DEFAULT_ROLE")
    @Transactional
    public Mono<GroupDefaultRoleDto> createGroupDefaultRole(Mono<CreateGroupDefaultRoleDto> createDtoMono) {
        return createDtoMono
                .flatMap(dto -> groupDefaultRoleRepository.existsByGroupName(dto.getGroupName())
                        .flatMap(exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                log.warn("Attempted to create duplicate GroupDefaultRole for group: {}", dto.getGroupName());
                                return Mono.error(new ConflictException("GroupDefaultRole mapping already exists for group: " + dto.getGroupName()));
                            }
                            log.info("Creating GroupDefaultRole for group: {}", dto.getGroupName());
                            GroupDefaultRole entity = groupDefaultRoleMapper.toEntity(dto);
                            // Auditing fields handled by framework
                            return groupDefaultRoleRepository.save(entity);
                        }))
                .map(groupDefaultRoleMapper::toDto)
                .doOnSuccess(savedDto -> log.info("Successfully created GroupDefaultRole with ID: {}", savedDto.getId()))
                .doOnError(ConflictException.class, e -> log.warn(e.getMessage()))
                .doOnError(e -> !(e instanceof ConflictException), e -> log.error("Error creating GroupDefaultRole: {}", e.getMessage(), e));
    }

    public Flux<GroupDefaultRoleDto> getAllGroupDefaultRoles() {
        log.debug("Fetching all GroupDefaultRoles");
        return groupDefaultRoleRepository.findAll()
                .map(groupDefaultRoleMapper::toDto)
                .doOnError(e -> log.error("Error fetching all GroupDefaultRoles: {}", e.getMessage(), e));
    }

    public Mono<GroupDefaultRoleDto> getGroupDefaultRoleById(String id) {
        log.debug("Fetching GroupDefaultRole by ID: {}", id);
        return groupDefaultRoleRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("GroupDefaultRole not found with id: " + id)))
                .map(groupDefaultRoleMapper::toDto)
                .doOnError(ResourceNotFoundException.class, e -> log.warn(e.getMessage()))
                .doOnError(e -> !(e instanceof ResourceNotFoundException), e -> log.error("Error fetching GroupDefaultRole by ID {}: {}", id, e.getMessage(), e));
    }

    public Mono<GroupDefaultRoleDto> getGroupDefaultRoleByGroupName(String groupName) {
        log.debug("Fetching GroupDefaultRole by groupName: {}", groupName);
        return groupDefaultRoleRepository.findByGroupName(groupName)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("GroupDefaultRole not found for group: " + groupName)))
                .map(groupDefaultRoleMapper::toDto)
                .doOnError(ResourceNotFoundException.class, e -> log.warn(e.getMessage()))
                .doOnError(e -> !(e instanceof ResourceNotFoundException), e -> log.error("Error fetching GroupDefaultRole by groupName {}: {}", groupName, e.getMessage(), e));
    }

    public Flux<GroupDefaultRoleDto> findByGroupNames(List<String> groupNames) {
        log.debug("Fetching GroupDefaultRoles for groups: {}", groupNames);
        return groupDefaultRoleRepository.findByGroupNameIn(groupNames)
                .map(groupDefaultRoleMapper::toDto)
                .doOnError(e -> log.error("Error fetching GroupDefaultRoles by group names: {}", e.getMessage(), e));
    }


    @AuditLog(action = "UPDATE_GROUP_DEFAULT_ROLE")
    @Transactional
    public Mono<GroupDefaultRoleDto> updateGroupDefaultRole(String id, Mono<UpdateGroupDefaultRoleDto> updateDtoMono) {
        return groupDefaultRoleRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("GroupDefaultRole not found for update with id: " + id)))
                .zipWith(updateDtoMono, (existingEntity, updateDto) -> {
                    log.info("Attempting to update GroupDefaultRole ID: {}", id);
                    groupDefaultRoleMapper.updateEntityFromDto(updateDto, existingEntity);
                    // Auditing fields handled by framework
                    return existingEntity;
                })
                .flatMap(groupDefaultRoleRepository::save)
                .map(groupDefaultRoleMapper::toDto)
                .doOnSuccess(savedDto -> log.info("Successfully updated GroupDefaultRole ID: {}", savedDto.getId()))
                .doOnError(ResourceNotFoundException.class, e -> log.warn("Update failed: {}", e.getMessage()))
                .doOnError(e -> !(e instanceof ResourceNotFoundException), e -> log.error("Error updating GroupDefaultRole ID {}: {}", id, e.getMessage(), e));
    }

    @AuditLog(action = "DELETE_GROUP_DEFAULT_ROLE")
    @Transactional
    public Mono<Void> deleteGroupDefaultRole(String id) {
        log.info("Attempting to delete GroupDefaultRole ID: {}", id);
        // Ensure it exists before deleting
        return groupDefaultRoleRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("GroupDefaultRole not found for deletion with id: " + id)))
                .flatMap(entity -> {
                    log.info("Found GroupDefaultRole for group '{}' for deletion.", entity.getGroupName());
                    return groupDefaultRoleRepository.delete(entity); // Use delete(entity) or deleteById(id)
                })
                .doOnSuccess(v -> log.info("Successfully deleted GroupDefaultRole ID: {}", id))
                .doOnError(ResourceNotFoundException.class, e -> log.warn("Deletion failed: {}", e.getMessage()))
                .doOnError(e -> !(e instanceof ResourceNotFoundException), e -> log.error("Error deleting GroupDefaultRole ID {}: {}", id, e.getMessage(), e));
    }
}