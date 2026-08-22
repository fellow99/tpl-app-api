# 002-user-auth 模块规格文档

> 模块：用户认证（auth）
> 项目：tpl-app-api（tpl-workspace后端 — 用户侧API）
> 状态：已实现
> 最后更新：2026-08-12
> 对应系统规格：[overall-spec.md](../overall-spec.md) §3.1 用户认证、§3.2 验证码、§3.3 会话管理
> 父工程规格引用：需求基准见 [../../specs/002-user-auth/spec.md](../../specs/002-user-auth/spec.md)，本文档仅补充本工程（后端）特有规格

---

## 1. 模块概述

### 1.1 模块目的

用户认证模块（002-user-auth）是 tpl-app-api 的入口模块，负责用户身份注册、登录认证、会话管理和人机验证。它为用户提供首次进入系统的账号创建能力，以及后续访问受保护资源所需的身份凭证（JWT Token）签发与注销。

### 1.2 模块边界

**在范围内**：
- 图形验证码的生成与下发（GET /auth/code）
- 用户注册（POST /auth/register）
- 用户登录与 Token 签发（POST /auth/login）
- 用户登出与 Token 作废（POST /auth/logout）

**不在范围内**：
- 密码找回/重置
- 邮箱/手机验证
- 微信以外的第三方登录（QQ/微博等 OAuth2）
- 角色/权限管理（RBAC）
- 用户资料修改

---

## 2. 用户故事

### US-001：获取图形验证码

> 作为未登录用户，我可以在注册或登录前获取图形验证码，以便完成人机验证。

**验收标准**：
- 系统生成 4 位字母数字混合的图形验证码
- 返回验证码唯一标识（uuid）及 Base64 编码的 PNG 图片
- 验证码有效期为 5 分钟，过期后无法使用
- 验证码一次性消费，使用后立即作废

### US-002：用户注册

> 作为用户，我可以注册一个账号，提供用户名、密码信息，以便使用 tpl-workspace 的功能。

**验收标准**：
- 用户提供合法的用户名、密码和验证码
- 系统校验用户名唯一性，拒绝重复注册
- 系统使用 BCrypt 安全存储密码
- 注册成功后自动创建用户档案（昵称默认为用户名）

### US-003：用户登录

> 作为已注册用户，我可以使用用户名和密码登录系统，获取访问令牌以访问需要身份认证的功能。

**验收标准**：
- 用户提供正确的用户名和密码
- 系统校验密码正确性和账号状态
- 登录成功返回 JWT 访问令牌（有效期 30 天）
- 停用账号（status='1'）被拒绝登录

### US-004：用户登出

> 作为已登录用户，我可以主动登出，使当前会话令牌失效。

**验收标准**：
- 携带有效 Token 的用户请求登出
- 系统使当前 Token 立即失效
- 登出后该 Token 不再能访问任何受保护接口

---

## 3. 功能需求

> 以下需求编号沿用 [overall-spec.md](../overall-spec.md) 中的功能需求 ID，链接指向系统级定义。

### 3.1 验证码生成

- **FR-001-010**：系统 MUST 生成 4 位字母数字混合的图形验证码（实现：`CaptchaUtils.generateCaptcha()`）
- **FR-001-011**：验证码 MUST 存储于 Redis，Key 格式为 `captcha_codes:{uuid}`，TTL 为 300 秒（5 分钟）
- **FR-001-013**：验证码校验 MUST NOT 区分大小写（实现：`equalsIgnoreCase()`）

### 3.2 验证码校验

- **FR-AUTH-001**：验证码校验为私有方法 `validateCaptcha()`，接受 uuid 与 code 两个参数
- **FR-AUTH-002**：系统 MUST 在校验前检查 uuid 和 code 均非空，任一为空抛出异常 "验证码不能为空"
- **FR-AUTH-003**：系统 MUST 从 Redis 检索验证码，若不存在（已过期/已消费）抛出异常 "验证码已过期"
- **FR-AUTH-004**：系统 MUST 对检索到的验证码做大小写不敏感比较，不匹配抛出异常 "验证码错误"
- **FR-001-012**：验证码 MUST 一次性消费，校验后（无论成功与否）MUST 从 Redis 删除
- **FR-001-009**：验证码功能 MUST 支持通过 `captcha.enable` 配置项全局开关：当 `captcha.enable=false` 时，跳过 validateCaptcha() 调用

### 3.3 用户注册

