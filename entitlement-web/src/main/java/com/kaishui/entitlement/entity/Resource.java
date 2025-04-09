package com.kaishui.entitlement.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document; // Use BSON Document
import org.springframework.data.annotation.CreatedBy; // Import auditing annotation
import org.springframework.data.annotation.CreatedDate; // Import auditing annotation
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy; // Import auditing annotation
import org.springframework.data.annotation.LastModifiedDate; // Import auditing annotation

import java.util.Date;
import java.util.List; // Import List

@Data
@org.springframework.data.mongodb.core.mapping.Document(collection = "resources") // Specify collection
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    private String name; // e.g., "User Management Page", "Product Catalog"

    // Use List<Document> based on markdown definition
    private List<Document> permission;

    private String type; // e.g., "page", "button", "api", "condition"
    private String description;
    private String region; // e.g., "uk", "sg", "hk"

    @Builder.Default // Ensure default value is set by Lombok builder
    private Boolean isActive = true;

    // Add Spring Data Auditing annotations
    @CreatedBy
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @LastModifiedBy
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String updatedBy; // Mapped to lastModifiedBy

    @CreatedDate
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Date createdDate;

    @LastModifiedDate
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Date lastModifiedDate;
}