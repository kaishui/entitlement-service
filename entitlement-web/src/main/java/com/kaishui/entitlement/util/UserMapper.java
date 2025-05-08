package com.kaishui.entitlement.util;


import com.kaishui.entitlement.entity.Entitlement;
import com.kaishui.entitlement.entity.User;
import com.kaishui.entitlement.entity.dto.UserCreateDto;
import com.kaishui.entitlement.entity.dto.UserDto;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    // --- Entity to DTO ---

    // Entitlements and Resources will be populated by the service layer
    @Mapping(target = "entitlements", ignore = true)
    @Mapping(target = "resources", ignore = true)
    UserDto toDto(User user);

    // --- DTO to Entity ---

    @Mapping(target = "entitlements", ignore = true)
    User toEntity(UserCreateDto createDto);

    // --- Update Entity from DTO ---
    @Mapping(target = "entitlements", ignore = true)
    void updateEntityFromDto(UserDto source, @MappingTarget User target);


    // --- Custom mapping logic ---

    // This method is no longer directly used by toDto for UserDto.entitlements
    // It could be kept if UserDto had an adGroups field for some other purpose,
    // or removed if not needed. For now, I'll keep it as it doesn't harm.
    default List<String> mapEntitlementsToAdGroups(List<Entitlement> entitlements) {
        if (entitlements == null) {
            return Collections.emptyList();
        }
        return entitlements.stream()
                .map(Entitlement::getAdGroup)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @AfterMapping
    default void mapAdGroupsToEntitlements(UserCreateDto dto, @MappingTarget User user) {
        if (dto.getAdGroups() != null && !dto.getAdGroups().isEmpty()) {
            List<Entitlement> entitlements = dto.getAdGroups().stream()
                    .filter(java.util.Objects::nonNull)
                    .map(adGroup -> Entitlement.builder()
                            .adGroup(adGroup)
                            .roleIds(Collections.emptyList())
                            .build())
                    .collect(Collectors.toList());
            user.setEntitlements(entitlements);
        } else {
            user.setEntitlements(Collections.emptyList());
        }
    }
}