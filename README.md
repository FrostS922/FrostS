# 企业级测试管理平台 (FrostS Test Platform)

一个功能完整的企业级测试管理解决方案，支持完整的测试生命周期管理。

## 技术栈

### 后端
- **框架:** Java Spring Boot 3.2
- **安全:** Spring Security + JWT
- **数据库:** PostgreSQL 16
- **ORM:** JPA / Hibernate
- **缓存:** Redis 7

### 前端
- **框架:** React 18 + TypeScript
- **UI库:** Ant Design 5
- **构建工具:** Vite 5
- **状态管理:** Zustand
- **HTTP客户端:** Axios
- **路由:** React Router 6

### 部署
- **容器化:** Docker + Docker Compose
- **反向代理:** Nginx

## 核心功能模块

### 1. 用户认证与权限管理
- JWT Token认证
- RBAC角色权限控制
- 预定义角色：系统管理员、测试经理、测试工程师、开发人员
- 细粒度权限管理

### 2. 项目管理
- 项目创建、编辑、删除
- 项目成员管理
- 项目状态跟踪（规划中、进行中、已完成、已暂停）
- 项目编码自动生成

### 3. 需求管理
- 需求层次结构（父子关系）
- 需求类型：功能需求、非功能需求、接口需求
- 需求状态流转：草稿 -> 评审中 -> 已批准/已拒绝
- 优先级管理（高、中、低）
- 需求编号自动生成

### 4. 测试用例管理
- 测试用例模块化管理
- 与需求关联
- 测试类型：功能测试、集成测试、性能测试、安全测试
- 测试步骤、前置条件、预期结果详细记录
- 用例状态管理：草稿、激活、废弃
- 用例编号自动生成

### 5. 测试计划管理
- 测试计划制定
- 测试用例关联到计划
- 测试执行跟踪
- 测试范围和策略文档
- 计划状态：草稿、进行中、已完成、已阻塞

### 6. 缺陷管理
- 缺陷提交与跟踪
- 缺陷生命周期：新建 -> 打开 -> 已解决 -> 已关闭
- 严重性分级：致命、严重、一般、轻微
- 优先级管理
- 缺陷指派
- 环境信息记录
- 缺陷编号自动生成
- 缺陷统计分析

### 7. 报告分析
- 缺陷统计仪表板
- 测试执行进度
- 用例覆盖率分析
- 趋势分析

## 快速开始

### 环境要求
- Docker & Docker Compose
- Java 17+ (本地开发)
- Node.js 20+ (本地开发)
- PostgreSQL 16 (本地开发)
- Redis 7 (本地开发)

### Docker Compose 一键部署

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

启动后访问：
- 前端界面: http://localhost
- 后端API: http://localhost:8080/api

### 本地开发

#### 后端启动
```bash
cd backend
mvn spring-boot:run
```

#### 前端启动
```bash
cd frontend
npm install
npm run dev
```

### 默认账号
- 用户名: admin
- 密码: admin123

## 项目结构

```
FrostS/
├── backend/                    # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/frosts/testplatform/
│   │   │   │   ├── config/           # 配置类
│   │   │   │   ├── common/           # 公共类
│   │   │   │   ├── entity/           # 实体类
│   │   │   │   ├── repository/       # 数据访问层
│   │   │   │   ├── service/          # 业务逻辑层
│   │   │   │   ├── controller/       # 控制器层
│   │   │   │   ├── security/         # 安全相关
│   │   │   │   └── dto/              # 数据传输对象
│   │   │   └── resources/
│   │   │       └── application.yml   # 应用配置
│   │   └── test/                     # 测试代码
│   ├── pom.xml                       # Maven配置
│   └── Dockerfile                    # Docker镜像构建
├── frontend/                   # 前端项目
│   ├── src/
│   │   ├── api/                # API接口
│   │   ├── components/         # 组件
│   │   ├── pages/              # 页面
│   │   ├── store/              # 状态管理
│   │   ├── utils/              # 工具函数
│   │   ├── App.tsx             # 应用入口
│   │   └── main.tsx            # React入口
│   ├── package.json            # 依赖配置
│   ├── vite.config.ts          # Vite配置
│   ├── tsconfig.json           # TypeScript配置
│   ├── nginx.conf              # Nginx配置
│   └── Dockerfile              # Docker镜像构建
└── docker-compose.yml          # Docker编排配置
```

