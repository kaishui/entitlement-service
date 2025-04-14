package com.kaishui.entitlement.web; // Assuming controller is in this package

import com.kaishui.entitlement.entity.dto.CreateGroupDefaultRoleDto;
import com.kaishui.entitlement.entity.dto.GroupDefaultRoleDto;
import com.kaishui.entitlement.entity.dto.UpdateGroupDefaultRoleDto;
import com.kaishui.entitlement.service.GroupDefaultRoleService;
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

import java.util.List;

@RestController
@RequestMapping("/v1/api/group-default-roles") // Base path
@RequiredArgsConstructor
@Tag(name = "Group Default Role Management", description = "APIs for managing default roles assigned to groups")
public class GroupDefaultRoleController {

    private final GroupDefaultRoleService groupDefaultRoleService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a group default role mapping",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Mapping created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data"),
                    @ApiResponse(responseCode = "409", description = "Mapping already exists for this group")
            })
    public Mono<GroupDefaultRoleDto> createGroupDefaultRole(
            @Valid @RequestBody Mono<CreateGroupDefaultRoleDto> createDtoMono) {
        return groupDefaultRoleService.createGroupDefaultRole(createDtoMono);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all group default role mappings",
            description = "Retrieves all mappings or filters by a list of group names.",
            responses = @ApiResponse(responseCode = "200", description = "Successfully retrieved mappings"))
    public Flux<GroupDefaultRoleDto> getGroupDefaultRoles(
            @Parameter(description = "Optional list of group names to filter by")
            @RequestParam(required = false) List<String> groupNames) {
        if (groupNames != null && !groupNames.isEmpty()) {
            return groupDefaultRoleService.findByGroupNames(groupNames);
        }
        return groupDefaultRoleService.getAllGroupDefaultRoles();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a mapping by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Mapping found"),
                    @ApiResponse(responseCode = "404", description = "Mapping not found")
            })
    public Mono<ResponseEntity<GroupDefaultRoleDto>> getGroupDefaultRoleById(
            @Parameter(description = "ID of the mapping", required = true) @PathVariable String id) {
        return groupDefaultRoleService.getGroupDefaultRoleById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/by-group/{groupName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a mapping by Group Name",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Mapping found"),
                    @ApiResponse(responseCode = "404", description = "Mapping not found")
            })
    public Mono<ResponseEntity<GroupDefaultRoleDto>> getGroupDefaultRoleByGroupName(
            @Parameter(description = "Name of the AD group", required = true) @PathVariable String groupName) {
        return groupDefaultRoleService.getGroupDefaultRoleByGroupName(groupName)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a mapping by ID",
            description = "Updates the list of role IDs for a given mapping ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Mapping updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data"),
                    @ApiResponse(responseCode = "404", description = "Mapping not found")
            })
    public Mono<ResponseEntity<GroupDefaultRoleDto>> updateGroupDefaultRole(
            @Parameter(description = "ID of the mapping to update", required = true) @PathVariable String id,
            @Valid @RequestBody Mono<UpdateGroupDefaultRoleDto> updateDtoMono) {
        return groupDefaultRoleService.updateGroupDefaultRole(id, updateDtoMono)
                .map(ResponseEntity::ok)
                // Service handles not found, but defaultIfEmpty is a safeguard
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a mapping by ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Mapping deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Mapping not found")
            })
    public Mono<Void> deleteGroupDefaultRole(
            @Parameter(description = "ID of the mapping to delete", required = true) @PathVariable String id) {
        return groupDefaultRoleService.deleteGroupDefaultRole(id);
    }
}