- **FR-001-001**：系统 MUST 接受 username、password、code、uuid 及可选 phoneNumber 作为注册参数
- **FR-001-002**：系统 MUST 通过 `tpl_user_view` 视图检查用户名唯一性，若已存在抛出异常 "用户名已存在"
- **FR-001-004**：系统 MUST 使用 BCrypt 哈希算法存储密码（实现：`BCrypt.hashpw(password)`）
- **FR-001-005**：系统 MUST 在注册时同时创建 `sys_user` 记录和 `tpl_user_profile` 记录，操作 MUST 包裹在 `@Transactional(rollbackFor = Exception.class)` 事务中
- **FR-AUTH-005**：注册时 `sys_user.nickName` MUST 默认为 username
- **FR-AUTH-006**：注册时 `sys_user.userId` MUST 使用雪花算法（`IdType.ASSIGN_ID`）生成

### 3.4 用户登录

- **FR-AUTH-007**：系统 MUST 校验 username 和 password 均非空，任一为空抛出异常 "用户名和密码不能为空"
- **FR-AUTH-008**：系统 MUST 通过 `tpl_user_view` 视图查询用户信息，若用户不存在抛出异常 "用户不存在"
- **FR-001-006**：系统 MUST 使用 BCrypt 比对密码（实现：`BCrypt.checkpw(plainPassword, hashedPassword)`），不匹配抛出异常 "密码错误"
- **FR-001-008**：系统 MUST 检查用户状态，`status == "1"`（停用）时 MUST 拒绝登录并抛出异常 "用户已被停用"
- **FR-001-007**：登录成功后系统 MUST 调用 `StpUtil.login(userId)` 签发 JWT 令牌
- **FR-001-014**：Token 有效期 MUST 为 2592000 秒（30 天），通过 `sa-token.timeout` 配置

### 3.5 用户登出

- **FR-001-015**：系统 MUST 支持已登录用户主动登出（实现：`StpUtil.logout()`）
- **FR-AUTH-009**：登出操作 MUST 需要有效 Token（`@SaIgnore` 不标记于 logout 端点）

### 3.6 鉴权配置

- **FR-AUTH-010**：`/auth/code`、`/auth/register`、`/auth/login` 端点 MUST 使用 `@SaIgnore` 注解标记为无需登录
- **FR-AUTH-011**：`/auth/logout` 端点 MUST NOT 标记 `@SaIgnore`，需通过 `SaInterceptor` 全局登录校验
- **FR-001-016**：`SaInterceptor` MUST 拦截所有路径 `/**`，排除 `/auth/**`、`/error`、`/swagger-ui/**`、`/v3/api-docs/**`

### 3.7 验证码配置

- **FR-AUTH-012**：`AuthController.getCode()` MUST 返回 `AjaxResult`（包含 `captchaEnabled` 布尔值、`uuid` 和 `img` Base64 字符串）
- **FR-AUTH-013**：当 `captcha.enable=false` 时，`getCode()` MUST 返回 `captchaEnabled=false` 且 uuid 和 img 均为空字符串
- **FR-AUTH-014**：`captchaEnabled` 字段 MUST 通过 `@Value("${captcha.enable:true}")` 注入，开发环境默认 false，Docker 环境默认 true

---

## 4. 关键实体

> 详细字段定义参见 [overall-data-model.md](../overall-data-model.md)

### 4.1 模块直接操作的实体

| 实体 | 类型 | 用途 |
|------|------|------|
| `SysUser` | 数据库实体（`sys_user` 表） | 用户核心信息：用户名、密码（BCrypt）、状态、昵称 |
| `UserProfile` | 数据库实体（`tpl_user_profile` 表） | 用户扩展信息：生日 |
| `UserView` | 数据库视图（`tpl_user_view`） | 联表查询视图，供登录和注册校验时一次性获取完整用户信息 |

### 4.2 模块使用的 DTO

| DTO | 方向 | 用途 |
|-----|------|------|
| `RegisterRequest` | 请求 | 携带 username、password、phoneNumber、code、uuid |
| `LoginRequest` | 请求 | 携带 username、password、code、uuid |
| `LoginResponse` | 响应 | 返回 access_token 和 token_type（固定 "Bearer"） |
| `AjaxResult` | 响应 | 验证码接口返回，含 captchaEnabled、uuid、img |

### 4.3 验证码存储

| 存储项 | 位置 | Key 格式 | TTL | 说明 |
|--------|------|----------|-----|------|
| 图形验证码文本 | Redis | `captcha_codes:{uuid}` | 300s (5分钟) | 校验后立即删除 |

---

## 5. 验收场景

### 5.1 验证码获取

| # | 场景 | 前置条件 | 期望结果 |
|---|------|----------|----------|
| AC-01 | 获取验证码（开启） | `captcha.enable=true` | 返回 200，data 包含 uuid 和 Base64 PNG 图片 |
| AC-02 | 获取验证码（关闭） | `captcha.enable=false` | 返回 200，captchaEnabled=false，uuid/img 为空 |

### 5.2 用户注册

