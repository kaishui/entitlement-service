package com.kaishui.entitlement.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Data Transfer Object for creating a new Group Default Role mapping")
public class CreateGroupDefaultRoleDto {
    @NotBlank(message = "Group name cannot be blank")
    @Size(max = 100, message = "Group name cannot exceed 100 characters")
    @Schema(description = "AD group name", requiredMode = Schema.RequiredMode.REQUIRED, example = "sg-users")
    private String groupName;

    @NotEmpty(message = "Role IDs list cannot be empty")
    @Schema(description = "List of default role IDs", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"roleId3\", \"roleId4\"]")
    private List<String> roleIds;
}
