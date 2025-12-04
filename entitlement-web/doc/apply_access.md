# “Apply Access” 功能介绍

## 1. 背景与目标

在现有的基于 RBAC (Role-Based Access Control) 和用户角色属性 (User Role Attribute) 的权限体系下，我们引入了全新的 “Apply Access” 功能。

**目标:** 旨在提供一个高度灵活、可配置的权限申请流程，以适应不同业务场景下的复杂权限组合需求，同时兼容现有的权限体系，支持未来平滑迁移。

---

## 2. 核心功能概览

“Apply Access” 功能的核心是**通过统一的配置模型来驱动不同场景的权限申请**。

### 2.1. 功能点 (Function List)

我们将不同的用户角色 (Role) 按功能域 (Function) 进行归类。用户在申请权限时，首先选择一个功能域，系统会动态展示与该功能域相关的角色列表供用户选择。

**核心设计:**
- **按功能分类:** 将繁多的 Role 按业务功能进行组织，简化用户的查找和理解成本。
- **动态角色列表:** 根据用户选择的功能，精确展示相关的 Role。
- **可见性控制:** 我们实现了一个开关 (`true`/`false`)，可以控制某些内部使用（如 IT-Ready-Only）的 Role 是否对普通用户可见。

### 2.2. 核心概念

- **Role:** 权限的基本单位。
- **Entity:** 业务实体，如某个具体的业务单元或系统。
- **GB (Global Business):** 全局业务线。
- **Function:** 功能域，用于对 Role 进行逻辑上的分类。

---

## 3. 场景化配置与示例

我们通过一个统一的 `use_case_config` 表来定义和驱动所有权限申请场景。这意味着未来扩展新的申请场景时，只需修改配置即可，无需改动代码。

以下是几个典型的应用场景：

### 3.1. 场景一：复杂权限组合 (以 IRRBB 为例)

此场景适用于需要同时满足多个维度才能确定唯一权限的复杂情况。

- **权限组合:** `Role` + `Entity` + `GB` + `Function`
- **配置示例:**
  ```json
  {
    "use_case": "IRRBB",
    "hierarchy": ["Function", "GB", "Entity", "Role"],
    "roles": [
      {"name": "IRRBB_Analyst"},
      {"name": "IRRBB_Admin"}
    ]
  }
  ```

### 3.2. 场景二：实体与角色组合

- **权限组合:** `Entity` + `Role`
- **配置示例:**
  ```json
  {
    "use_case": "SimpleEntityAccess",
    "hierarchy": ["Entity", "Role"],
    ...
  }
  ```

### 3.3. 场景三：角色与自定义列表组合

- **权限组合:** `Role` + `自定义列表` (例如，A, B, C)
- **配置示例:**
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

### 3.4. 场景四：单一角色申请

最简单的场景，用户直接申请某个角色。

- **权限组合:** `Role`
- **配置示例:**
  ```json
  {
    "use_case": "SingleRoleAccess",
    "hierarchy": ["Role"],
    ...
  }
  ```

---

## 4. 审批流程 (Approval Workflow)

### 4.1. Maker / Checker 机制

- **Maker:** 权限的申请者。
- **Checker:** 权限的审批者。
- **核心规则:** 为确保职责分离，**Maker 和 Checker 不能是同一个人**。

### 4.2. 流程概览

1.  **申请 (Apply):** Maker 提交权限申请后，系统会跳转到申请详情页。
2.  **审批 (Approve):** Checker 收到邮件通知，进入系统进行审批。
3.  **拒绝 (Reject):** Checker 也可以拒绝申请，并填写拒绝理由。

---

## 5. 邮件通知 (Email Templates)

我们为审批流程中的关键节点设计了邮件模板。

- **Checker Email Template:** 当有新的申请提交时，发送给对应的 Checker。
- **Approval Email Template:** 当申请被批准后，发送给 Maker。
- **Reject Email Template:** 当申请被拒绝后，发送给 Maker。

**注意:** 根据公司政策，非生产环境无法向个人邮箱发送邮件。所有测试邮件将通过指定的 Test Account 发送到 Test Mailbox。为了方便 UAT，我们可能需要向 Email Gateway Team 申请一个用于测试的生产环境账号。

---

## 6. 用户与权限管理

### 6.1. Admin Page

我们提供了管理后台，用于配置和管理上述所有场景。

### 6.2. 权限划分

- **超级用户 (Super User):** 例如 H / k，拥有最高权限，可以看到所有角色列表和所有用户的申请记录。
- **普通审批者 (Approver):** 只能看到和审批他们有权限管理的那些角色的申请列表。

---

## 7. 总结与未来规划

### 7.1. 核心优势

我们通过统一的配置化方案，实现了对多种复杂权限申请场景的支持。这套方案不仅灵活、可扩展，而且能够**兼容现有的权限体系**。

### 7.2. 迁移计划

我们允许现有业务（如 ILM）继续使用其既有的 Role 定义。在 IRRBB 项目完成后，我们将提供详细的迁移指南 (Guideline)，帮助他们按照新的模型来改造和迁移现有的 Role 和用户数据，最终逐步统一到新的权限管理体系下。
```