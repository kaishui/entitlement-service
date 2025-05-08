package com.kaishui.entitlement.util;


import com.kaishui.entitlement.entity.dto.CreateResourceDto;
import com.kaishui.entitlement.entity.dto.ResourceDto;
import com.kaishui.entitlement.entity.dto.UpdateResourceDto;
import com.kaishui.entitlement.entity.Resource;
import org.bson.Document;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResourceMapper {

    ResourceDto toDto(Resource resource);

    // Let MapStruct do the initial mapping, ignore permission for now
    @Mapping(target = "permission", ignore = true)
    Resource toEntity(CreateResourceDto createDto);

    @AfterMapping
    default void afterMappingCreateDtoToEntity(CreateResourceDto dto, @MappingTarget Resource entity) {
        if (dto.getPermission() != null) {
            entity.setPermission(new Document(dto.getPermission())); // Deep copy
        }
    }

    void updateEntityFromDto(UpdateResourceDto source, @MappingTarget Resource target);

    // Similar @AfterMapping might be needed for updateEntityFromDto if it handles 'permission'
    @AfterMapping
    default void afterMappingUpdateDtoToEntity(UpdateResourceDto dto, @MappingTarget Resource entity) {
        if (dto.getPermission() != null) { // Assuming UpdateResourceDto also has a Document permission
            entity.setPermission(new Document(dto.getPermission())); // Deep copy
        }
    }
}