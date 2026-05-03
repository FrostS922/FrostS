# 测试计划与测试用例关联管理功能 — 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 FrostS 测试平台添加测试计划与测试用例的关联管理功能，支持批量添加/移除用例、在线执行、批量执行、筛选排序和分配执行人。

**Architecture:** 在现有 TestPlanCase 关联实体基础上扩展后端 API，前端将测试计划详情 Drawer 改造为全屏 Modal + Tab 切换，新增关联用例管理 Tab 和多个操作弹窗。

**Tech Stack:** Spring Boot + JPA (后端), React + Ant Design + TypeScript (前端)

---

### Task 1: 后端 — Repository 层扩展

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/repository/TestPlanCaseRepository.java`

**Step 1: 添加批量查询方法**

在 `TestPlanCaseRepository.java` 中新增：

```java
List<TestPlanCase> findByIdIn(List<Long> ids);

List<TestPlanCase> findByTestPlanIdAndTestCaseIdIn(Long testPlanId, List<Long> testCaseIds);

@Query("SELECT tpc.testCase.id FROM TestPlanCase tpc WHERE tpc.testPlan.id = :testPlanId AND tpc.isDeleted = false")
List<Long> findTestCaseIdsByTestPlanId(@Param("testPlanId") Long testPlanId);
```

**Step 2: 验证编译通过**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/repository/TestPlanCaseRepository.java
git commit -m "feat: add batch query methods to TestPlanCaseRepository"
```

---

### Task 2: 后端 — Service 层扩展

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/service/TestPlanService.java`

**Step 1: 添加批量添加用例方法**

```java
public List<TestPlanCase> batchAddTestCases(Long testPlanId, List<Long> testCaseIds) {
    if (testCaseIds == null || testCaseIds.isEmpty()) {
        throw new RuntimeException("用例ID列表不能为空");
    }
    if (testCaseIds.size() > 100) {
        throw new RuntimeException("单次批量操作不能超过100条");
    }
    TestPlan testPlan = testPlanRepository.findById(testPlanId)
            .orElseThrow(() -> new RuntimeException("测试计划不存在: " + testPlanId));

    List<Long> existingCaseIds = testPlanCaseRepository.findTestCaseIdsByTestPlanId(testPlanId);
    List<Long> newCaseIds = testCaseIds.stream()
            .filter(id -> !existingCaseIds.contains(id))
            .toList();

    List<TestPlanCase> saved = new ArrayList<>();
    for (Long caseId : newCaseIds) {
        TestPlanCase tpc = new TestPlanCase();
        tpc.setTestPlan(testPlan);
        TestCase testCase = testCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("测试用例不存在: " + caseId));
        tpc.setTestCase(testCase);
        tpc.setStatus("NOT_RUN");
        saved.add(testPlanCaseRepository.save(tpc));
    }
    updatePlanStatistics(testPlanId);
    return saved;
}
```

注意：需要注入 `TestCaseRepository`。

**Step 2: 添加批量移除用例方法**

```java
public void batchRemoveTestCases(Long testPlanId, List<Long> planCaseIds) {
    if (planCaseIds == null || planCaseIds.isEmpty()) {
        throw new RuntimeException("请选择要移除的用例");
    }
    if (planCaseIds.size() > 100) {
        throw new RuntimeException("单次批量操作不能超过100条");
    }
    List<TestPlanCase> cases = testPlanCaseRepository.findByIdIn(planCaseIds);
    for (TestPlanCase tpc : cases) {
        tpc.setIsDeleted(true);
    }
    testPlanCaseRepository.saveAll(cases);
    updatePlanStatistics(testPlanId);
}
```

**Step 3: 添加批量执行用例方法**

```java
public List<TestPlanCase> batchExecuteTestCases(List<Long> planCaseIds, String status, String executedBy) {
    if (planCaseIds == null || planCaseIds.isEmpty()) {
        throw new RuntimeException("请选择要执行的用例");
    }
    if (planCaseIds.size() > 100) {
        throw new RuntimeException("单次批量操作不能超过100条");
    }
    List<TestPlanCase> cases = testPlanCaseRepository.findByIdIn(planCaseIds);
    Set<Long> planIds = new HashSet<>();
    for (TestPlanCase tpc : cases) {
        tpc.setStatus(status);
        tpc.setExecutedBy(executedBy);
        tpc.setExecutedAt(LocalDateTime.now());
        if ("BLOCKED".equals(status)) {
            tpc.setIsBlocked(true);
        } else {
            tpc.setIsBlocked(false);
            tpc.setBlockReason(null);
        }
        planIds.add(tpc.getTestPlan().getId());
    }
    List<TestPlanCase> saved = testPlanCaseRepository.saveAll(cases);
    for (Long planId : planIds) {
        updatePlanStatistics(planId);
    }
    return saved;
}
```

**Step 4: 添加分配执行人方法**

```java
public TestPlanCase assignTestCase(Long planCaseId, String assignedTo) {
    TestPlanCase tpc = testPlanCaseRepository.findById(planCaseId)
            .orElseThrow(() -> new RuntimeException("测试计划用例不存在: " + planCaseId));
    tpc.setAssignedTo(assignedTo);
    return testPlanCaseRepository.save(tpc);
}

