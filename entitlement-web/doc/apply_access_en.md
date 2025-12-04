# Introduction to "Apply Access" Feature

## 1. Background and Objective

Building upon our existing entitlement system based on RBAC (Role-Based Access Control) and User Role Attributes, we are
introducing the new "Apply Access" feature.

**Objective:** To provide a highly flexible and configurable access application workflow that accommodates complex
permission combinations for various business scenarios. It is designed to be compatible with the existing entitlement
system and support a smooth future migration.

---

## 2. Core Feature Overview

The core of the "Apply Access" feature is a **unified configuration model that drives different access application
scenarios**.

### 2.1. Function List

We categorize different roles (Roles) by their functional areas (Functions). When applying for access, a user first
selects a function, and the system dynamically displays a list of related roles for the user to choose from.

**Core Design:**

- **Categorization by Function:** Organizes numerous roles by business function, simplifying the search and
  understanding process for users.
- **Dynamic Role List:** Accurately displays relevant roles based on the selected function.
- **Visibility Control:** We have implemented a `true`/`false` toggle to control whether certain internal roles (e.g.,
  IT-Ready-Only) are visible to regular users.

### 2.2. Core Concepts

- **Role:** The basic unit of permission.
- **Entity:** A business entity, such as a specific business unit or system.
- **GB (Global Business):** A global business line.
- **Function:** A functional area used to logically group roles.

---

## 3. Scenario-based Configuration and Examples

We use a unified `use_case_config` table to define and drive all access application scenarios. This means that extending
to new application scenarios in the future only requires configuration changes, with no code modification needed.

Here are some typical use cases:

### 3.1. Scenario 1: Complex Permission Combination (e.g., IRRBB)

This scenario is for complex cases where unique permissions are determined by multiple dimensions.

- **Permission Combination:** `Role` + `Entity` + `GB` + `Function`
- **Configuration Example:**
  ```json
  {
    "use_case": "IRRBB",
    "hierarchy": ["Function", "GB", "Entity", "Role"],
    "roles": [
      {"name": "IRRBB_Analyst", "visible": true},
      {"name": "IRRBB_Admin", "visible": false}
    ]
  }
  ```

### 3.2. Scenario 2: Entity and Role Combination

- **Permission Combination:** `Entity` + `Role`
- **Configuration Example:**
  ```json
  {
    "use_case": "SimpleEntityAccess",
    "hierarchy": ["Entity", "Role"],
    ...
  }
  ```

### 3.3. Scenario 3: Role and Custom List Combination

- **Permission Combination:** `Role` + `Custom List` (e.g., A, B, C)
- **Configuration Example:**
  ```json
  {
    "use_case": "CustomListAccess",
    "hierarchy": ["Role", "CustomList"],
    "custom_lists": {
        "Market": ["A", "B", "C"]
    },
    ...
  }
  ```

### 3.4. Scenario 4: Single Role Application

The simplest scenario where a user directly applies for a single role.

- **Permission Combination:** `Role`
- **Configuration Example:**
  ```json
  {
    "use_case": "SingleRoleAccess",
    "hierarchy": ["Role"],
    ...
  }
  ```

---

## 4. Approval Workflow

### 4.1. Maker / Checker Mechanism

- **Maker:** The requester of the permission.
- **Checker:** The approver of the permission.
- **Core Rule:** To ensure separation of duties, the **Maker and Checker cannot be the same person**.

### 4.2. Workflow Overview

1. **Apply:** After the Maker submits an access request, the system redirects to the application details page.
2. **Approve:** The Checker receives an email notification and logs into the system to approve the request.
3. **Reject:** The Checker can also reject the request and must provide a reason.

---

## 5. Email Notifications (Email Templates)

We have designed email templates for key steps in the approval workflow.

- **Checker Email Template:** Sent to the corresponding Checker when a new request is submitted.
- **Approval Email Template:** Sent to the Maker after the request is approved.
- **Reject Email Template:** Sent to the Maker after the request is rejected.

**Note:** Due to company policy, non-production environments cannot send emails to individual inboxes. All test emails
will be sent via a designated Test Account to a Test Mailbox. For UAT purposes, we may need to request a production
account from the Email Gateway Team for testing.

---

## 6. User and Permission Management

### 6.1. Admin Page

We provide an admin page for configuring and managing all the scenarios described above.

### 6.2. Permission Segregation

- **Super User:** (e.g., H / K) Has the highest level of permissions, with visibility of all role lists and all
  user application records.
- **Standard Approver:** Can only see and approve application lists for the roles they are authorized to manage.

---

## 7. Summary and Future Plans

### 7.1. Core Advantages

Our unified, configuration-driven solution supports a variety of complex access application scenarios. This approach is
not only flexible and extensible but also **compatible with the existing entitlement system**.

### 7.2. Migration Plan

We allow existing business lines (e.g., ILM) to continue using their current role definitions. After the IRRBB project
is completed, we will provide a detailed Migration Guideline to help them adapt and migrate their existing roles and
user data to the new model, eventually unifying everything under the new entitlement management system.
