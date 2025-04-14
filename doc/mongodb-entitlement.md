## 1. User Collections

```
{
  "_id": String,
  // MongoDB's default primary key
  "username": String,
  "staffId": String,
  "email": String,
  "department": String,
  "functionalManager": String,
  "entityManager": String,
  "jobTitle": String,
  "isActive": boolean,
  // e.g., true - "active", false - "inactive"
  "createdBy": String,
  "updatedBy": String,
  "createdDate": Date,
  "lastModifiedDate": Date
  "adGroups": [
    String
  ],
  // Array of AD group names (e.g., ["AD_Users", "AD_Developers"])
  "roleIds": [
    String
  ]
  // Array of roleIds
}
```

## 2. Role Collections

```
{
  "_id": String,
  "roleName": String, // e.g., "Global Admin", "Regional Admin", "Approver", "User"
  "type": String, // e.g., "global", "regional", "user"
  "isApprover": Boolean, // Indicates if the role has approval permissions
  "description": String,
  "createdBy": String,
  "updatedBy": String,
  "createdDate": Date,
  "lastModifiedDate": Date
  "region": String, // e.g., "EMEA", "APAC", "NA"
  "resourceIds": [String] // Array of resourceIds
  "isActive": boolean,
  // e.g., true - "active", false - "inactive"
}
```

## 3. Resource Collections

```
{
  "_id": String,
  "name": String, // e.g., "User Management Page", "Product Catalog"
  "permission": List<Document>, // e.g., [{method: "GET: uri: "/users/*"}], [method: "POST", uri: "/products"}, ["xxPage.button.view"], [{date: 20250101}] , [{jobStatus: "pending"}, {jobStatus: "completed"}]
  "type": String, // e.g., "page", "button", "api", condition
  "description": String,
  "createdBy": String,
  "updatedBy": String,
  "createdDate": Date,
  "lastModifiedDate": Date
  "isActive": boolean,
  // e.g., true - "active", false - "inactive"
  "adGroups": List<String>, // e.g., ["uk-admin", "sg-regional-user", "hk-user"]

}
```
for example:
```json
{
  "name": "User Management PAGE",
  "type": "URI",
  "permission": [
    {"code":  "P_USER_MANAGEMENT"}
  ],
  "adGroups": ["hk-admin", "sg-admin"]
}
```
```json

{
  "name": "User Management uri",
  "type": "URI",
  "permission": [
    {"code":  "B_USER_ALL", "method" :  "*", "uri":  "/users/*", "parentPage":  "P_USER_MANAGEMENT"}
  ],
  "adGroups": ["hk-admin", "sg-admin"]
}

```



## 4. Group default role Collection

```
{
  "_id": String,
  "groupName": String, // AD group name (e.g., "AD_Approvers", "AD_Users")
  "roleId": [String], // Reference to a document in the `roles` collection
  "createdBy": String,
  "updatedBy": String,
  "createdDate": Date,
  "lastModifiedDate": Date
  // ... other group-default-role-related fields ...
}

```

## 6. Audit Log Collections

```
{
  "_id": String,
  "userId": String, // Reference to a document in the `users` collection staff id
  "action": String, // e.g., "user_created", "role_assigned", "resource_accessed"
  "detail": Document,
  "createdBy": String,
  "updatedBy": String,
  "createdDate": Date,
  "lastModifiedBy": String
   "isActive": boolean,
  // e.g., true - "active", false - "inactive"
  }
```

## 7. hierarchy approval Collections
```
{
  "_id": String,
  "groupName": String, // AD group name (e.g., "AD_Approvers", "AD_Users")
  "parentGroupName": String, // Parent AD group name (e.g., "AD_Global_Approvers")
  "level": Number, // Hierarchy level (e.g., 0 for root, 1 for first level, etc.)
  "createdBy": String,
  "updatedBy": String,
  "createdDate": Date,
  "lastModifiedDate": Date,
  "isActive": Boolean // e.g., true - "active", false - "inactive"
}

```

eg:
假设我们有以下层级关系：
AD_Global_Approvers (根节点)
    -- AD_Regional_Approvers (第一级子节点)
        -- AD_Country_Approvers (第二级子节点)
```
[
  {
    "_id": "1",
    "groupName": "AD_Global_Approvers",
    "parentGroupName": null,
    "level": 0,
    "createdBy": "admin",
    "updatedBy": "admin",
    "createdDate": ISODate("2023-10-01T00:00:00Z"),
    "lastModifiedDate": ISODate("2023-10-01T00:00:00Z"),
    "isActive": true
  },
  {
    "_id": "2",
    "groupName": "AD_Regional_Approvers",
    "parentGroupName": "AD_Global_Approvers",
    "level": 1,
    "createdBy": "admin",
    "updatedBy": "admin",
    "createdDate": ISODate("2023-10-01T00:00:00Z"),
    "lastModifiedDate": ISODate("2023-10-01T00:00:00Z"),
    "isActive": true
  },
  {
    "_id": "3",
    "groupName": "AD_Country_Approvers",
    "parentGroupName": "AD_Regional_Approvers",
    "level": 2,
    "createdBy": "admin",
    "updatedBy": "admin",
    "createdDate": ISODate("2023-10-01T00:00:00Z"),
    "lastModifiedDate": ISODate("2023-10-01T00:00:00Z"),
    "isActive": true
  }
]

```