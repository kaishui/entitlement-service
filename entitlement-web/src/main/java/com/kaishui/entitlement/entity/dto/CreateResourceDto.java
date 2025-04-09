package com.kaishui.entitlement.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Data Transfer Object for creating a new Resource")
public class CreateResourceDto {
    @NotBlank(message = "Resource name cannot be blank")
    @Size(max = 100, message = "Resource name cannot exceed 100 characters")
    @Schema(description = "Name of the resource", requiredMode = Schema.RequiredMode.REQUIRED, example = "Product Catalog API")
    private String name;

    @NotNull(message = "Permission definition cannot be null")
    @Schema(description = "List of permission documents", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "[{\"method\": \"POST\", \"uri\": \"/products\"}]")
    private List<Document> permission; // Use List<Document>

    @NotBlank(message = "Resource type cannot be blank")
    @Size(max = 50, message = "Resource type cannot exceed 50 characters")
    @Schema(description = "Type of the resource", requiredMode = Schema.RequiredMode.REQUIRED, example = "api")
    private String type;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Schema(description = "Optional description of the resource", example = "API endpoint for product data")
    private String description;

    @Size(max = 50, message = "Region cannot exceed 50 characters")
    @Schema(description = "Optional region for the resource", example = "uk")
    private String region;

    @Schema(description = "Set whether the resource is active (defaults to true)", example = "true")
    private boolean isActive = true; // Default value
}