public List<TestPlanCase> batchAssignTestCases(Long testPlanId, List<Long> planCaseIds, String assignedTo) {
    if (planCaseIds == null || planCaseIds.isEmpty()) {
        throw new RuntimeException("请选择要分配的用例");
    }
    if (planCaseIds.size() > 100) {
        throw new RuntimeException("单次批量操作不能超过100条");
    }
    List<TestPlanCase> cases = testPlanCaseRepository.findByIdIn(planCaseIds);
    for (TestPlanCase tpc : cases) {
        tpc.setAssignedTo(assignedTo);
    }
    return testPlanCaseRepository.saveAll(cases);
}
```

**Step 5: 添加必要的 import 和依赖注入**

在 TestPlanService 中注入 `TestCaseRepository`：

```java
private final TestCaseRepository testCaseRepository;
```

添加 import：
```java
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import com.frosts.testplatform.repository.TestCaseRepository;
```

**Step 6: 验证编译通过**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 7: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/service/TestPlanService.java
git commit -m "feat: add batch association methods to TestPlanService"
```

---

### Task 3: 后端 — Controller 层扩展

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/controller/TestPlanController.java`

**Step 1: 添加批量添加用例端点**

```java
@PostMapping("/{planId}/cases/batch")
public ResponseEntity<ApiResponse<List<TestPlanCase>>> batchAddTestCases(
        @PathVariable Long planId,
        @RequestBody java.util.Map<String, List<Long>> body) {
    List<Long> testCaseIds = body.get("testCaseIds");
    return ResponseEntity.ok(ApiResponse.success(testPlanService.batchAddTestCases(planId, testCaseIds)));
}
```

**Step 2: 添加批量移除用例端点**

```java
@DeleteMapping("/{planId}/cases/batch")
public ResponseEntity<ApiResponse<Void>> batchRemoveTestCases(
        @PathVariable Long planId,
        @RequestBody java.util.Map<String, List<Long>> body) {
    List<Long> planCaseIds = body.get("planCaseIds");
    testPlanService.batchRemoveTestCases(planId, planCaseIds);
    return ResponseEntity.ok(ApiResponse.success(null));
}
```

**Step 3: 添加批量执行用例端点**

```java
@PostMapping("/cases/batch-execute")
public ResponseEntity<ApiResponse<List<TestPlanCase>>> batchExecuteTestCases(
        @RequestBody java.util.Map<String, Object> body) {
    @SuppressWarnings("unchecked")
    List<Long> planCaseIds = (List<Long>) body.get("planCaseIds");
    String status = (String) body.get("status");
    String executedBy = (String) body.get("executedBy");
    return ResponseEntity.ok(ApiResponse.success(testPlanService.batchExecuteTestCases(planCaseIds, status, executedBy)));
}
```

**Step 4: 添加分配执行人端点**

```java
@PutMapping("/cases/{caseId}/assign")
public ResponseEntity<ApiResponse<TestPlanCase>> assignTestCase(
        @PathVariable Long caseId,
        @RequestBody java.util.Map<String, String> body) {
    return ResponseEntity.ok(ApiResponse.success(testPlanService.assignTestCase(caseId, body.get("assignedTo"))));
}

