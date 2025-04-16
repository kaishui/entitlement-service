package com.kaishui.entitlement.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Data Transfer Object for Resource details")
public class UserResourceDto {
    @Schema(description = "Unique identifier of the resource", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotBlank(message = "Resource name cannot be blank if provided for update")
    @Schema(description = "Name of the resource", example = "User Management Page")
    private String name;

    @Schema(description = "List of permission documents associated with the resource",
            example = "[{\"method\": \"GET\", \"uri\": \"/users/*\"}, {\"page\": \"userAdmin\", \"action\": \"view\"}]")
    private List<Document> permission;

    @Schema(description = "Type of the resource", example = "page")
    private String type;

    @Schema(description = "Description of the resource", example = "Page for managing user accounts")
    private String description;

}