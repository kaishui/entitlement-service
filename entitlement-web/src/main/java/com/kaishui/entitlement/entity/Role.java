package com.kaishui.entitlement.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank; // For validation
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
@Document(collection = "roles") // Match collection name from doc
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotBlank(message = "Role name cannot be blank")
    @Indexed(unique = true) // Assuming role names should be unique
    private String roleName;

    private String type; // e.g., "global", "regional", "user"

    @Builder.Default
    private Boolean isApprover = false;

    private String description;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String updatedBy;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Date createdDate;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Date lastModifiedDate;

    private List<String> resourceIds; // Array of resourceIds

    @Builder.Default
    private boolean isActive = true;
}