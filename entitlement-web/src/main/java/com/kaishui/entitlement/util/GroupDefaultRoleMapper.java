package com.kaishui.entitlement.util;


import com.kaishui.entitlement.entity.GroupDefaultRole;
import com.kaishui.entitlement.entity.dto.CreateGroupDefaultRoleDto;
import com.kaishui.entitlement.entity.dto.GroupDefaultRoleDto;
import com.kaishui.entitlement.entity.dto.UpdateGroupDefaultRoleDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupDefaultRoleMapper {

    GroupDefaultRoleDto toDto(GroupDefaultRole entity);

    GroupDefaultRole toEntity(CreateGroupDefaultRoleDto createDto);

    // Update only roleIds from the DTO
    @Mapping(target = "roleIds", source = "roleIds")
    void updateEntityFromDto(UpdateGroupDefaultRoleDto source, @MappingTarget GroupDefaultRole target);
}