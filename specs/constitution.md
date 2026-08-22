# 宪法原则

> 项目：tpl-app-api（tpl-workspace后端 — 用户侧API）
> 生成日期：2026-08-12
> 说明：以下原则从现有代码库中提取，描述当前代码**实际遵循**的编码规范和架构约束。

---

## 原则一：分层架构规范

**声明**：代码 MUST 遵循 Controller → Service → Mapper 三层架构。

**证据**：
- 所有 REST 端点均定义在 `controller/` 包中（`@RestController`）
- 业务逻辑均封装在 `service/` 包中（`@Service`）
- 数据访问均通过 `mapper/` 包中的 `BaseMapper` 子接口

**约束**：
- Controller 不直接调用 Mapper
- Service 不直接操作 HTTP 请求/响应对象
- Mapper 接口 MUST 继承 `BaseMapper<T>`

---

## 原则二：统一响应格式

**声明**：所有 API 响应 MUST 使用 `R<T>` 统一包装。

**证据**：所有 Controller 方法均返回 `R<T>` 类型。

**格式**：
```json
{
  "code": 200,        // 200=成功, 500=业务异常
  "msg": "success",   // 提示信息
  "data": { ... }     // 业务数据
}
```

**约束**：
- 成功使用 `R.ok(data)` 或 `R.ok()`
- 业务异常使用 `R.fail(message)`（HTTP 200 + Body code=500）
- HTTP 状态码仅用于少数必要场景：未登录/过期返回 401、无权限返回 403、未知系统异常返回 500（见 `GlobalExceptionHandler`）

---

## 原则三：异常处理策略

**声明**：业务异常 MUST 通过 `IllegalArgumentException` 向上抛出，由 Controller 捕获并转换为 `R.fail()`；Controller 未捕获的异常由全局异常处理器兜底。

**证据**：
- `AuthService.register()` 和 `login()` 中对参数校验失败均 `throw new IllegalArgumentException()`
- Controller 方法使用 `try-catch (IllegalArgumentException)` 统一处理
- `exception/GlobalExceptionHandler.java` 使用 `@RestControllerAdvice` 兜底处理未捕获异常

**约束**：
- Service 方法抛出 `IllegalArgumentException` 表示业务异常
- Controller 捕获该异常并返回 `R.fail(e.getMessage())`
- 全局异常处理器（`exception/GlobalExceptionHandler`）兜底处理 Controller 未捕获的异常（`AuthException`、Sa-Token `NotLoginException`、`IllegalStateException`、未知 `Exception`），保证响应体始终携带 `msg` 字段

---

## 原则四：鉴权模型 — 轻量 Token 认证

**声明**：鉴权 MUST 基于 Sa-Token JWT Token，不实现 RBAC 权限模型。

**证据**：
- Sa-Token 配置仅使用 `StpUtil.login()` / `StpUtil.logout()` / `StpUtil.checkLogin()`
- 未配置角色、权限注解（如 `@SaCheckRole`、`@SaCheckPermission`）
- `SaTokenConfigure` 使用 `SaInterceptor` 做全局登录校验
- 公开接口使用 `@SaIgnore` 标记放行

**约束**：
- 所有业务接口 MUST 经过登录校验（除非标记 `@SaIgnore`）
- 不引入角色/权限概念，不做 URL 级别的细粒度鉴权

---

## 原则五：数据库交互规范

**声明**：数据访问 MUST 通过 MyBatis-Plus `BaseMapper`，自定义 SQL SHOULD 使用注解方式写在 Mapper 接口上。

**证据**：
- 所有 Mapper 均继承 `BaseMapper<T>`
- 自定义查询（`selectByUserName`、`countByUserId`）使用 `@Select` 注解
- 无 XML Mapper 文件（`mapper-locations` 配置了但无对应文件）

**约束**：
- 逻辑删除 MUST 使用 `delFlag` 字段
- 表名 MUST 使用 `@TableName` 显式声明
- 字段映射不一致时 MUST 使用 `@TableField` 显式映射

---

## 原则六：配置管理

**声明**：环境差异配置 MUST 通过 Spring Profile 分离。

**证据**：
- `application.yml` — 公共配置
- `application-dev.yml` — 开发环境
- `application-docker.yml` — Docker 环境
- `captcha.enable` 在不同环境有不同值

**约束**：
- 数据库连接串、Redis 地址 MUST 配置在环境特定文件中
- 敏感配置（密码）不加密（生产需改进）

---

## 原则七：代码简化与风格

**声明**：代码 SHOULD 使用 Lombok 减少样板代码，SHOULD 使用构造函数注入。

**证据**：
- 所有 Entity/DTO 使用 `@Data`
- Service/Controller 使用 `@RequiredArgsConstructor` + `private final` 实现构造函数注入
- 日志使用 `@Slf4j`

**约束**：
- 字段注入（`@Autowired`）MUST NOT 使用
- 无参构造函数 MUST NOT 出现在需要注入的类中

---

## 原则八：数据库实体标识

**声明**：主键生成策略 MUST 在实体类中通过 `@TableId` 显式声明。

**证据**：
- `SysUser` 使用 `IdType.ASSIGN_ID`（雪花算法）
- 其他实体使用 `IdType.AUTO`（数据库自增）

---

## 原则九：验证码安全

**声明**：验证码 MUST 存储在 Redis 并设 TTL，MUST 一次性消费。

**证据**：
- `AuthService.generateCaptcha()` 生成验证码存 Redis，Key: `captcha_codes:{uuid}`，TTL: 5 分钟
- `validateCaptcha()` 校验后立即 `stringRedisTemplate.delete(key)`
- 支持通过 `captcha.enable` 配置项全局开关

---

## 原则十：数据库密码安全

**声明**：用户密码 MUST 使用 BCrypt 哈希存储。

**证据**：
- 注册时使用 `BCrypt.hashpw(req.getPassword())`
- 登录时使用 `BCrypt.checkpw(req.getPassword(), user.getPassword())`

---

## 合规性总结

以上原则均从实际代码中提取，描述当前代码库的**实然状态**，非应然要求。未来如引入新模块，建议继续遵循以上约定；如需偏离，应在对应模块的 `plan.md` 中明确注明理由。

---

## 文档分工治理规则（与父工程对齐）

> 本节遵循父工程宪法 `../../specs/constitution.md` 第四章「文档分工治理规则」定义的文档分工契约。本工程 MUST 遵守以下规则：

1. **模块编号对齐**：本工程模块编号 MUST 与父工程模块编号对齐（同一产品功能跨工程使用一致编号，如 002-user-auth）。
2. **父工程侧重需求规格**：产品功能需求的权威定义在父工程 `spec.md`，本工程 `spec.md` 不重复编写需求。
3. **父工程 plan 侧重实现逻辑**：父工程 `plan.md` 描述跨工程实现逻辑，本工程 `plan.md` 不重复。
4. **本工程侧重落地实现**：本工程规范文档 MUST 重点描述功能在本工程的落地实现，核心文档是 `plan.md` 与 `test-cases.md`。
5. **本工程 spec 引用父工程**：本工程 `spec.md` SHOULD 引用父工程对应 `spec.md`，再补充本工程必要的特有规格。
