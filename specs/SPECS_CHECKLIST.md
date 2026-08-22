# 规格检查清单

> 项目：tpl-app-api（tpl-workspace后端 — 用户侧API）
> 生成日期：2026-08-12
> 最后更新：2026-08-12
> 状态说明：✅ 已完成 | 🔄 进行中 | ⬜ 待完成

---

## 一、项目级文档

| # | 文档 | 路径 | 状态 | 备注 |
|---|------|------|------|------|
| P-01 | 目录结构 | [STRUCTURE.md](./STRUCTURE.md) | ✅ Done | 源码目录树 + API 路由清单 + 数据库表映射 |
| P-02 | 技术选型 | [TECH.md](./TECH.md) | ✅ Done | 12 项技术栈版本、选型理由、配置参数 |
| P-03 | 系统架构 | [ARCHITECTURE.md](./ARCHITECTURE.md) | ✅ Done | 分层架构图、3 条数据流、部署拓扑 |
| P-04 | 宪法原则 | [constitution.md](./constitution.md) | ✅ Done | 10 条从代码中提取的编码规范与架构约束 |
| P-05 | 整体规格 | [overall-spec.md](./overall-spec.md) | ✅ Done | 用户故事、功能需求、NFR |
| P-06 | 整体方案 | [overall-plan.md](./overall-plan.md) | ✅ Done | 技术方案、宪法合规检查、改进项 |
| P-07 | 数据模型 | [overall-data-model.md](./overall-data-model.md) | ✅ Done | 实体 ER 图、DTO、验证规则 |
| P-08 | 接口模型 | [overall-api.md](./overall-api.md) | ✅ Done | 5 个 API 端点完整契约（含请求/响应示例） |
| P-09 | 测试用例索引 | [overall-test-cases.md](./overall-test-cases.md) | ✅ Done | 测试策略与覆盖说明 |
| P-10 | 规格检查清单 | [SPECS_CHECKLIST.md](./SPECS_CHECKLIST.md) | ✅ Done | 本文档 |

---

## 二、模块级文档

### 002 — 用户认证（user-auth）

| # | 文档 | 路径 | 状态 | 备注 |
|---|------|------|------|------|
| M-01 | 功能规格 | [002-user-auth/spec.md](./002-user-auth/spec.md) | ✅ Done | 用户故事、需求、验收场景 |
| M-02 | 技术方案 | [002-user-auth/plan.md](./002-user-auth/plan.md) | ✅ Done | 认证架构、源代码分析、已知问题 |

### 004 — 国际化（i18n）

| # | 文档 | 路径 | 状态 | 备注 |
|---|------|------|------|------|
| M-03 | 功能规格 | [004-i18n/spec.md](./004-i18n/spec.md) | ✅ Done | 后端消息 key 化规格 |
| M-04 | 技术方案 | [004-i18n/plan.md](./004-i18n/plan.md) | ✅ Done | MessageKey 常量类、13 文件迁移 |

### 202 — 短信平台对接（sms-integration）

| # | 文档 | 路径 | 状态 | 备注 |
|---|------|------|------|------|
| M-05 | 功能规格 | [202-sms-integration/spec.md](./202-sms-integration/spec.md) | ✅ Done | 短信验证码发送规格 |
| M-06 | 技术方案 | [202-sms-integration/plan.md](./202-sms-integration/plan.md) | ✅ Done | SmsService 落地实现 |
| M-07 | 测试用例 | [202-sms-integration/test-cases.md](./202-sms-integration/test-cases.md) | ✅ Done | 参数校验 + 配置默认值 |

---

## 三、索引文档

| # | 文档 | 路径 | 状态 | 备注 |
|---|------|------|------|------|
| I-01 | 文档索引 | [README.md](./README.md) | ✅ Done | 含文档总览、模块索引、快速导航 |

---

## 四、统计

| 类别 | 总数 | 已完成 | 待完成 | 完成率 |
|------|------|--------|--------|--------|
| 项目级文档 | 10 | 10 | 0 | 100% |
| 模块级文档 | 7 | 7 | 0 | 100% |
| 索引文档 | 1 | 1 | 0 | 100% |
| **合计** | **18** | **18** | **0** | **100%** ✅ |
