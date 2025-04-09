package com.kaishui.entitlement.service;

import com.kaishui.entitlement.annotation.AuditLog;
import com.kaishui.entitlement.entity.dto.CreateResourceDto;
import com.kaishui.entitlement.entity.dto.ResourceDto;
import com.kaishui.entitlement.entity.dto.UpdateResourceDto;
import com.kaishui.entitlement.entity.Resource;
import com.kaishui.entitlement.exception.ResourceNotFoundException; // Assuming you create this
import com.kaishui.entitlement.util.ResourceMapper; // Using MapStruct (recommended)
import com.kaishui.entitlement.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Optional for consistency if needed
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper; // Inject the mapper

    @AuditLog(action = "CREATE_RESOURCE")
    @Transactional // Optional: Use if multiple reactive operations need atomicity (requires reactive transaction manager)
    public Mono<ResourceDto> createResource(Mono<CreateResourceDto> createDtoMono) {
        return createDtoMono
                .map(resourceMapper::toEntity) // Map DTO to Entity
                .flatMap(resource -> {
                    log.info("Attempting to create resource: {}", resource.getName());
                    // Auditing fields (createdBy, createdDate) are handled by @EnableReactiveMongoAuditing
                    return resourceRepository.save(resource);
                })
                .map(resourceMapper::toDto) // Map saved Entity back to DTO
                .doOnSuccess(savedDto -> log.info("Successfully created resource with ID: {}", savedDto.getId()))
                .doOnError(e -> log.error("Error creating resource: {}", e.getMessage(), e));
    }

    public Flux<ResourceDto> findResources(String name, String type, String region, Boolean isActive) {
        log.debug("Searching resources - Name: '{}', Type: '{}', Region: '{}', Active: {}", name, type, region, isActive);

        // Start with a base Flux (e.g., find by active status if provided, else find all)
        Flux<Resource> results = (isActive != null)
                ? resourceRepository.findByIsActive(isActive)
                : resourceRepository.findAll();

        // Apply filters reactively
        if (name != null && !name.isBlank()) {
            // Use repository method if available and indexed for better performance
            // results = resourceRepository.findByNameContainingIgnoreCaseAndIsActive(name, isActive != null ? isActive : true); // Example repo method
            // Or filter in memory (less efficient for large datasets without index)
            results = results.filter(r -> r.getName() != null && r.getName().toLowerCase().contains(name.toLowerCase()));
        }
        if (type != null && !type.isBlank()) {
            results = results.filter(r -> type.equalsIgnoreCase(r.getType()));
        }
        if (region != null && !region.isBlank()) {
            results = results.filter(r -> region.equalsIgnoreCase(r.getRegion()));
        }
        // isActive filter is already applied at the start if provided

        return results
                .map(resourceMapper::toDto)
                .doOnError(e -> log.error("Error searching resources: {}", e.getMessage(), e))
                .doOnComplete(() -> log.debug("Resource search completed."));
    }


    public Mono<ResourceDto> getResourceById(String id) {
        log.debug("Fetching resource by ID: {}", id);
        return resourceRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Resource not found with id: " + id)))
                .map(resourceMapper::toDto)
                .doOnError(ResourceNotFoundException.class, e -> log.warn("Resource lookup failed: {}", e.getMessage()))
                .doOnError(e -> !(e instanceof ResourceNotFoundException), e -> log.error("Error fetching resource by ID {}: {}", id, e.getMessage(), e));
    }

    @AuditLog(action = "UPDATE_RESOURCE")
    @Transactional // Optional
    public Mono<ResourceDto> updateResource(String id, Mono<UpdateResourceDto> updateDtoMono) {
        return resourceRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Resource not found for update with id: " + id)))
                .zipWith(updateDtoMono, (existingResource, updateDto) -> {
                    log.info("Attempting to update resource ID: {}", id);
                    // Use mapper to update only non-null fields from DTO
                    resourceMapper.updateEntityFromDto(updateDto, existingResource);
                    // Auditing fields (updatedBy, lastModifiedDate) handled by @EnableReactiveMongoAuditing
                    return existingResource;
                })
                .flatMap(resourceRepository::save) // Save the updated resource
                .map(resourceMapper::toDto)
                .doOnSuccess(savedDto -> log.info("Successfully updated resource ID: {}", savedDto.getId()))
                .doOnError(ResourceNotFoundException.class, e -> log.warn("Resource update failed: {}", e.getMessage()))
                .doOnError(e -> !(e instanceof ResourceNotFoundException), e -> log.error("Error updating resource ID {}: {}", id, e.getMessage(), e));
    }

    @AuditLog(action = "DELETE_RESOURCE")
    @Transactional // Optional
    public Mono<Void> deleteResource(String id) {
        log.info("Attempting to delete resource ID: {}", id);
        return resourceRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Resource not found for deletion with id: " + id)))
                .flatMap(resource -> {
                    log.info("Found resource '{}' for deletion.", resource.getName());
                    return resourceRepository.delete(resource); // Delete the found resource
                })
                .doOnSuccess(v -> log.info("Successfully deleted resource ID: {}", id))
                .doOnError(ResourceNotFoundException.class, e -> log.warn("Resource deletion failed: {}", e.getMessage()))
                .doOnError(e -> !(e instanceof ResourceNotFoundException), e -> log.error("Error deleting resource ID {}: {}", id, e.getMessage(), e));
    }
}