## API文档

### 认证接口
- `POST /api/auth/login` - 用户登录

### 项目接口
- `GET /api/projects` - 获取项目列表
- `GET /api/projects/{id}` - 获取项目详情
- `POST /api/projects` - 创建项目
- `PUT /api/projects/{id}` - 更新项目
- `DELETE /api/projects/{id}` - 删除项目
- `POST /api/projects/{id}/members/{username}` - 添加成员
- `DELETE /api/projects/{id}/members/{username}` - 移除成员

### 需求接口
- `GET /api/projects/{projectId}/requirements` - 获取需求列表
- `GET /api/projects/{projectId}/requirements/{id}` - 获取需求详情
- `POST /api/projects/{projectId}/requirements` - 创建需求
- `PUT /api/projects/{projectId}/requirements/{id}` - 更新需求
- `DELETE /api/projects/{projectId}/requirements/{id}` - 删除需求

### 测试用例接口
- `GET /api/projects/{projectId}/testcases` - 获取用例列表
- `GET /api/projects/{projectId}/testcases/{id}` - 获取用例详情
- `POST /api/projects/{projectId}/testcases` - 创建用例
- `PUT /api/projects/{projectId}/testcases/{id}` - 更新用例
- `DELETE /api/projects/{projectId}/testcases/{id}` - 删除用例

### 测试计划接口
- `GET /api/projects/{projectId}/testplans` - 获取计划列表
- `GET /api/projects/{projectId}/testplans/{id}` - 获取计划详情
- `POST /api/projects/{projectId}/testplans` - 创建计划
- `PUT /api/projects/{projectId}/testplans/{id}` - 更新计划
- `DELETE /api/projects/{projectId}/testplans/{id}` - 删除计划
- `POST /api/projects/{projectId}/testplans/{planId}/cases` - 添加用例到计划
- `POST /api/projects/{projectId}/testplans/cases/{caseId}/execute` - 执行测试用例

### 缺陷接口
- `GET /api/projects/{projectId}/defects` - 获取缺陷列表
- `GET /api/projects/{projectId}/defects/{id}` - 获取缺陷详情
- `POST /api/projects/{projectId}/defects` - 提交缺陷
- `PUT /api/projects/{projectId}/defects/{id}` - 更新缺陷
- `POST /api/projects/{projectId}/defects/{id}/resolve` - 解决缺陷
- `POST /api/projects/{projectId}/defects/{id}/close` - 关闭缺陷
- `DELETE /api/projects/{projectId}/defects/{id}` - 删除缺陷
- `GET /api/projects/{projectId}/defects/statistics` - 获取缺陷统计

## 数据库表结构

主要实体表：
- `sys_user` - 用户表
- `sys_role` - 角色表
- `sys_permission` - 权限表
- `sys_user_role` - 用户角色关联表
- `sys_role_permission` - 角色权限关联表
- `project` - 项目表
- `project_member` - 项目成员表
- `requirement` - 需求表
- `test_case` - 测试用例表
- `test_case_module` - 用例模块表
- `test_plan` - 测试计划表
- `test_plan_case` - 计划用例关联表
- `defect` - 缺陷表
- `defect_attachment` - 缺陷附件表

## 开发指南

### 后端开发规范
- 使用Lombok简化代码
- 使用MapStruct进行对象映射
- RESTful API设计原则
- 统一响应格式
- 全局异常处理

### 前端开发规范
- TypeScript严格模式
- 组件化开发
- 响应式设计
- Ant Design组件库
- Zustand状态管理

## 许可证

MIT License
