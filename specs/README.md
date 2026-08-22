# 规格文档索引

**项目名称：** tpl-app-api（tpl-workspace后端 — 用户侧API）
**版本：** 1.0.0-SNAPSHOT
**技术栈：** Spring Boot 4.1.0 / Java 21 / MyBatis-Plus 3.5.17 / Sa-Token 1.45.0 / PostgreSQL / Redis
**文档生成时间：** 2026-08-12
**最后更新：** 2026-08-12

---

## 一、文档总览

| 层级 | 分类 | 文档数量 | 说明 |
|------|------|---------|------|
| 整体 | 项目级顶层文档 | 6 | 架构、技术、宪法、结构、API 等全局文档 |
| 整体 | 整体规格文档 | 5 | overall-* 系列文档（规格/方案/数据模型/接口/测试） |
| 模块 | 用户认证（002-user-auth） | 2 | 注册、登录、登出、验证码 |
| 模块 | 国际化（004-i18n） | 2 | 后端消息 key 化 |
| 模块 | 短信平台对接（202-sms-integration） | 3 | 短信验证码发送 |
| **合计** | **5 目录 / 18 文件** | | |

---

## 二、项目级顶层文档

全局性的架构、技术、宪法等文档，定义项目基线和开发准则。

| 文档 | 路径 | 说明 |
|------|------|------|
| **方案总纲** | [ARCHITECTURE.md](./ARCHITECTURE.md) | 系统分层架构图、3 条核心数据流、部署拓扑 |
| **技术选型** | [TECH.md](./TECH.md) | 12 项核心技术栈选型、版本、依赖关系图 |
| **宪法原则** | [constitution.md](./constitution.md) | 10 条从代码中提取的编码规范与架构约束 |
| **项目结构** | [STRUCTURE.md](./STRUCTURE.md) | 源码目录树、API 路由清单（5 端点）、数据库表映射 |
| **API 清单** | [overall-api.md](./overall-api.md) | 5 个 REST API 端点完整契约（含请求/响应示例、错误码） |
| **检查清单** | [SPECS_CHECKLIST.md](./SPECS_CHECKLIST.md) | 规格文档完成度追踪（15 项） |

### 整体规格文档

描述跨模块的全局规格、方案和数据模型。

| 文档 | 路径 | 说明 |
|------|------|------|
| **整体规格** | [overall-spec.md](./overall-spec.md) | 系统级功能规格 — 用户故事、功能需求、NFR |
| **整体方案** | [overall-plan.md](./overall-plan.md) | 系统级技术实现方案 — 宪法合规检查、已知改进项 |
| **数据模型** | [overall-data-model.md](./overall-data-model.md) | 实体定义、ER 关系图、DTO、完整验证规则 |
| **接口模型** | [overall-api.md](./overall-api.md) | API 端点详细定义、通用规范 |
| **测试用例索引** | [overall-test-cases.md](./overall-test-cases.md) | 全模块测试策略与覆盖说明 |

> 注：仅列出实际生成的文档，未生成的文档不在此表中。

---

## 三、业务模块（002 / 004 / 202）

### 002 — 用户认证（user-auth）

> 用户认证模块是 tpl-app-api 的入口模块，负责用户身份注册、登录认证、会话管理和图形验证码签发。

| 文档 | 链接 | 说明 |
|------|------|------|
| 功能规格 | [002-user-auth/spec.md](./002-user-auth/spec.md) | 用户故事、功能需求、验收场景 |
| 技术方案 | [002-user-auth/plan.md](./002-user-auth/plan.md) | 认证架构设计、源代码实现分析、已知问题 |

### 004 — 国际化（i18n）

> 后端消息 key 化：`R.msg` 返回 i18n key，由前端按语言取文案。

| 文档 | 链接 | 说明 |
|------|------|------|
| 功能规格 | [004-i18n/spec.md](./004-i18n/spec.md) | 后端返回 key 契约 |
| 技术方案 | [004-i18n/plan.md](./004-i18n/plan.md) | MessageKey 常量类、13 文件迁移 |

### 202 — 短信平台 API 对接（sms-integration）

> 对接 Spug Push 短信平台，实现短信验证码发送能力，封装为 `SmsService` 功能类供注册、登录、找回密码等模块调用。

| 文档 | 链接 | 说明 |
|------|------|------|
| 功能规格 | [202-sms-integration/spec.md](./202-sms-integration/spec.md) | 短信验证码发送功能规格 |
| 技术方案 | [202-sms-integration/plan.md](./202-sms-integration/plan.md) | SmsService/SmsProperties/SmsSendResult 落地实现 |
| 测试用例 | [202-sms-integration/test-cases.md](./202-sms-integration/test-cases.md) | 单元测试用例（参数校验 + 配置默认值） |

---

## 四、模块编号一览

| 编号 | 模块名 | 英文名 | 分类 |
|------|--------|--------|------|
| 002 | 用户认证 | user-auth | 业务模块 |
| 004 | 国际化 | i18n | 基础设施 |
| 202 | 短信平台 API 对接 | sms-integration | 基础设施 |

