package com.kaishui.entitlement.entity;

import io.swagger.v3.oas.annotations.media.Schema; // Add for consistency
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
// No need for ObjectId import here
import org.springframework.data.annotation.CreatedBy; // Import auditing annotation
import org.springframework.data.annotation.CreatedDate; // Import auditing annotation
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy; // Import auditing annotation
import org.springframework.data.annotation.LastModifiedDate; // Import auditing annotation
import org.springframework.data.mongodb.core.index.Indexed; // Import for index
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document(collection = "groupDefaultRoles")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDefaultRole {
    @Id
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @Indexed(unique = true) // Ensure groupName is unique
    @Schema(description = "AD group name", example = "uk-admin")
    private String groupName;

    @Schema(description = "List of default role IDs associated with the group", example = "[\"roleId1\", \"roleId2\"]")
    private List<String> roleIds;

    // Add Spring Data Auditing annotations
    @CreatedBy
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    // Note: 'updatedBy' is typically mapped by @LastModifiedBy
    // Remove 'updatedBy' field if you only use @LastModifiedBy
    // private String updatedBy; // Remove this if using @LastModifiedBy

    @LastModifiedBy
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String lastModifiedBy; // This will store the last modifier

    @CreatedDate
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Date createdDate;

    @LastModifiedDate
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Date lastModifiedDate;
}