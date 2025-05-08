package com.kaishui.entitlement.entity.dto;

import com.kaishui.entitlement.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDto {
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) // Mark as read-only for OpenAPI
    private String id;

    private String username;

    private String staffId;

    private String email;
    private String department;
    private String functionalManager;
    private String entityManager;
    private String jobTitle;

    @Builder.Default
    private boolean isActive = true;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String lastModifiedBy;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Date createdDate;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Date lastModifiedDate;

    private List<String> adGroups;
    @Builder.Default // Add default for isFirstLogin if needed
    private boolean isFirstLogin = true;
}