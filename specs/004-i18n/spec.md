# 004-i18n 模块规格文档

> 模块：多语言国际化（i18n）
> 项目：tpl-app-api（tpl-workspace后端，用户侧 API）
> 文档类型：功能规格（spec.md）
> 状态：已实现
> 生成日期：2026-08-18
> 父工程规格引用：需求基准见 [../../../specs/004-i18n/spec.md](../../../specs/004-i18n/spec.md)，本文档仅补充本工程（后端）特有规格

---

## 1. 模块概述

### 1.1 模块目的

多语言国际化模块（004-i18n）在 tpl-app-api 侧的落点是「后端消息 key 化」：面向前端的统一响应 `R` 的 `msg` 字段一律返回 i18n key，不再返回真实中文文案。前端各端拿 key 到本端语料取对应语言文案，实现一次改动、多语言切换、文案与后端代码解耦。

语言集、语料命名规范、同步管道、前端接入等通用需求由父工程 spec.md 定义，本文档不重复，仅补充后端特有规格。

### 1.2 模块边界

**在范围内**：
- `R.msg` 返回 i18n key
- `MessageKey` 常量类（集中声明可返回 key）
- 13 个 controller/service/util/exception/filter 文件的硬编码中文消息替换为 `MessageKey` 引用
- 后端日志保持中文原文

**不在范围内**：
- 语料 JSON 维护（父工程 `i18n/`）
- 前端 i18n 接入（tpl-app-web / tpl-app-android / tpl-app-harmony / tpl-app-mini）
- 语料同步与校验脚本（父工程 `i18n/scripts/`）
- tpl-manage / tpl-manage-ui（本轮不改动）

---

## 2. 本工程特有功能需求

> 本节为 tpl-app-api 特有规格，FR 编号沿用模块前缀 004，从 001 起。父工程 FR-004-001~020 为跨工程需求（语言集、命名规范、同步管道、前端接入等），本节 FR 为后端落地规格，二者作用域不同、互不冲突。

### 2.1 统一响应 key 化

- **FR-004-001**：统一响应 `R` 的 `msg` 字段 MUST 承载 i18n key，MUST NOT 承载真实中文文案。前端据此 key 到本端语料取对应语言文案。
- **FR-004-002**：`R.ok()` 与 `R.ok(data)` 的 `msg` MUST 置为 `MessageKey.SUCCESS`（即 `"message.success"`）。
- **FR-004-003**：`R.fail(msg)`、`R.fail(code, msg)`、`R.fail(code, msg, data)` 的 `msg` 参数 MUST 传入 `MessageKey` 常量（key），MUST NOT 传硬编码中文字符串。

### 2.2 带参消息

- **FR-004-004**：`R` MUST 提供 `msgArgs` 字段（`String[]`，默认 `new String[0]`），用于带参消息的插值参数，由前端按 `{name}` 占位符插值。

### 2.3 MessageKey 常量类

- **FR-004-005**：后端 MUST 建立 `MessageKey` 常量类（`org.fellow99.tpl.appapi.model.MessageKey`），集中声明全部可返回 key，MUST NOT 在业务代码中散落魔法字符串。
- **FR-004-006**：`MessageKey` 常量值 MUST 遵循父工程语料命名规范：信息反馈用 `message.*`，错误/校验反馈用 `message.error.*`。
- **FR-004-007**：`MessageKey` 声明的全部 key MUST 是父工程 i18n 语料键的子集（后端可返回的 key ⊆ 语料键），由父工程 `i18n/scripts/verify-i18n.mjs` 的契约校验卡口保证。

### 2.4 日志语言

- **FR-004-008**：后端日志（`log.info` / `log.warn` / `log.error`）MUST 保持中文原文，MUST NOT 替换为 i18n key。日志仅供运维与开发排查，不面向最终用户。

---

## 3. 关键契约

### 3.1 后端返回 key 契约

| 场景 | R.msg | msgArgs | 说明 |
|------|-------|---------|------|
| 操作成功 | `"message.success"` | `[]` | `R.ok()` 内置 |
| 登录失败 | `"message.error.loginFailed"` | `[]` | `MessageKey.LOGIN_FAILED` |
| 未登录访问 | `"message.error.unauthorized"` | `[]` | HTTP 401，`MessageKey.UNAUTHORIZED` |

### 3.2 带参消息（契约预留）

带参消息通过「key + `msgArgs` 数组」传递，占位符统一 `{name}`，三语言必须一一对应。当前后端暂无带参 key，`msgArgs` 字段为契约预留，未来引入带参消息时再补插值场景。

---

## 4. 验收场景

| # | 场景 | 前置条件 | 期望结果 |
|---|------|----------|----------|
| AC-01 | 登录失败返回 key | 用户名或密码错误 | `R.code=500`，`R.msg="message.error.loginFailed"`（非中文文案） |
| AC-02 | 操作成功返回 key | 任意 `R.ok()` 调用 | `R.msg="message.success"` |
| AC-03 | 未登录访问 | 无 Token 访问受保护端点 | HTTP 401，`R.msg="message.error.unauthorized"` |
| AC-04 | 契约校验 | CI 执行 `verify-i18n.mjs` | `MessageKey` 全部 key 均在语料中，无契约错误 |
| AC-05 | 日志保持中文 | 触发任意异常 | 服务端日志输出中文原文，非 key |

---

## 5. 约束与假设

- 后端只返回 key，不感知语言；语言选择与翻译由前端负责。
- `MessageKey` 常量值一旦发布即成为前后端契约，MUST 与父工程语料同步变更。
- 语料变更 MUST 经父工程 CI 校验（`verify-i18n.mjs`）通过后方可分发。
- 未命中兜底（前端语料无该 key 时显示原 key）由前端实现，后端不感知。
