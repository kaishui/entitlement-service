package com.kaishui.entitlement.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Data Transfer Object for updating an existing Group Default Role mapping")
public class UpdateGroupDefaultRoleDto {
    // groupName is typically not updatable as it's the key identifier,
    // but if needed, add validation. Usually, you delete and recreate.

    @NotEmpty(message = "Role IDs list cannot be empty when updating") // Ensure roles are provided for update
    @Schema(description = "Updated list of default role IDs. This will replace the existing list.", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"roleId5\"]")
    private List<String> roleIds; // Only allow updating the role list
}
