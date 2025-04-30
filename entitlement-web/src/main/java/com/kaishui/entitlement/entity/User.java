package com.kaishui.entitlement.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document(collection = "users")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) // Mark as read-only for OpenAPI
    private String id;

    @NotBlank(message = "Username cannot be blank") // Add this annotation
    private String username;

    @Indexed(unique = true)
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
    private List<String> roleIds;
    @Builder.Default // Add default for isFirstLogin if needed
    private boolean isFirstLogin = true;
}