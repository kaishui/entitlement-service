package com.kaishui.entitlement.util;


import com.kaishui.entitlement.entity.User;
import com.kaishui.entitlement.entity.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", // Integrates with Spring DI
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, // Ignore nulls during update
        unmappedTargetPolicy = ReportingPolicy.IGNORE) // Ignore unmapped fields
public interface UserMapper {

    // --- Entity to DTO ---
    UserDto toDto(User user);

    // --- DTO to Entity ---
    User toEntity(UserDto createDto);

    // --- Update Entity from DTO ---
    // This method updates the 'target' entity with non-null values from the 'source' DTO
    void updateEntityFromDto(UserDto source, @MappingTarget User target);
}