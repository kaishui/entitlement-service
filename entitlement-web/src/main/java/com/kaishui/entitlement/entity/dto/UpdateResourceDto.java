package com.kaishui.entitlement.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Data Transfer Object for updating an existing Resource")
public class UpdateResourceDto {
    // Fields are optional for partial updates, but validated if present

    @Size(max = 100, message = "Resource name cannot exceed 100 characters")
    @Schema(description = "Updated name of the resource", example = "Product Catalog API v2")
    private String name;

    @Schema(description = "Updated list of permission documents",
            example = "[{\"method\": \"PUT\", \"uri\": \"/products/{id}\"}]")
    private List<Document> permission; // Allow updating permission

    @Size(max = 50, message = "Resource type cannot exceed 50 characters")
    @Schema(description = "Updated type of the resource", example = "api")
    private String type;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Schema(description = "Updated description", example = "API endpoint for product data (version 2)")
    private String description;

    @Size(max = 50, message = "Region cannot exceed 50 characters")
    @Schema(description = "Updated region", example = "emea")
    private String region;

    @Schema(description = "Updated active status", example = "false")
    private Boolean isActive;
}