@PostMapping("/{planId}/cases/batch-assign")
public ResponseEntity<ApiResponse<List<TestPlanCase>>> batchAssignTestCases(
        @PathVariable Long planId,
        @RequestBody java.util.Map<String, Object> body) {
    @SuppressWarnings("unchecked")
    List<Long> planCaseIds = (List<Long>) body.get("planCaseIds");
    String assignedTo = (String) body.get("assignedTo");
    return ResponseEntity.ok(ApiResponse.success(testPlanService.batchAssignTestCases(planId, planCaseIds, assignedTo)));
}
```

**Step 5: 验证编译通过**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 6: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/controller/TestPlanController.java
git commit -m "feat: add batch association API endpoints to TestPlanController"
```

---

### Task 4: 前端 — API 层扩展

**Files:**
- Modify: `frontend/src/api/testPlan.ts`

**Step 1: 添加新的 API 调用函数**

```typescript
export const batchAddTestCases = (projectId: number, planId: number, data: { testCaseIds: number[] }) => {
  return request.post(`/projects/${projectId}/testplans/${planId}/cases/batch`, data)
}

export const batchRemoveTestCases = (projectId: number, planId: number, data: { planCaseIds: number[] }) => {
  return request.delete(`/projects/${projectId}/testplans/${planId}/cases/batch`, { data })
}

export const batchExecuteTestCases = (projectId: number, data: { planCaseIds: number[]; status: string; executedBy: string }) => {
  return request.post(`/projects/${projectId}/testplans/cases/batch-execute`, data)
}

export const assignTestCase = (projectId: number, caseId: number, data: { assignedTo: string }) => {
  return request.put(`/projects/${projectId}/testplans/cases/${caseId}/assign`, data)
}

export const batchAssignTestCases = (projectId: number, planId: number, data: { planCaseIds: number[]; assignedTo: string }) => {
  return request.post(`/projects/${projectId}/testplans/${planId}/cases/batch-assign`, data)
}
```

**Step 2: 验证 TypeScript 编译**

Run: `cd frontend && npx tsc --noEmit --pretty 2>&1 | head -20`
Expected: 无类型错误（或仅有与 testPlan.ts 无关的已有错误）

**Step 3: Commit**

```bash
git add frontend/src/api/testPlan.ts
git commit -m "feat: add batch association API functions to testPlan.ts"
```

---

### Task 5: 前端 — 改造 TestPlanList 页面（全屏 Modal + 基本信息 Tab）

**Files:**
- Modify: `frontend/src/pages/TestPlanList.tsx`

**Step 1: 将现有 Drawer 改造为全屏 Modal + Tabs**

- 移除 `drawerVisible` / `viewingPlan` 相关的 Drawer 代码
- 新增 `detailModalVisible` 状态
- 创建全屏 Modal，内部使用 `Tabs` 组件，包含"基本信息"和"关联用例"两个 Tab
- 将现有 Drawer 中的详情内容迁移到"基本信息" Tab
- 修改 `openDetail` 函数打开全屏 Modal

关键代码结构：

