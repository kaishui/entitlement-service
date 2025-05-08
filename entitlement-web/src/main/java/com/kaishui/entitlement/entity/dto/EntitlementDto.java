package com.kaishui.entitlement.entity.dto;

import com.kaishui.entitlement.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EntitlementDto {

    private String adGroup;
    private List<Role> roles;
}
