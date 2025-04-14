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
