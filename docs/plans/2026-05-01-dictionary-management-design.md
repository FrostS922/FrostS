# 数据字典管理模块设计方案

## 目标

为测试管理平台添加完整的数据字典管理功能模块，支持多级树形分类、枚举值CRUD、按分类的权限控制、Excel导入导出、Redis缓存及操作日志记录。

## 架构

采用前后端分离架构，后端基于Spring Boot 3.2 + JPA + PostgreSQL + Redis，前端基于React + Ant Design + TypeScript。新增字典分类、字典项、分类权限、操作日志四张表，通过Redis缓存提升性能，通过分类权限表实现细粒度权限控制。

## 技术栈

- **后端**: Spring Boot 3.2, Spring Data JPA, Spring Security, Redis, Apache POI, PostgreSQL
- **前端**: React 18, Ant Design 5, ProComponents, TypeScript
- **工具**: MapStruct, Lombok, Flyway (数据库迁移)

## 数据库设计

### 1. 字典分类表 (sys_dictionary_type)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| parent_id | BIGINT | FK -> sys_dictionary_type(id) | 父分类ID，支持树形结构 |
| code | VARCHAR(50) | NOT NULL, UNIQUE | 分类编码，如 DEFECT_STATUS |
| name | VARCHAR(50) | NOT NULL | 分类名称 |
| description | TEXT | | 描述 |
| sort_order | INT | DEFAULT 0 | 排序 |
| enabled | BOOLEAN | DEFAULT TRUE | 启用状态 |
| is_system | BOOLEAN | DEFAULT FALSE | 是否系统内置 |
| created_at | TIMESTAMP | | 创建时间 |
| updated_at | TIMESTAMP | | 更新时间 |
| created_by | VARCHAR(50) | | 创建人 |
| updated_by | VARCHAR(50) | | 更新人 |
| is_deleted | BOOLEAN | DEFAULT FALSE | 逻辑删除 |

### 2. 字典枚举值表 (sys_dictionary_item)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| type_id | BIGINT | NOT NULL, FK | 所属分类ID |
| code | VARCHAR(50) | NOT NULL | 枚举编码 |
| name | VARCHAR(50) | NOT NULL | 枚举名称 |
| value | VARCHAR(100) | | 实际值 |
| description | TEXT | | 描述 |
| sort_order | INT | DEFAULT 0 | 排序 |
| enabled | BOOLEAN | DEFAULT TRUE | 启用状态 |
| is_default | BOOLEAN | DEFAULT FALSE | 是否默认值 |
| color | VARCHAR(20) | | 前端显示颜色 |
| created_at | TIMESTAMP | | 创建时间 |
| updated_at | TIMESTAMP | | 更新时间 |
| created_by | VARCHAR(50) | | 创建人 |
| updated_by | VARCHAR(50) | | 更新人 |
| is_deleted | BOOLEAN | DEFAULT FALSE | 逻辑删除 |

**唯一约束**: (type_id, code) - 同一分类下枚举编码唯一

### 3. 字典分类权限表 (sys_dictionary_type_role)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| type_id | BIGINT | NOT NULL, FK | 字典分类ID |
| role_id | BIGINT | NOT NULL, FK | 角色ID |
| permission | VARCHAR(20) | NOT NULL | 权限类型: READ/WRITE/ADMIN |
| created_at | TIMESTAMP | | 创建时间 |

**唯一约束**: (type_id, role_id) - 同一角色对同一分类只能有一条权限记录

### 4. 操作日志表 (sys_dictionary_log)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| type_id | BIGINT | | 字典分类ID |
| item_id | BIGINT | | 字典项ID |
| action | VARCHAR(20) | NOT NULL | 操作类型 |
| old_value | JSONB | | 变更前的值 |
| new_value | JSONB | | 变更后的值 |
| operator | VARCHAR(50) | NOT NULL | 操作人 |
| operated_at | TIMESTAMP | NOT NULL | 操作时间 |
| ip_address | VARCHAR(50) | | IP地址 |

## API设计

### 字典分类API

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /dictionary/types | 获取字典分类树 | 任意登录用户 |
| GET | /dictionary/types/{id} | 获取单个分类详情 | 任意登录用户 |
| POST | /dictionary/types | 创建分类 | 该分类的ADMIN权限 |
| PUT | /dictionary/types/{id} | 更新分类 | 该分类的WRITE/ADMIN权限 |
| DELETE | /dictionary/types/{id} | 删除分类 | 该分类的ADMIN权限 |
| GET | /dictionary/types/{id}/permissions | 获取分类权限 | ADMIN角色 |
| PUT | /dictionary/types/{id}/permissions | 设置分类权限 | ADMIN角色 |

### 字典项API

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /dictionary/types/{typeId}/items | 获取分类下的枚举值 | 该分类的READ权限 |
| GET | /dictionary/items/{id} | 获取单个枚举值 | 该分类的READ权限 |
| POST | /dictionary/items | 创建枚举值 | 该分类的WRITE/ADMIN权限 |
| PUT | /dictionary/items/{id} | 更新枚举值 | 该分类的WRITE/ADMIN权限 |
| DELETE | /dictionary/items/{id} | 删除枚举值 | 该分类的WRITE/ADMIN权限 |
| PATCH | /dictionary/items/{id}/status | 启用/禁用枚举值 | 该分类的WRITE/ADMIN权限 |

### 导入导出API

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /dictionary/import | Excel导入 | ADMIN角色 |
| GET | /dictionary/export | Excel导出 | 任意登录用户 |

### 操作日志API

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /dictionary/logs | 查询操作日志 | ADMIN角色 |