| # | 场景 | 输入 | 期望结果 |
|---|------|------|----------|
| AC-03 | 正常注册 | 合法 username、password、正确验证码 | 返回 200，sys_user + user_profile 均已创建，密码 BCrypt 加密 |
| AC-04 | 用户名已存在 | username 已存在于 tpl_user_view | 返回 code=500, msg="用户名已存在" |
| AC-07 | 验证码为空 | uuid 或 code 为空 | 返回 code=500, msg="验证码不能为空" |
| AC-08 | 验证码过期 | Redis 中无对应 uuid | 返回 code=500, msg="验证码已过期" |
| AC-09 | 验证码错误 | code 与 Redis 中值不匹配 | 返回 code=500, msg="验证码错误" |
| AC-10 | 验证码关闭时注册 | `captcha.enable=false`，不提供验证码 | 返回 200，注册成功（跳过验证码校验） |
| AC-11 | 事务回滚 | user_profile 插入失败 | sys_user 插入回滚，数据库无残留数据 |

### 5.3 用户登录

| # | 场景 | 输入 | 期望结果 |
|---|------|------|----------|
| AC-12 | 正常登录 | 正确 username、password、验证码 | 返回 200，data 包含 access_token 和 token_type="Bearer" |
| AC-13 | 用户不存在 | 不存在的 username | 返回 code=500, msg="用户不存在" |
| AC-14 | 密码错误 | 正确 username、错误 password | 返回 code=500, msg="密码错误" |
| AC-15 | 账号停用 | status="1" 的用户 | 返回 code=500, msg="用户已被停用" |
| AC-16 | 用户/密码为空 | username 或 password 为空 | 返回 code=500, msg="用户名和密码不能为空" |
| AC-17 | Token 有效期 | 正常登录 | 返回的 Token 在 30 天内有效 |

### 5.4 用户登出

| # | 场景 | 前置条件 | 期望结果 |
|---|------|----------|----------|
| AC-18 | 正常登出 | 携带有效 Token | 返回 200，Token 立即失效，后续请求返回鉴权失败 |
| AC-19 | 未登录登出 | 不携带 Token | 由 SaInterceptor 拦截，返回鉴权失败 |

---

## 6. 非功能需求

### 6.1 安全性

- **NFR-AUTH-S-001**：用户密码 MUST 使用 BCrypt 哈希存储，MUST NOT 明文存储或可逆加密
- **NFR-AUTH-S-002**：登录失败 MUST NOT 区分"用户不存在"和"密码错误"之外的信息，防止用户名枚举攻击（当前实现已满足：不存在和密码错误返回不同消息，略有信息泄露风险 [NEEDS CLARIFICATION: 是否需要统一登录失败提示为"用户名或密码错误"以增强安全性？]）
- **NFR-AUTH-S-003**：验证码 MUST 一次性消费，防止重放攻击
- **NFR-AUTH-S-004**：JWT 签名密钥当前为硬编码弱密钥 `abcdefghijklmnopqrstuvwxyz`，生产环境 SHOULD 更换为强随机密钥

### 6.2 数据完整性

- **NFR-AUTH-D-001**：注册操作 MUST 使用 `@Transactional` 确保 `sys_user` 和 `tpl_user_profile` 同时创建或同时回滚
- **NFR-AUTH-D-002**：用户数据查询 SHOULD 通过逻辑删除字段（delFlag='0'）自动过滤已删除记录

### 6.3 性能

- **NFR-AUTH-P-001**：验证码生成 SHOULD 在 100ms 内完成（图形绘制 + Base64 编码）
- **NFR-AUTH-P-002**：登录 Token 签发 SHOULD 在 200ms 内完成（含 Redis 写入）

### 6.4 可维护性

- **NFR-AUTH-M-001**：验证码功能 MUST 支持通过配置开关 `captcha.enable` 在开发/测试环境关闭
- **NFR-AUTH-M-002**：所有错误消息 MUST 以中文返回，便于前端直接展示

---

## 7. 假设与约束

| # | 假设/约束 | 说明 |
|---|-----------|------|
| A-AUTH-001 | `tpl_user_view` 数据库视图已存在 | 该视图联表 sys_user + tpl_user_profile，由数据库管理员或 tpl-manage 工程维护 |
| A-AUTH-002 | Redis 服务可用 | 验证码存储和 Sa-Token 会话管理均依赖 Redis，不可用时系统无法正常工作 |
| A-AUTH-003 | 前端负责 Token 存储和携带 | 前端（tpl-app-web）在登录成功后存储 access_token，后续请求在 Authorization Header 中携带 |
| A-AUTH-004 | 不区分用户类型 | 所有通过注册入口创建的用户均为普通用户，无角色区分 |
| A-AUTH-005 | 同账号允许多端登录 | Sa-Token 配置 `is-concurrent: true`，允许多个设备同时登录同一账号 |
| A-AUTH-006 | 数据库 `tpl_manage` 由 tpl-manage 初始化 | 本工程不负责数据库 DDL 的创建和维护 |