> 注：编号连续，无跳号。

---

## 五、模块文档结构规范

每个模块目录 `NNN-name/` 下包含以下标准文档：

| 文件 | 命名 | 说明 |
|------|------|------|
| 功能规格 | `spec.md` | 定义模块的功能需求、用户故事、验收标准（技术无关） |
| 技术方案 | `plan.md` | 模块的技术实现方案、架构决策、源代码分析、文件清单 |

> 如项目需要，模块目录还可扩展以下文档：
> - `tasks.md` — 开发任务拆解、依赖关系、里程碑
> - `api.md` — 模块涉及的 API 接口定义
> - `data-model.md` — 模块所需的实体、类型、枚举定义
> - `test-cases.md` — 模块 API 功能测试用例

---

## 六、关联工程

### 用户端前端工程 — tpl-app-web

| 项目 | 路径 | 技术栈 | 说明 |
|------|------|--------|------|
| tpl-app-web | `..\tpl-app-web\` | Vue 3.5 + Vite 6 + TypeScript 5.6 | 本 API 的前端消费方（SPA 应用） |

**tpl-app-web 规范文档**（20 份）：

| 文档 | 链接 | 说明 |
|------|------|------|
| 工程 README | [../tpl-app-web/README.md](../tpl-app-web/README.md) | 前端工程说明、快速开始、技术栈 |
| 规范文档索引 | [../tpl-app-web/specs/README.md](../tpl-app-web/specs/README.md) | 全部 20 份前端规范文档导航 |
| 前端架构 | [../tpl-app-web/specs/ARCHITECTURE.md](../tpl-app-web/specs/ARCHITECTURE.md) | 前端分层架构、数据流、部署上下文 |
| 前端宪法 | [../tpl-app-web/specs/constitution.md](../tpl-app-web/specs/constitution.md) | 7 类前端编码原则 |
| 前端 API 清单 | [../tpl-app-web/specs/API.md](../tpl-app-web/specs/API.md) | 前端 HTTP 客户端、拦截器、API 映射 |
| 整体规格 | [../tpl-app-web/specs/overall-spec.md](../tpl-app-web/specs/overall-spec.md) | 前端系统级功能规格 |

**tpl-app-web 模块文档**（与本工程模块对应）：

| 前端模块 | 链接 | 对应后端模块 |
|---------|------|-------------|
| 002-user-auth（用户认证） | [spec](../tpl-app-web/specs/002-user-auth/spec.md) / [plan](../tpl-app-web/specs/002-user-auth/plan.md) / [test-cases](../tpl-app-web/specs/002-user-auth/test-cases.md) | [002-user-auth](./002-user-auth/) |
| 001-app-shell（应用外壳） | [spec](../tpl-app-web/specs/001-app-shell/spec.md) / [plan](../tpl-app-web/specs/001-app-shell/plan.md) | 前端基础设施（无后端对应） |

### 管理后台工程

| 工程 | 路径 | 关系 | 规格文档 |
|------|------|------|---------|
| `tpl-manage` | `..\tpl-manage\` | 管理后台后端（RuoYi-Vue-Plus），共享数据库 | `..\tpl-manage\specs\` |
| `tpl-manage-ui` | `..\tpl-manage-ui\` | 管理后台前端（Vue 3 + Element Plus） | `..\tpl-manage-ui\specs\` |

---

## 七、快速导航

| 目标读者 | 推荐阅读顺序 |
|---------|-------------|
| **新加入开发者** | constitution.md → STRUCTURE.md → overall-spec.md → 002-user-auth/spec.md |
| **架构师 / Tech Lead** | ARCHITECTURE.md → TECH.md → overall-plan.md → overall-api.md |
| **后端开发** | STRUCTURE.md → overall-api.md → overall-data-model.md → 002-user-auth/plan.md |
| **前端开发（tpl-app-web）** | overall-api.md → overall-spec.md → 002-user-auth/spec.md |
| **测试 / QA** | overall-test-cases.md → SPECS_CHECKLIST.md → overall-spec.md（验收场景） |
| **产品经理** | overall-spec.md → 002-user-auth/spec.md |

---

## 七、父工程规范文档（tpl-workspace 产品线）

tpl-workspace产品线级规范文档位于父工程 `../../specs/`，用于理解本工程在整体产品中的定位与约束。

| 文档 | 链接 | 说明 |
|------|------|------|
| 父工程规范索引 | [../../specs/README.md](../../specs/README.md) | 产品线级规范文档索引（22 份） |
| 整体架构 | [../../specs/ARCHITECTURE.md](../../specs/ARCHITECTURE.md) | 产品线整体架构、子工程依赖关系、数据流、部署拓扑 |
| 宪法原则 | [../../specs/constitution.md](../../specs/constitution.md) | 产品线级开发原则（框架继承/命名/模块编号等 10 条） |
| 整体数据模型 | [../../specs/overall-data-model.md](../../specs/overall-data-model.md) | 全局数据实体、表结构、状态机、数据字典 |

---

**文档维护者：** tpl-app-api 开发团队
