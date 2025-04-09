package com.kaishui.entitlement.util;


import com.kaishui.entitlement.entity.dto.CreateResourceDto;
import com.kaishui.entitlement.entity.dto.ResourceDto;
import com.kaishui.entitlement.entity.dto.UpdateResourceDto;
import com.kaishui.entitlement.entity.Resource;
import org.mapstruct.*;

@Mapper(componentModel = "spring", // Integrates with Spring DI
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, // Ignore nulls during update
        unmappedTargetPolicy = ReportingPolicy.IGNORE) // Ignore unmapped fields
public interface ResourceMapper {

    // --- Entity to DTO ---
    ResourceDto toDto(Resource resource);

    // --- DTO to Entity ---
    Resource toEntity(CreateResourceDto createDto);

    // --- Update Entity from DTO ---
    // This method updates the 'target' entity with non-null values from the 'source' DTO
    void updateEntityFromDto(UpdateResourceDto source, @MappingTarget Resource target);

    // If you need to map UpdateResourceDto directly to a new Resource entity (less common for updates)
    // Resource toEntity(UpdateResourceDto updateDto);
}