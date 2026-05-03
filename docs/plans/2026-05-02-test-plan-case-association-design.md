# 测试计划与测试用例关联管理功能设计

## 1. 概述

为 FrostS 测试平台添加测试计划与测试用例的关联管理功能，支持在测试计划详情页中批量添加/移除用例、在线执行用例、批量执行、筛选排序以及分配执行人。

## 2. 方案选型

**选定方案：全屏 Modal + Tab 切换**

在测试计划列表页点击"查看详情"时，打开全屏 Modal，内含两个 Tab：
- 基本信息 Tab：展示计划详情（迁移自现有 Drawer）
- 关联用例 Tab：用例列表 + 批量添加/移除/执行/分配

选择理由：不增加新路由，改动范围小，全屏 Modal 空间充足，用户在列表上下文中操作不丢失位置。

## 3. 交互流程

```
测试计划列表 → 点击"查看详情" → 打开全屏 Modal
                                ├── Tab 1: 基本信息
                                └── Tab 2: 关联用例
                                      ├── 用例列表（带筛选/排序/搜索）
                                      ├── 添加用例（批量勾选弹窗）
                                      ├── 移除用例（批量选择后移除）
                                      ├── 执行用例（单个/批量执行弹窗）
                                      └── 分配执行人（批量分配弹窗）
```

## 4. 关联用例 Tab 布局

```
┌─────────────────────────────────────────────────┐
│ [添加用例] [批量执行] [分配执行人] [批量移除]   筛选: [模块▼] [优先级▼] [状态▼] 搜索: [____] │
├─────────────────────────────────────────────────┤
│ ☐ │ 编号 │ 标题 │ 模块 │ 优先级 │ 状态 │ 执行人 │ 操作 │
│ ☐ │ TC-001 │ 登录验证 │ 认证 │ 高 │ 通过 │ 张三 │ [执行] │
│ ☐ │ TC-002 │ 权限检查 │ 权限 │ 中 │ 未执行 │ — │ [执行] │
│ ...                                                              │
├─────────────────────────────────────────────────┤
│ 底部统计栏: 总计 25 | 通过 15 | 失败 3 | 阻塞 2 | 未执行 5     │
└─────────────────────────────────────────────────┘
```

## 5. 后端 API 设计

### 5.1 现有 API（保留不变）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/projects/{projectId}/testplans` | 获取测试计划列表 |
| GET | `/projects/{projectId}/testplans/{id}` | 获取测试计划详情 |
| POST | `/projects/{projectId}/testplans` | 创建测试计划 |
| PUT | `/projects/{projectId}/testplans/{id}` | 更新测试计划 |
| DELETE | `/projects/{projectId}/testplans/{id}` | 删除测试计划 |
| GET | `/projects/{projectId}/testplans/{planId}/cases` | 获取计划关联用例 |
| POST | `/projects/{projectId}/testplans/{planId}/cases` | 添加单个用例 |
| POST | `/projects/{projectId}/testplans/cases/{caseId}/execute` | 执行用例 |

### 5.2 新增 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/projects/{projectId}/testplans/{planId}/cases/batch` | 批量添加用例到计划 |
| DELETE | `/projects/{projectId}/testplans/{planId}/cases/batch` | 批量移除计划中的用例 |
| POST | `/projects/{projectId}/testplans/cases/batch-execute` | 批量执行用例 |
| PUT | `/projects/{projectId}/testplans/cases/{caseId}/assign` | 分配执行人 |
| POST | `/projects/{projectId}/testplans/{planId}/cases/batch-assign` | 批量分配执行人 |

### 5.3 API 详细设计

**批量添加用例**
```
POST /projects/{projectId}/testplans/{planId}/cases/batch
Request Body: { "testCaseIds": [1, 2, 3] }
Response: { "code": 200, "data": [TestPlanCase, ...] }
```

**批量移除用例**
```
DELETE /projects/{projectId}/testplans/{planId}/cases/batch
Request Body: { "planCaseIds": [10, 11, 12] }
Response: { "code": 200, "message": "成功移除 3 个用例" }
```

**批量执行用例**
```
POST /projects/{projectId}/testplans/cases/batch-execute
Request Body: {
  "planCaseIds": [10, 11],
  "status": "PASSED",
  "executedBy": "张三"
}
Response: { "code": 200, "data": [TestPlanCase, ...] }
```

**分配执行人**
```
PUT /projects/{projectId}/testplans/cases/{caseId}/assign
Request Body: { "assignedTo": "李四" }
Response: { "code": 200, "data": TestPlanCase }
```

