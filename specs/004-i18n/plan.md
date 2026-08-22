# 004-i18n 模块技术方案

> 模块：多语言国际化（i18n）
> 项目：tpl-app-api（tpl-workspace后端，用户侧 API）
> 文档类型：技术方案（plan.md）
> 对应规格：[spec.md](./spec.md)
> 生成日期：2026-08-18
> 父工程规格引用：需求基准见 [../../../specs/004-i18n/spec.md](../../../specs/004-i18n/spec.md)，跨工程落地见 [../../../specs/004-i18n/plan.md](../../../specs/004-i18n/plan.md)
> 说明：本文档为 As-Built 回溯性方案，描述本工程（后端）模块的实际落地实现。

---

## 1. 技术上下文

### 1.1 后端 i18n 机制

tpl-app-api 在本模块的落点是「后端消息 key 化」，不改动语言选择逻辑（语言由前端感知）。

数据流：

```
tpl-app-api 返回 R.msg = key（如 "message.error.loginFailed"）
        │
        ▼
各前端 t(key) 取本端语料（父工程 sync-i18n.mjs 生成的原生格式）
        │
        ▼
未命中 → 回退显示原 key（不白屏）
```

后端职责边界：只返回 key，不翻译，不感知语言。文案事实源在父工程 `i18n/{common,app}/{zh-CN,en-US,zh-TW}.json`。

### 1.2 技术栈

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | Java | 21 | 主力开发语言 |
| 框架 | Spring Boot | 4.1.0 | IoC、Web、自动配置 |
| 认证鉴权 | Sa-Token | 1.45.0 | 未登录/无权限异常（`NotLoginException`） |
| 简化代码 | Lombok | — | `@Data`、`@Slf4j` |

> 本模块不引入新依赖，仅在现有代码上做 key 化改造。

---

## 2. 实现策略

### 2.1 R.java 改造

统一响应类 `model/R.java` 做两处改造：

1. 新增 `msgArgs` 字段：`private String[] msgArgs = new String[0];`，承载带参消息插值参数，默认空数组（契约预留，当前后端暂无带参 key）。
2. `R.ok(data)` 的 `msg` 改为 `MessageKey.SUCCESS`，成功响应不再返回硬编码 `"success"`。

`msg` 字段语义已明确为「i18n key」，Javadoc 同步标注；`R.fail(...)` 系列的 `msg` 参数由调用方传入 `MessageKey` 常量。

### 2.2 MessageKey 常量类

新增 `model/MessageKey.java`（`final` 类，私有构造器，纯常量），集中声明后端可返回的全部 47 个 key 常量，分类如下：

| 分类 | key 前缀 | 数量 | 示例 |
|------|---------|:---:|------|
| 信息反馈 | `message.*` | 3 | `SUCCESS = "message.success"`、`USER_CREATED`、`SMS_NOT_EXPIRED` |
| 通用错误 | `message.error.*` | 6 | `UNAUTHORIZED`、`FORBIDDEN`、`BAD_REQUEST`、`SERVER_BUSY`、`REQUEST_BODY_EMPTY`、`DECRYPT_FAILED` |
| 用户/认证 | `message.error.*` | 7 | `LOGIN_FAILED`、`USER_NOT_FOUND`、`USER_DISABLED`、`UNSUPPORTED_GRANT_TYPE` 等 |
| 手机号/密码/校验 | `message.error.*` | 8 | `PHONE_EMPTY`、`PASSWORD_MISMATCH` 等 |
| 验证码/短信 | `message.error.*` | 6 | `CAPTCHA_REQUIRED`、`SMS_CODE_INCORRECT`、`SMS_SEND_FAILED` 等 |
| 微信/三方登录 | `message.error.*` | 13 | `WECHAT_LOGIN_FAILED`、`WECHAT_XCX_CODE_REQUIRED` 等 |
| 三方绑定 | `message.error.*` | 3 | `SOCIAL_PARAM_REQUIRED`、`SOCIAL_ALREADY_BOUND` 等 |
| **合计** | | **47** | |

命名规范：常量名全大写下划线（对应 key 的 camelCase 转大写），key 值严格等于父工程语料键（如 `LOGIN_FAILED = "message.error.loginFailed"`）。

### 2.3 13 文件迁移（62 处替换）

把 13 个文件中散落的硬编码中文消息替换为 `MessageKey` 常量引用，共 62 处：

| # | 文件 | MessageKey 引用数 | 涉及场景 |
|---|------|:---:|------|
| 1 | `controller/AuthController.java` | 10 | 注册/登录/登出/验证码端点 |
| 2 | `controller/SysSocialController.java` | 1 | 三方登录端点 |
| 3 | `service/AuthService.java` | 14 | 注册/登录/验证码业务逻辑 |
| 4 | `service/UserService.java` | 10 | 用户资料/密码修改 |
| 5 | `service/SmsService.java` | 4 | 短信发送 |
| 6 | `service/CaptchaService.java` | 3 | 图形验证码校验 |
| 7 | `service/SysSocialService.java` | 1 | 三方绑定/解绑 |
| 8 | `service/strategy/XcxAuthStrategy.java` | 7 | 小程序一键登录 |
| 9 | `service/strategy/SocialAuthStrategy.java` | 3 | 网站/App 扫码登录 |
| 10 | `service/strategy/IAuthStrategy.java` | 1 | 认证策略接口 |
| 11 | `util/WechatAuthUtils.java` | 2 | 微信凭据工具 |
| 12 | `exception/GlobalExceptionHandler.java` | 5 | 全局异常兜底 |
| 13 | `filter/ApiDecryptFilter.java` | 1 | 请求解密失败响应 |
| **合计** | | **62** | |