```tsx
import { Tabs } from 'antd'

const [detailModalVisible, setDetailModalVisible] = useState(false)
const [viewingPlan, setViewingPlan] = useState<any>(null)
const [activeTab, setActiveTab] = useState('basic')

const openDetail = (record: any) => {
  setViewingPlan(record)
  setActiveTab('basic')
  setDetailModalVisible(true)
}

// Modal 替代 Drawer
<Modal
  title={...}
  open={detailModalVisible}
  onCancel={() => setDetailModalVisible(false)}
  width="100vw"
  style={{ top: 0, paddingBottom: 0, maxWidth: '100vw' }}
  bodyStyle={{ height: 'calc(100vh - 110px)', overflow: 'auto' }}
  footer={null}
  destroyOnClose
>
  <Tabs activeKey={activeTab} onChange={setActiveTab}>
    <Tabs.TabPane tab="基本信息" key="basic">
      {/* 迁移自 Drawer 的详情内容 */}
    </Tabs.TabPane>
    <Tabs.TabPane tab="关联用例" key="cases">
      {/* Task 6 中实现 */}
    </Tabs.TabPane>
  </Tabs>
</Modal>
```

**Step 2: 验证页面渲染正常**

Run: `cd frontend && pnpm dev`
打开浏览器访问测试计划页面，点击"查看详情"确认全屏 Modal 和基本信息 Tab 正常显示。

**Step 3: Commit**

```bash
git add frontend/src/pages/TestPlanList.tsx
git commit -m "refactor: replace TestPlan detail Drawer with fullscreen Modal + Tabs"
```

---

### Task 6: 前端 — 关联用例 Tab（列表 + 筛选 + 统计）

**Files:**
- Modify: `frontend/src/pages/TestPlanList.tsx`

**Step 1: 添加关联用例列表的状态和逻辑**

```tsx
const [planCases, setPlanCases] = useState<any[]>([])
const [planCasesLoading, setPlanCasesLoading] = useState(false)
const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([])
const [filterModule, setFilterModule] = useState<string | undefined>()
const [filterPriority, setFilterPriority] = useState<string | undefined>()
const [filterStatus, setFilterStatus] = useState<string | undefined>()
const [searchText, setSearchText] = useState('')
```

**Step 2: 添加获取关联用例的函数**

```tsx
const fetchPlanCases = useCallback(async () => {
  if (!viewingPlan) return
  setPlanCasesLoading(true)
  try {
    const response: any = await getTestPlanCases(Number(projectId), viewingPlan.id)
    if (response.code === 200) {
      setPlanCases(response.data || [])
    }
  } catch {
    message.error('获取关联用例失败')
  } finally {
    setPlanCasesLoading(false)
  }
}, [viewingPlan, projectId, message])
```

**Step 3: 添加筛选后的数据**

```tsx
const filteredPlanCases = useMemo(() => {
  return planCases.filter((pc) => {
    if (filterModule && pc.testCase?.module?.name !== filterModule) return false
    if (filterPriority && pc.priority !== filterPriority) return false
    if (filterStatus && pc.status !== filterStatus) return false
    if (searchText) {
      const tc = pc.testCase
      const text = searchText.toLowerCase()
      if (!tc?.title?.toLowerCase().includes(text) && !tc?.caseNumber?.toLowerCase().includes(text)) return false
    }
    return true
  })
}, [planCases, filterModule, filterPriority, filterStatus, searchText])
```

**Step 4: 添加关联用例 Tab 的 UI**

在 Tabs.TabPane key="cases" 中添加：
- 工具栏（添加用例 / 批量执行 / 分配执行人 / 批量移除按钮）
- 筛选栏（模块 / 优先级 / 状态下拉 + 搜索框）
- 用例表格（带行选择 checkbox）
- 底部统计栏

**Step 5: 验证关联用例列表渲染**

Run: `cd frontend && pnpm dev`
打开测试计划详情，切换到"关联用例" Tab，确认列表和筛选正常。

**Step 6: Commit**

```bash
git add frontend/src/pages/TestPlanList.tsx
git commit -m "feat: add associated test cases list with filtering in plan detail"
```

---

### Task 7: 前端 — 添加用例弹窗（批量勾选）