## 缓存设计

### Redis Key规范

| Key | 类型 | 说明 | TTL |
|-----|------|------|-----|
| `dictionary:type:{code}` | String | 单个分类信息 | 30分钟 |
| `dictionary:items:{typeCode}` | String (JSON) | 分类下的所有枚举值 | 30分钟 |
| `dictionary:tree` | String (JSON) | 完整的分类树 | 30分钟 |
| `dictionary:user:{userId}:types` | String (JSON) | 用户有权限的分类列表 | 15分钟 |

### 缓存策略

1. **读取时**: 先查缓存，缓存未命中则查数据库并写入缓存
2. **写入时**: 清除相关缓存（分类信息、枚举值列表、分类树）
3. **权限变更时**: 清除用户权限缓存

## Excel格式

### Sheet1 - 字典分类 (DictionaryTypes)

| 分类编码 | 分类名称 | 父分类编码 | 描述 | 排序 | 启用状态 |
|---------|---------|-----------|------|------|---------|
| DEFECT_STATUS | 缺陷状态 | SYSTEM | 缺陷状态字典 | 1 | 启用 |
| PRIORITY | 优先级 | SYSTEM | 优先级字典 | 2 | 启用 |

### Sheet2+ - 枚举值 (各分类独立Sheet)

Sheet名称格式: `Items_{分类编码}`

| 枚举编码 | 枚举名称 | 实际值 | 描述 | 排序 | 启用状态 | 是否默认 | 颜色 |
|---------|---------|--------|------|------|---------|---------|------|
| NEW | 新建 | NEW | 新建状态 | 1 | 启用 | 是 | #1890ff |
| IN_PROGRESS | 处理中 | IN_PROGRESS | 处理中状态 | 2 | 启用 | 否 | #faad14 |
| RESOLVED | 已解决 | RESOLVED | 已解决状态 | 3 | 启用 | 否 | #52c41a |
| CLOSED | 已关闭 | CLOSED | 已关闭状态 | 4 | 启用 | 否 | #8c8c8c |

## 权限控制规则

### 权限继承

1. 子分类继承父分类的权限配置
2. 如果子分类有自己的权限配置，则使用自己的配置
3. ADMIN角色拥有所有字典的完全权限

### 权限类型

| 权限类型 | 说明 |
|---------|------|
| READ | 可查看该分类及其枚举值 |
| WRITE | 可编辑枚举值（增删改、启用/禁用） |
| ADMIN | 可管理分类本身（修改分类信息、设置权限） |

### 权限校验流程

1. 获取当前用户的所有角色
2. 查询目标分类及其父分类的权限配置
3. 从最近的一级有配置的分类开始匹配
4. 检查用户角色是否有足够的权限

## 前端设计

### 页面布局

```
+--------------------------------------------------+
| 数据字典管理                                        |
+--------------------------------------------------+
| 导入 | 导出 | 新建分类 | 刷新                        |
+--------------------------------------------------+
| 左侧分类树                | 右侧枚举值列表              |
|                         |                             |
| [系统配置]               | + 新建枚举值                 |
|   [业务配置]             |                             |
|     [缺陷状态] ◄──选中   | | 编码 | 名称 | 状态 | 操作 | |
|     [优先级]             | | NEW  | 新建 | 启用 | 编辑 | |
|     [用例类型]           | | ...  | ...  | ...  | ... | |
|   [用户配置]             |                             |
+--------------------------------------------------+
```

### 组件结构

- `DictionaryManagement.tsx` - 主页面
- `DictionaryTypeTree.tsx` - 左侧分类树
- `DictionaryItemTable.tsx` - 右侧枚举值表格
- `DictionaryTypeModal.tsx` - 分类编辑弹窗
- `DictionaryItemModal.tsx` - 枚举值编辑弹窗
- `DictionaryPermissionModal.tsx` - 权限设置弹窗
- `DictionaryLogDrawer.tsx` - 操作日志抽屉

## 操作日志记录规则

| 操作 | 记录内容 |
|------|---------|
| 创建分类 | 新分类的完整信息 |
| 更新分类 | 变更前后的字段对比 |
| 删除分类 | 被删除分类的信息 |
| 创建枚举值 | 新枚举值的完整信息 |
| 更新枚举值 | 变更前后的字段对比 |
| 删除枚举值 | 被删除枚举值的信息 |
| 启用/禁用 | 状态变更记录 |
| 导入 | 导入的文件名、导入的分类和枚举值数量 |
| 导出 | 导出的文件名、导出的分类和枚举值数量 |

## 安全考虑

1. **SQL注入防护**: 使用JPA参数化查询，避免字符串拼接SQL
2. **XSS防护**: 前端转义显示内容，后端校验输入
3. **权限校验**: 每个操作都进行权限校验，防止越权访问
4. **Excel安全**: 限制导入文件大小（最大10MB），校验文件类型
5. **操作审计**: 所有写操作记录日志，便于追溯

## 性能优化

1. **数据库索引**:
   - `sys_dictionary_type`: code, parent_id, enabled
   - `sys_dictionary_item`: type_id, code, enabled
   - `sys_dictionary_type_role`: type_id, role_id
   - `sys_dictionary_log`: type_id, item_id, operated_at

2. **缓存优化**:
   - 热点数据（常用字典）缓存30分钟
   - 权限数据缓存15分钟
   - 批量操作后延迟清除缓存（避免缓存雪崩）

3. **查询优化**:
   - 分类树一次性查询，内存构建树结构
   - 枚举值列表支持分页和搜索
   - 日志查询支持时间范围过滤
