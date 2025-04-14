package com.kaishui.entitlement.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Data Transfer Object for Group Default Role details")
public class GroupDefaultRoleDto {
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @Schema(description = "AD group name", example = "uk-admin")
    private String groupName;

    @Schema(description = "List of default role IDs", example = "[\"roleId1\", \"roleId2\"]")
    private List<String> roleIds;

    @Schema(description = "User who created the record", accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @Schema(description = "User who last updated the record", accessMode = Schema.AccessMode.READ_ONLY)
    private String lastModifiedBy;

    @Schema(description = "Date when the record was created", accessMode = Schema.AccessMode.READ_ONLY)
    private Date createdDate;

    @Schema(description = "Date when the record was last modified", accessMode = Schema.AccessMode.READ_ONLY)
    private Date lastModifiedDate;
}