**Files:**
- Modify: `frontend/src/pages/TestPlanList.tsx`

**Step 1: 添加弹窗状态**

```tsx
const [addCasesModalVisible, setAddCasesModalVisible] = useState(false)
const [allTestCases, setAllTestCases] = useState<any[]>([])
const [selectedCaseIds, setSelectedCaseIds] = useState<number[]>([])
const [addCasesLoading, setAddCasesLoading] = useState(false)
```

**Step 2: 添加打开弹窗和加载数据的逻辑**

```tsx
const openAddCasesModal = async () => {
  setAddCasesModalVisible(true)
  setAddCasesLoading(true)
  try {
    const response: any = await getTestCases(Number(projectId), { page: 0, size: 500 })
    if (response.code === 200) {
      setAllTestCases(response.data || [])
    }
  } catch {
    message.error('获取用例列表失败')
  } finally {
    setAddCasesLoading(false)
  }
  setSelectedCaseIds([])
}
```

**Step 3: 添加确认添加的处理函数**

```tsx
const handleAddCases = async () => {
  if (selectedCaseIds.length === 0) {
    message.warning('请选择要添加的用例')
    return
  }
  try {
    await batchAddTestCases(Number(projectId), viewingPlan.id, { testCaseIds: selectedCaseIds })
    message.success(`成功添加 ${selectedCaseIds.length} 个用例`)
    setAddCasesModalVisible(false)
    fetchPlanCases()
    fetchData()
  } catch {
    message.error('添加用例失败')
  }
}
```

**Step 4: 添加弹窗 UI**

使用 Modal + Table（带 checkbox），已关联的用例禁用勾选。

**Step 5: 验证添加用例功能**

Run: `cd frontend && pnpm dev`
打开测试计划详情 → 关联用例 Tab → 点击"添加用例" → 勾选用例 → 确认添加。

**Step 6: Commit**

```bash
git add frontend/src/pages/TestPlanList.tsx
git commit -m "feat: add batch add test cases modal to plan detail"
```

---

### Task 8: 前端 — 执行用例弹窗（单个 + 批量）

**Files:**
- Modify: `frontend/src/pages/TestPlanList.tsx`

**Step 1: 添加执行弹窗状态**

```tsx
const [executeModalVisible, setExecuteModalVisible] = useState(false)
const [executingCase, setExecutingCase] = useState<any>(null)
const [isBatchExecute, setIsBatchExecute] = useState(false)
const [executeForm] = Form.useForm()
```

**Step 2: 添加单个执行和批量执行的逻辑**

```tsx
const openExecuteModal = (planCase: any) => {
  setExecutingCase(planCase)
  setIsBatchExecute(false)
  executeForm.resetFields()
  executeForm.setFieldsValue({ status: 'PASSED' })
  setExecuteModalVisible(true)
}

const openBatchExecuteModal = () => {
  if (selectedRowKeys.length === 0) {
    message.warning('请选择要执行的用例')
    return
  }
  setExecutingCase(null)
  setIsBatchExecute(true)
  executeForm.resetFields()
  executeForm.setFieldsValue({ status: 'PASSED' })
  setExecuteModalVisible(true)
}

const handleExecute = async () => {
  const values = await executeForm.validateFields()
  try {
    if (isBatchExecute) {
      await batchExecuteTestCases(Number(projectId), {
        planCaseIds: selectedRowKeys,
        status: values.status,
        executedBy: values.executedBy,
      })
      message.success(`批量执行 ${selectedRowKeys.length} 个用例成功`)
    } else {
      await executeTestCase(Number(projectId), executingCase.id, values)
      message.success('执行成功')
    }
    setExecuteModalVisible(false)
    fetchPlanCases()
    fetchData()
    setSelectedRowKeys([])
  } catch {
    message.error('执行失败')
  }
}
```

**Step 3: 添加执行弹窗 UI**

Modal + Form，包含：状态选择、实际结果、执行人、缺陷ID、缺陷链接、证据、阻塞原因。