---

## 8. 依赖

### 8.1 本模块依赖的系统/服务

| 系统/服务 | 依赖方式 | 说明 |
|-----------|---------|------|
| PostgreSQL | JDBC 直连 | 用户数据持久存储（sys_user、tpl_user_profile 表，tpl_user_view 视图） |
| Redis | Lettuce 客户端直连 | 验证码临时存储、Sa-Token 会话 Token 管理 |

### 8.2 依赖本模块的外部系统

| 系统 | 依赖方式 | 说明 |
|------|---------|------|
| tpl-app-web（用户前端） | HTTP REST API | 注册、登录、登出、获取验证码均通过本模块接口实现 |

### 8.3 框架/库依赖

| 库 | 用途 |
|----|------|
| Sa-Token (1.45.0) | Token 签发（StpUtil.login/logout）、全局鉴权拦截（SaInterceptor） |
| MyBatis-Plus (3.5.17) | 数据库 CRUD（BaseMapper）、逻辑删除、雪花算法 ID 生成 |
| Hutool (5.8.47) | 字符串判空（StrUtil.isBlank）、随机字符生成（RandomUtil） |
| BCrypt (Spring Security 内嵌) | 密码哈希（hashpw）与校验（checkpw） |

---

## 9. 待澄清事项

| # | 问题 | 上下文 |
|---|------|--------|
| [NEEDS CLARIFICATION] | 登录失败是否需要统一返回模糊消息（如 "用户名或密码错误"）而不区分"用户不存在"和"密码错误"？ | 当前实现区分两种错误，可能被用于用户名枚举攻击 |
| [NEEDS CLARIFICATION] | 验证码校验失败时是否需要删除 Redis 中的验证码？ | 当前实现校验失败仍从 Redis 删除（一次性消费），降低了暴力破解窗口但也影响了用户体验 |
| [NEEDS CLARIFICATION] | 注册成功后的默认头像（avatar 字段）策略是什么？ | 当前代码未显式设置 avatar 字段值，sys_user 表中该字段可能为 null |

---

## 微信登录集成

> 需求基准见父工程 [../../specs/002-user-auth/spec.md](../../specs/002-user-auth/spec.md) §11，技术设计见 `docs/002-用户注册及登录设计-微信登录集成.md`（§八）。本端实现完全参考 RuoYi-Vue-Plus。

### 实现范围

| 能力 | 端点 / 策略 | 说明 |
|------|------------|------|
| 网站扫码登录 | `POST /auth/login` grantType=`social`，`source=wechat_open` | `SocialAuthStrategy`（`AuthWeChatOpenRequest`，scope=snsapi_login） |
| 移动拉起登录 | `POST /auth/login` grantType=`social`，`source=wechat_app` | 同上策略，`ignoreCheckState=true`（移动端无服务端 state 缓存） |
| 小程序一键登录 | `POST /auth/login` grantType=`xcx` | `XcxAuthStrategy`（jscode2session，`AuthWechatMiniProgramRequest`） |
| 授权 URL | `GET /auth/binding/{source}` | 仅 `wechat_open`，返回 qrconnect 授权地址 |
| 绑定 | `POST /auth/social/callback`（需 Token） | 已登录用户绑定微信 |
| 解绑 | `DELETE /auth/unlock/{socialId}`（需 Token） | 解除第三方绑定 |
| 绑定列表 | `GET /system/social/list`（需 Token） | 当前用户第三方绑定列表 |

### 账号绑定规则

- `sys_social.auth_id = source + openid`（`source` ∈ `wechat_open`/`wechat_app`/`wechat_xcx`），**零 DDL 修改**。
- 登录匹配：auth_id 命中 → 直接登录；social 未命中 → 返回业务码 `2001`（未绑定）；xcx 未命中 → 业务码 `2002`（需手机号），`phoneCode` 换手机号后绑定或建号。
- 微信凭据（AppSecret/access_token/refresh_token/session_key）仅服务端保存，禁止下发前端；`session_key` 不落库明文。

### 编译开关

- `wechat.enabled`（环境变量 `WECHAT_LOGIN_ENABLED`，默认 `true`）：关闭时不注册 social/xcx 策略 Bean、不开放相关端点（对齐既有 `captcha.enable` 开关模式）。

### 依赖

- `io.github.windtool:JustAuth:3.0.1`（windtool fork，依赖 Jackson 3，与 Spring Boot 4.1 兼容，import 包 `io.github.windtool.*`）。

---

## 验证码开关（禁用验证码）

- tpl-app-api 提供图形验证码开关配置项 captcha.enable。
- captcha.enable: false（禁用）时：后端跳过验证码校验、GET /auth/code 返回 captchaEnabled: false、本端隐藏图形验证码表单项并禁用相关功能。
- 详见父工程需求基准 specs/002-user-auth/spec.md §10。