迁移后校验：13 个文件中 `R.fail(...)` 与 `throw new ...Exception(...)` 无硬编码中文字符串，剩余中文仅为日志与注释。

### 2.4 全局异常处理器（GlobalExceptionHandler）

`exception/GlobalExceptionHandler.java` 的返回 `msg` 统一 key 化：

| 异常 | HTTP | R.msg |
|------|------|-------|
| `NotLoginException` | 401 | `MessageKey.UNAUTHORIZED` |
| `NotPermissionException` / `NotRoleException` | 403 | `MessageKey.FORBIDDEN` |
| `AuthException`（微信授权） | 200 | `e.getMessage()` 非空时透传，否则 `MessageKey.WECHAT_LOGIN_FAILED` |
| `IllegalArgumentException` | 200 | `e.getMessage()`（Service 已传 key） |
| `IllegalStateException` | 200 | `e.getMessage()` |
| `HttpMessageNotReadableException` | 200 | `MessageKey.BAD_REQUEST` |
| `Exception`（兜底） | 500 | `MessageKey.SERVER_BUSY` |

异常处理器内的 `log.warn` / `log.error` 保持中文原文（如「未登录访问被拦截」「系统异常」）。

### 2.5 解密过滤器（ApiDecryptFilter）

`filter/ApiDecryptFilter.java` 解密失败时直接写响应体，`msg` 固定为 `MessageKey.DECRYPT_FAILED`：

```java
response.getWriter().write("{\"code\":500,\"msg\":\"" + MessageKey.DECRYPT_FAILED + "\",\"data\":null}");
```

`log.error("请求解密失败", e)` 保持中文。

### 2.6 日志保持中文

`log.info` / `log.warn` / `log.error` 一律保持中文原文，不替换为 key。日志面向运维与开发排查，不面向最终用户，故不受 key 化约束。

---

## 3. 文件清单

| 文件 | 改动 | 说明 |
|------|------|------|
| `model/R.java` | 修改 | 新增 `msgArgs` 字段；`R.ok()` 的 msg 改为 `MessageKey.SUCCESS` |
| `model/MessageKey.java` | 新增 | 集中声明 47 个可返回 key 常量 |
| `controller/AuthController.java` | 修改 | 硬编码消息 → `MessageKey` |
| `controller/SysSocialController.java` | 修改 | 同上 |
| `service/AuthService.java` | 修改 | 同上 |
| `service/UserService.java` | 修改 | 同上 |
| `service/SmsService.java` | 修改 | 同上 |
| `service/CaptchaService.java` | 修改 | 同上 |
| `service/SysSocialService.java` | 修改 | 同上 |
| `service/strategy/IAuthStrategy.java` | 修改 | 同上 |
| `service/strategy/SocialAuthStrategy.java` | 修改 | 同上 |
| `service/strategy/XcxAuthStrategy.java` | 修改 | 同上 |
| `util/WechatAuthUtils.java` | 修改 | 同上 |
| `exception/GlobalExceptionHandler.java` | 修改 | 返回 msg key 化 |
| `filter/ApiDecryptFilter.java` | 修改 | 解密失败响应 key 化 |

---

## 4. 契约校验

后端可返回的 key 必须 ⊆ 父工程语料键，由父工程 `i18n/scripts/verify-i18n.mjs` 的契约校验卡口保证：

- 脚本解析 `MessageKey.java` 中形如 `"message.*"` 的字符串字面量，逐项检查是否存在于语料键集合（`common` + `app` 的 zh-CN）。
- 任一 key 不在语料中 → CI 阻断，提示 `[contract] MessageKey 使用了语料不存在的 key: xxx`。

校验触发时机：语料变更经 CI 执行 `verify-i18n.mjs`（失败阻断）→ `sync-i18n.mjs`（生成分发）。

---

## 5. 宪法符合性

> 对照父工程宪法 [../../../specs/constitution.md](../../../specs/constitution.md) 逐项检查。

| # | 原则 | 合规状态 | 证据 |
|---|------|---------|------|
| 1 | 框架继承，不复制源码 | ✅ 合规 | 仅在 `org.fellow99.tpl.appapi` 自定义代码内做 key 化改造，不 fork RVP 源码 |
| 2 | RuoYi 标准表零修改（sys_* Frozen） | ✅ 合规 | 本模块纯代码改造，无任何 DDL 变更；`dict.*` 语料仅引用 `sys_dict_data` 的 `dict_value`，不改字典表 |
| 3 | 命名规范统一 | ✅ 合规 | key 值遵循扁平点号命名（`message.*` / `message.error.*`），常量类置于 `org.fellow99.tpl.appapi.model` 包 |
| 4 | 模块编号跨端对齐 | ✅ 合规 | 模块编号 `004` 在父工程（`specs/004-i18n/`）与本工程（`tpl-app-api/specs/004-i18n/`）一致 |
| 10 | 构建与部署一致 | ✅ 合规 | 语料分发走 CI，本工程仅新增/修改 Java 源文件，不改变构建方式 |

---

## 6. 已知边界

- 后端仅返回 key，不感知语言；语言选择、翻译、兜底显示原 key 均由前端负责。
- `GlobalExceptionHandler.handleAuthException` 对 `AuthException` 透传 `e.getMessage()`（第三方 SDK 原始文案），该路径可能返回非 key 文案，属已知边界，后续可按需收敛。
- `msgArgs` 字段当前为契约预留，后端暂无实际带参 key；未来引入带参消息时再补插值场景。