**Step 4: 验证执行功能**

Run: `cd frontend && pnpm dev`
单个执行：点击行内"执行"按钮 → 填写执行信息 → 确认。
批量执行：勾选多个用例 → 点击"批量执行" → 填写信息 → 确认。

**Step 5: Commit**

```bash
git add frontend/src/pages/TestPlanList.tsx
git commit -m "feat: add single and batch test case execution modal"
```

---

### Task 9: 前端 — 分配执行人弹窗 + 批量移除

**Files:**
- Modify: `frontend/src/pages/TestPlanList.tsx`

**Step 1: 添加分配执行人弹窗状态和逻辑**

```tsx
const [assignModalVisible, setAssignModalVisible] = useState(false)
const [assignForm] = Form.useForm()

const openBatchAssignModal = () => {
  if (selectedRowKeys.length === 0) {
    message.warning('请选择要分配的用例')
    return
  }
  assignForm.resetFields()
  setAssignModalVisible(true)
}

const handleBatchAssign = async () => {
  const values = await assignForm.validateFields()
  try {
    await batchAssignTestCases(Number(projectId), viewingPlan.id, {
      planCaseIds: selectedRowKeys,
      assignedTo: values.assignedTo,
    })
    message.success(`成功分配 ${selectedRowKeys.length} 个用例`)
    setAssignModalVisible(false)
    fetchPlanCases()
    setSelectedRowKeys([])
  } catch {
    message.error('分配失败')
  }
}
```

**Step 2: 添加批量移除逻辑**

```tsx
const handleBatchRemove = async () => {
  if (selectedRowKeys.length === 0) {
    message.warning('请选择要移除的用例')
    return
  }
  Modal.confirm({
    title: '确认移除',
    content: `确定要从计划中移除 ${selectedRowKeys.length} 个用例？`,
    onOk: async () => {
      try {
        await batchRemoveTestCases(Number(projectId), viewingPlan.id, {
          planCaseIds: selectedRowKeys,
        })
        message.success('移除成功')
        fetchPlanCases()
        fetchData()
        setSelectedRowKeys([])
      } catch {
        message.error('移除失败')
      }
    },
  })
}
```

**Step 3: 添加分配弹窗 UI**

Modal + Form，包含执行人输入框。

**Step 4: 验证分配和移除功能**

Run: `cd frontend && pnpm dev`
分配：勾选用例 → 点击"分配执行人" → 输入执行人 → 确认。
移除：勾选用例 → 点击"批量移除" → 确认。

**Step 5: Commit**

```bash
git add frontend/src/pages/TestPlanList.tsx
git commit -m "feat: add batch assign and remove test cases from plan"
```

---

### Task 10: 前端 — 样式优化与最终验证

**Files:**
- Modify: `frontend/src/index.css`（如需要）

**Step 1: 添加全屏 Modal 相关样式**

为全屏 Modal 内的关联用例 Tab 添加必要的样式，确保布局美观、表格可滚动、底部统计栏固定。

**Step 2: 端到端功能验证**

验证完整流程：
1. 打开测试计划列表 → 点击"查看详情" → 全屏 Modal 打开
2. 基本信息 Tab 显示计划详情
3. 切换到关联用例 Tab → 显示已关联用例列表
4. 点击"添加用例" → 勾选用例 → 确认添加 → 列表刷新
5. 勾选用例 → 点击"批量执行" → 填写执行信息 → 确认 → 统计更新
6. 勾选用例 → 点击"分配执行人" → 输入执行人 → 确认
7. 勾选用例 → 点击"批量移除" → 确认 → 列表刷新
8. 单个用例点击"执行" → 填写执行信息 → 确认
9. 筛选和搜索功能正常

**Step 3: Commit**

```bash
git add frontend/src/index.css frontend/src/pages/TestPlanList.tsx
git commit -m "style: polish fullscreen modal and associated cases tab styling"
```
