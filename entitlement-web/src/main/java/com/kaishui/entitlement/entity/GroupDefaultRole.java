package com.kaishui.entitlement.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
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
    private ObjectId id;
    private String groupName; // AD group name
    private List<ObjectId> roleIds; // List of default roleIds
    private String createdBy;
    private String updatedBy;
    private Date createdDate;
    private String lastModifiedBy;
    private Date lastModifiedDate;
}