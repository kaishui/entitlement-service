package com.kaishui.entitlement.web;


import com.kaishui.entitlement.entity.dto.CreateResourceDto;
import com.kaishui.entitlement.entity.dto.ResourceDto;
import com.kaishui.entitlement.entity.dto.UpdateResourceDto;
import com.kaishui.entitlement.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/resources") // Base path for resource endpoints
@RequiredArgsConstructor
@Tag(name = "Resource Management", description = "APIs for managing Resources") // OpenAPI Tag
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new resource",
            description = "Adds a new resource definition to the system.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Resource created successfully",
                            content = @Content(schema = @Schema(implementation = ResourceDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input data")
            })
    public Mono<ResourceDto> createResource(
            @Parameter(description = "Resource details for creation", required = true)
            @Valid @RequestBody Mono<CreateResourceDto> createDtoMono) {
        // @Valid triggers validation on the DTO
        return resourceService.createResource(createDtoMono);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Find resources",
            description = "Retrieves a list of resources, optionally filtered by name, type, region, or active status.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved resources")
            })
    public Flux<ResourceDto> findResources(
            @Parameter(description = "Filter by resource name (case-insensitive, partial match)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filter by resource type (exact match)")
            @RequestParam(required = false) String type,
            @Parameter(description = "Filter by resource region (exact match)")
            @RequestParam(required = false) String region,
            @Parameter(description = "Filter by active status (true or false)")
            @RequestParam(required = false) Boolean isActive) {
        // Service method handles the filtering logic
        return resourceService.findResources(name, type, region, isActive);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a resource by ID",
            description = "Retrieves the details of a specific resource by its unique ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resource found",
                            content = @Content(schema = @Schema(implementation = ResourceDto.class))),
                    @ApiResponse(responseCode = "404", description = "Resource not found")
            })
    public Mono<ResponseEntity<ResourceDto>> getResourceById(
            @Parameter(description = "ID of the resource to retrieve", required = true)
            @PathVariable String id) {
        return resourceService.getResourceById(id)
                .map(ResponseEntity::ok) // If found, return 200 OK with body
                .defaultIfEmpty(ResponseEntity.notFound().build()); // If not found (empty Mono), return 404
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an existing resource",
            description = "Updates the details of an existing resource by its ID. Only provided fields are updated.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resource updated successfully",
                            content = @Content(schema = @Schema(implementation = ResourceDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input data"),
                    @ApiResponse(responseCode = "404", description = "Resource not found")
            })
    public Mono<ResponseEntity<ResourceDto>> updateResource(
            @Parameter(description = "ID of the resource to update", required = true)
            @PathVariable String id,
            @Parameter(description = "Updated resource details", required = true)
            @Valid @RequestBody Mono<UpdateResourceDto> updateDtoMono) {
        return resourceService.updateResource(id, updateDtoMono)
                .map(ResponseEntity::ok) // Return 200 OK with updated body
                .defaultIfEmpty(ResponseEntity.notFound().build()); // Should be handled by service exception, but good practice
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Return 204 No Content on successful deletion
    @Operation(summary = "Delete a resource by ID",
            description = "Permanently deletes a resource by its unique ID.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Resource deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Resource not found")
            })
    public Mono<Void> deleteResource(
            @Parameter(description = "ID of the resource to delete", required = true)
            @PathVariable String id) {
        return resourceService.deleteResource(id);
    }
}