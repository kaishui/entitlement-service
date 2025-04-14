package com.kaishui.entitlement.service;

import com.kaishui.entitlement.annotation.AuditLog;
import com.kaishui.entitlement.constant.ResourceType;
import com.kaishui.entitlement.entity.dto.CreateResourceDto;
import com.kaishui.entitlement.entity.dto.ResourceDto;
import com.kaishui.entitlement.entity.dto.UpdateResourceDto;
import com.kaishui.entitlement.entity.Resource;
import com.kaishui.entitlement.exception.ResourceNotFoundException; // Assuming you create this
import com.kaishui.entitlement.util.ResourceMapper; // Using MapStruct (recommended)
import com.kaishui.entitlement.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Optional for consistency if needed
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public Flux<ResourceDto> findResources(String name, String type, List<String> adGroups, Boolean isActive) {
        log.debug("Searching resources - Name: '{}', Type: '{}', Region: '{}', Active: {}", name, type, adGroups, isActive);

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
        if (adGroups != null && !adGroups.isEmpty()) {
            // Use the new repository method
            results = results.filter(r-> r.getAdGroups() != null && !r.getAdGroups().isEmpty() && !java.util.Collections.disjoint(r.getAdGroups(),adGroups));
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

    /**
     * Finds all resources matching the given IDs and type 'CONDITION',
     * extracts their permission documents, and returns them as a single set.
     *
     * @param resourceIds A list of resource IDs to search for.
     * @return A Mono emitting a Set containing all unique permission Documents found,
     *         or an empty Set if no matching resources or permissions are found.
     */
    public Mono<Set<Document>> getConditionPermissionsForResources(List<String> resourceIds) {
        if (CollectionUtils.isEmpty(resourceIds)) {
            log.debug("Resource ID list is empty, returning empty set of permissions.");
            return Mono.just(Collections.emptySet()); // Return empty set if no IDs provided
        }

        log.debug("Fetching CONDITION permissions for resource IDs: {}", resourceIds);

        // Use the appropriate repository method (adjust if you need isActive filter)
        // Assuming you added findAllByIdInAndType:
        return resourceRepository.findAllByIdInAndTypeAndIsActive(resourceIds, ResourceType.CONDITION.name(), true)
                // Alternative if you only have findAllByIdInAndTypeAndIsActive and want active ones:
                // return resourceRepository.findAllByIdInAndTypeAndIsActive(resourceIds, ResouceType.CONDITION.name(), true)

                // Filter out resources that don't have permissions (optional but good practice)
                .filter(resource -> !CollectionUtils.isEmpty(resource.getPermission()))
                // Get the list of permission documents from each resource
                .map(Resource::getPermission)
                // Flatten the Flux<List<Document>> into a Flux<Document>
                .flatMap(Flux::fromIterable)
                // Collect all unique documents into a Set
                .collect(Collectors.toSet()) // collectList().map(HashSet::new) also works
                .doOnSuccess(permissionSet -> log.info("Collected {} unique CONDITION permission documents for resource IDs: {}", permissionSet.size(), resourceIds))
                .doOnError(e -> log.error("Error fetching CONDITION permissions for resource IDs {}: {}", resourceIds, e.getMessage(), e))
                // If the stream is empty (no matching resources or permissions), return an empty set
                .defaultIfEmpty(Collections.emptySet());
    }
}