**批量分配执行人**
```
POST /projects/{projectId}/testplans/{planId}/cases/batch-assign
Request Body: { "planCaseIds": [10, 11, 12], "assignedTo": "李四" }
Response: { "code": 200, "data": [TestPlanCase, ...] }
```

### 5.4 安全考虑

- 所有 API 需经过 JWT 认证
- 批量操作限制单次最多 100 条，防止滥用
- 移除用例使用逻辑删除（设置 `isDeleted = true`），不物理删除

## 6. 前端组件设计

### 6.1 组件结构

```
TestPlanList.tsx（修改）
├── 测试计划列表（现有，不变）
├── 创建/编辑 Modal（现有，不变）
└── TestPlanDetailModal（新增，替代现有 Drawer）
    ├── Tab: 基本信息
    │   └── 计划详情 Descriptions（迁移自 Drawer）
    └── Tab: 关联用例
        ├── 工具栏：添加用例 / 批量执行 / 分配执行人 / 批量移除
        ├── 筛选栏：模块 / 优先级 / 状态 / 搜索
        ├── 用例表格（带行选择）
        └── 底部统计栏
├── AddCasesModal（新增）
│   └── 项目用例列表 + 勾选 + 确认添加
├── ExecuteCaseModal（新增）
│   └── 执行表单：状态 / 实际结果 / 缺陷链接 / 证据 / 阻塞原因
└── BatchAssignModal（新增）
    └── 选择执行人 + 确认分配
```

### 6.2 前端 API 新增（testPlan.ts）

```typescript
batchAddTestCases(projectId, planId, { testCaseIds })
batchRemoveTestCases(projectId, planId, { planCaseIds })
batchExecuteTestCases(projectId, { planCaseIds, status, executedBy })
assignTestCase(projectId, caseId, { assignedTo })
batchAssignTestCases(projectId, planId, { planCaseIds, assignedTo })
```

### 6.3 关键交互细节

- 添加用例弹窗：加载项目下所有用例，已关联的用例标记为已添加（禁用勾选），支持搜索和筛选
- 批量操作：表格支持行选择（checkbox），选中后工具栏按钮变为可用
- 执行用例：单个执行点击行内"执行"按钮，批量执行先勾选再点"批量执行"
- 状态自动同步：每次执行/添加/移除操作后，自动刷新关联用例列表和计划统计信息

## 7. 后端数据层与 Service 设计

### 7.1 Repository 新增方法

```java
// TestPlanCaseRepository 新增
List<TestPlanCase> findByIdIn(List<Long> ids);
List<TestPlanCase> findByTestPlanIdAndTestCaseIdIn(Long testPlanId, List<Long> testCaseIds);
```

### 7.2 Service 新增方法

```java
// TestPlanService 新增
List<TestPlanCase> batchAddTestCases(Long testPlanId, List<Long> testCaseIds);
void batchRemoveTestCases(Long testPlanId, List<Long> planCaseIds);
List<TestPlanCase> batchExecuteTestCases(List<Long> planCaseIds, String status, String executedBy);
TestPlanCase assignTestCase(Long planCaseId, String assignedTo);
List<TestPlanCase> batchAssignTestCases(Long testPlanId, List<Long> planCaseIds, String assignedTo);
```

### 7.3 关键业务逻辑

- 批量添加：先查询已存在的关联（防重复），仅添加新关联，初始状态 NOT_RUN
- 批量移除：逻辑删除（isDeleted = true），移除后自动更新计划统计
- 批量执行：遍历设置状态/执行人/执行时间，更新后刷新计划统计
- 分配执行人：更新 assignedTo 字段
- 统计更新：所有变更操作后调用 updatePlanStatistics 刷新计划的通过/失败/阻塞/未执行计数和进度百分比

### 7.4 错误处理

- 测试计划不存在 → 404
- 用例已关联 → 跳过重复，返回成功添加的数量
- 批量操作超过 100 条 → 400 Bad Request
- 计划状态为 COMPLETED/ARCHIVED 时禁止执行操作 → 400

## 8. 影响范围

### 后端修改文件
- `TestPlanController.java` — 新增 5 个 API 端点
- `TestPlanService.java` — 新增 5 个 Service 方法
- `TestPlanCaseRepository.java` — 新增 2 个查询方法

### 前端修改文件
- `TestPlanList.tsx` — 替换 Drawer 为全屏 Modal + Tab，新增关联用例管理
- `testPlan.ts` — 新增 5 个 API 调用函数

### 无需修改
- 数据库 — `test_plan_case` 表已存在，字段满足需求
- 实体类 — `TestPlanCase.java` 已包含所有必要字段
- 路由 — 不新增路由
