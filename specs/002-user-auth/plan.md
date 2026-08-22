# 002-user-auth 模块技术方案

> 模块：用户认证（auth）
> 项目：tpl-app-api（tpl-workspace后端 — 用户侧API）
> 对应规格：[spec.md](./spec.md)
> 最后更新：2026-08-12
> 说明：本文档为 As-Built 回溯性方案，描述模块**实际构建**的技术实现。

---

## 1. 技术上下文

### 1.1 模块技术栈

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | Java | 21 | 主力开发语言 |
| 框架 | Spring Boot | 4.1.0 | IoC、Web、自动配置 |
| 认证鉴权 | Sa-Token | 1.45.0 | JWT Token 签发、全局鉴权拦截 |
| ORM | MyBatis-Plus | 3.5.17 | BaseMapper CRUD、雪花 ID、逻辑删除 |
| 缓存客户端 | Spring Data Redis (Lettuce) | Spring Boot 默认 | Redis 操作（验证码存储） |
| 工具库 | Hutool | 5.8.47 | StrUtil 字符串工具、RandomUtil 随机字符 |
| 密码哈希 | BCrypt (Spring Security) | 内嵌 | 密码加密与校验 |
| 简化代码 | Lombok | optional | @Data、@RequiredArgsConstructor、@Slf4j |

### 1.2 模块文件清单

```
src/main/java/org/fellow99/tpl/appapi/
├── config/
│   └── SaTokenConfigure.java           # Sa-Token 全局鉴权拦截器配置
├── controller/
│   └── AuthController.java             # /auth/* REST 端点
├── service/
│   └── AuthService.java                # 注册、登录、验证码业务逻辑
├── mapper/
│   ├── SysUserMapper.java              # sys_user 表 Mapper
│   ├── UserProfileMapper.java          # tpl_user_profile 表 Mapper
│   └── UserViewMapper.java             # tpl_user_view 视图 Mapper (含自定义 SQL)
├── entity/
│   ├── SysUser.java                    # sys_user 实体
│   ├── UserProfile.java                # tpl_user_profile 实体
│   └── UserView.java                   # tpl_user_view 视图实体
├── model/
│   ├── R.java                          # 统一响应包装类
│   └── dto/
│       ├── RegisterRequest.java        # 注册请求 DTO
│       ├── LoginRequest.java           # 登录请求 DTO
│       └── LoginResponse.java          # 登录响应 DTO
└── util/
    └── CaptchaUtils.java               # 图形验证码生成工具类
```

---

## 2. 宪法合规性检查

> 对照 [constitution.md](../constitution.md) 中的 10 条原则逐项检查。

| # | 原则 | 合规状态 | 证据 |
|---|------|---------|------|
| 一 | 分层架构规范 | ✅ 合规 | Controller(`AuthController`) → Service(`AuthService`) → Mapper(`SysUserMapper`, `UserProfileMapper`, `UserViewMapper`) 严格分层 |
| 二 | 统一响应格式 | ✅ 合规 | 所有端点返回 `R<T>`，成功 `R.ok()`，失败 `R.fail(msg)` |
| 三 | 异常处理策略 | ✅ 合规 | Service 抛 `IllegalArgumentException`（如 "用户名已存在"、"密码错误"），Controller try-catch 转为 R.fail() |
| 四 | 轻量 Token 认证 | ✅ 合规 | 仅做登录校验（`StpUtil.login/logout`、`SaInterceptor`），无 `@SaCheckRole` 或 `@SaCheckPermission` |
| 五 | 数据库交互规范 | ✅ 合规 | 所有 Mapper 继承 `BaseMapper<T>`，自定义 SQL（`UserViewMapper.selectByUserName`）使用 `@Select` 注解，无 XML 文件 |
| 六 | 配置管理 | ✅ 合规 | `captcha.enable` 在 dev(=false) 和 docker(=true) 环境中分别配置 |
| 七 | 代码简化与风格 | ✅ 合规 | `AuthController` 和 `AuthService` 均使用 `@RequiredArgsConstructor` + `private final` 构造函数注入 |
| 八 | 数据库实体标识 | ✅ 合规 | `SysUser.userId` 使用 `@TableId(type = IdType.ASSIGN_ID)`（雪花算法），`UserProfile.profileId` 使用 `@TableId(type = IdType.AUTO)` |
| 九 | 验证码安全 | ✅ 合规 | Redis Key: `captcha_codes:{uuid}`，TTL 5 分钟，校验后删除，大小写不敏感 |
| 十 | 数据库密码安全 | ✅ 合规 | 注册 `BCrypt.hashpw()`，登录 `BCrypt.checkpw()` |
| - | JWT 密钥安全 | ❌ 违规 | 密钥 `abcdefghijklmnopqrstuvwxyz` 为硬编码弱密钥（见 §7 已知问题） |

---

## 3. 接口契约

> 对应规格 §3 功能需求，详细定义参见 [overall-api.md](../overall-api.md) §2.1-2.4。

### 3.1 接口总览

| # | Path | Method | 鉴权 | @SaIgnore | 实现需求 | 入口文件 |
|---|------|--------|------|-----------|----------|----------|
| 1 | `/auth/code` | GET | 否 | 是 | FR-001-010, FR-001-011, FR-AUTH-012-014 | `AuthController.java:48` |
| 2 | `/auth/register` | POST | 否 | 是 | FR-001-001~005, FR-AUTH-001~006 | `AuthController.java:58` |
| 3 | `/auth/login` | POST | 否 | 是 | FR-AUTH-007~008, FR-001-006~008, FR-001-014 | `AuthController.java:72` |
| 4 | `/auth/logout` | POST | 是 | 否 | FR-001-015, FR-AUTH-009 | `AuthController.java:83` |

### 3.2 Sa-Token 鉴权配置

**拦截器注册**（`SaTokenConfigure.java`）：

```java
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/auth/**",      // 所有认证端点放行
                    "/error",        // Spring Boot 错误处理
                    "/swagger-ui/**", // Swagger UI
                    "/v3/api-docs/**" // OpenAPI 文档
                );
    }
}
```

**注解放行**（`AuthController.java`）：
- `@SaIgnore` 标记在 `getCode()`、`register()`、`login()` 方法上
- `logout()` 不标记 `@SaIgnore`，依赖 `SaInterceptor` 做登录校验

### 3.3 Token 生成

**实现**（`AuthService.login()`）：
```java
StpUtil.login(userId);
String token = StpUtil.getTokenValue();
```

**关键配置**（`application.yml`）：
```yaml
sa-token:
  token-name: Authorization
  timeout: 2592000       # 30 天
  is-concurrent: true    # 允许多端登录
  token-style: tik
  jwt-secret-key: abcdefghijklmnopqrstuvwxyz  # ⚠️ 待更换
```

### 3.4 异常处理模式

**Controller 层统一模式**（`AuthController.java`）：

```java
@PostMapping("/register")
public R<Void> register(@RequestBody RegisterRequest req) {
    try {
        authService.register(req);
        return R.ok();
    } catch (IllegalArgumentException e) {
        return R.fail(e.getMessage());
    }
}
```

每个 Controller 方法独立 try-catch，无 `@ControllerAdvice` 全局异常处理器。

---

## 4. 数据模型

> 详细字段定义参见 [overall-data-model.md](../overall-data-model.md)

### 4.1 数据库表映射

| 表/视图 | 实体类 | Mapper | 主键策略 | 模块操作 |
|---------|--------|--------|---------|---------|
| `sys_user` | `SysUser.java` | `SysUserMapper.java` | `IdType.ASSIGN_ID`（雪花） | INSERT（注册） |
| `tpl_user_profile` | `UserProfile.java` | `UserProfileMapper.java` | `IdType.AUTO`（自增） | INSERT（注册） |
| `tpl_user_view` | `UserView.java` | `UserViewMapper.java` | 无（视图） | SELECT（登录查询、注册查重） |

### 4.2 自定义 SQL

| Mapper 方法 | SQL | 用途 |
|-------------|-----|------|
| `UserViewMapper.selectByUserName(String userName)` | `@Select("SELECT * FROM tpl_user_view WHERE user_name = #{userName} AND del_flag = '0'")` | 按用户名查询用户完整信息，供登录和注册查重使用 |

### 4.3 验证码 Redis 存储

| Key 格式 | Value | TTL | 写入方 | 消费方 |
|----------|-------|-----|--------|--------|
| `captcha_codes:{uuid}` | 4 位验证码文本（大小写不敏感） | 300 秒 | `AuthService.generateCaptcha()` | `AuthService.validateCaptcha()` |

**实现细节**：
```java
// 写入
stringRedisTemplate.opsForValue().set(
    "captcha_codes:" + uuid, code, 300, TimeUnit.SECONDS
);

// 校验 + 删除
String code = stringRedisTemplate.opsForValue().get(key);
// ... 校验逻辑 ...
stringRedisTemplate.delete(key);
```

### 4.4 DTO 字段映射

**RegisterRequest → SysUser + UserProfile**（`AuthService.register()`）：

| RegisterRequest 字段 | 目标实体 | 目标字段 | 转换 |
|---------------------|----------|----------|------|
| `username` | `SysUser` | `userName` | 直接赋值 |
| `password` | `SysUser` | `password` | `BCrypt.hashpw()` |
| `username` | `SysUser` | `nickName` | 直接赋值（昵称默认为用户名） |
| `phoneNumber` | `SysUser` | `phoneNumber` | 直接赋值（可选字段） |
| `birthDate` | `UserProfile` | `birthDate` | 直接赋值 |
| (自动生成) | `SysUser` | `userId` | `IdType.ASSIGN_ID` 雪花算法 |
| (关联) | `UserProfile` | `userId` | 引用已生成的 `SysUser.userId` |

---

## 5. 实现策略

### 5.1 验证码生成流程

**入口**：`AuthController.getCode()` → `AuthService.generateCaptcha()`

**步骤**（实现 `FR-001-010`, `FR-001-011`, `FR-AUTH-012-014`）：

1. 调用 `CaptchaUtils.generateCaptcha()` 生成 `CaptchaResult`（含 4 位 code 文本 + Base64 PNG 图片）
2. 使用 `IdUtil.fastUUID()` 生成 uuid
3. 将验证码 code 存入 Redis：Key = `captcha_codes:{uuid}`，TTL = 300 秒
4. 构建 `AjaxResult` 返回 `{captchaEnabled: true, uuid: ..., img: ...}`
5. 若 `captcha.enable=false`，跳过 CaptchaUtils 调用和 Redis 存储，返回 `{captchaEnabled: false, uuid: "", img: ""}`

**CaptchaUtils 实现要点**：
- 使用 `RandomUtil.randomString(4)` 生成 4 位随机字母数字
- 使用 Java `BufferedImage` 绘制图形（含干扰线）
- 输出为 PNG 格式，Base64 编码

### 5.2 用户注册流程

**入口**：`AuthController.register()` → `AuthService.register(RegisterRequest)`

**步骤**（实现 `FR-001-001~005`, `FR-AUTH-001~006`）：

1. 若 `captchaEnabled`，调用 `validateCaptcha(req.getUuid(), req.getCode())` 校验验证码
2. 校验用户名唯一性：`userViewMapper.selectByUserName(req.getUsername())` 返回 null 则通过，非 null 则 "用户名已存在"
3. BCrypt 哈希密码：`BCrypt.hashpw(req.getPassword(), BCrypt.gensalt())`
4. 构造 `SysUser` 对象并 `sysUserMapper.insert()`
5. 以 `sysUser.getUserId()` 构造 `UserProfile` 对象并 `userProfileMapper.insert()`
6. **事务管理**：方法标记 `@Transactional(rollbackFor = Exception.class)`，步骤 4/5 任一失败则全部回滚

### 5.3 用户登录流程

**入口**：`AuthController.login()` → `AuthService.login(LoginRequest)`

**步骤**（实现 `FR-AUTH-007~008`, `FR-001-006~008`, `FR-001-014`）：

1. 校验 username 和 password 均非空
2. 若 `captchaEnabled`，调用 `validateCaptcha(req.getUuid(), req.getCode())` 校验验证码
3. 查询用户：`userViewMapper.selectByUserName(req.getUsername())`，null 则 "用户不存在"
4. 密码校验：`BCrypt.checkpw(req.getPassword(), userView.getPassword())`，false 则 "密码错误"
5. 状态校验：`"1".equals(userView.getStatus())`，true 则 "用户已被停用"
6. 签发 Token：`StpUtil.login(userView.getUserId())`
7. 获取 Token 值：`StpUtil.getTokenValue()`
8. 构建 `LoginResponse`（access_token, token_type="Bearer"）并返回
9. 记录日志：`log.info("用户登录成功，userId={}, userName={}", ...)`

### 5.4 用户登出流程

**入口**：`AuthController.logout()` → `AuthService.logout()`

**步骤**（实现 `FR-001-015`, `FR-AUTH-009`）：

1. 调用 `StpUtil.logout()` 使当前 Token 立即失效
2. 返回 `R.ok()`

### 5.5 验证码校验（私有方法）

**入口**：`AuthService.validateCaptcha(String uuid, String code)`（private）

**步骤**（实现 `FR-AUTH-001~004`, `FR-001-012`）：

1. 校验 uuid 和 code 均非空，任一为空抛出异常 "验证码不能为空"
2. 构建 Redis Key：`captcha_codes:{uuid}`
3. 从 Redis 获取验证码：`stringRedisTemplate.opsForValue().get(key)`
4. 若获取值为 null，抛出异常 "验证码已过期"
5. 大小写不敏感比较：`!code.equalsIgnoreCase(captcha)` 则抛出异常 "验证码错误"
6. **无论校验成功与否**，从 Redis 删除 Key：`stringRedisTemplate.delete(key)`

---

## 6. 配置参数

### 6.1 验证码参数

| 参数 | 环境 | 值 | 说明 |
|------|------|-----|------|
| `captcha.enable` | dev | `false` | 开发环境关闭验证码 |
| `captcha.enable` | docker | `true` | 生产环境启用验证码 |
| `captcha.char-length` | 全局 | `4` | 验证码字符位数 |
| 验证码 TTL | 全局 | `300` 秒 | 硬编码于 `AuthService.generateCaptcha()` |

### 6.2 Sa-Token 参数

| 参数 | 值 | 说明 |
|------|-----|------|
| `sa-token.token-name` | `Authorization` | Token 在 HTTP Header 中的键名 |
| `sa-token.timeout` | `2592000` | Token 有效期（秒），即 30 天 |
| `sa-token.is-concurrent` | `true` | 允许多端同时登录 |
| `sa-token.token-style` | `tik` | Token 风格 |
| `sa-token.is-log` | `true` | 启用日志 |
| `sa-token.jwt-secret-key` | `abcdefghijklmnopqrstuvwxyz` | JWT 签名密钥（⚠️ 硬编码弱密钥） |

---

## 7. 已知问题与改进项

| # | 问题 | 严重度 | 来源 | 建议 |
|---|------|--------|------|------|
| I-AUTH-001 | JWT 签名密钥为硬编码弱密钥 `abcdefghijklmnopqrstuvwxyz` | **高** | `application.yml` | 改为环境变量注入或配置中心的强随机密钥（至少 256 位） |
| I-AUTH-002 | 登录失败区分"用户不存在"和"密码错误"，存在用户名枚举风险 | 中 | `AuthService.login()` | 统一为 "用户名或密码错误"（见 spec.md 待澄清事项） |
| I-AUTH-003 | 验证码校验失败后仍从 Redis 删除，用户需重新获取验证码 | 低 | `AuthService.validateCaptcha()` | 考虑失败时保留验证码（在 TTL 内允许重试），但需增加失败次数限制防止暴力破解 |
| I-AUTH-004 | 无登录失败次数限制，无账号锁定机制 | 中 | 缺失功能 | 新增登录失败计数（Redis），超过阈值临时锁定账号 |
| I-AUTH-005 | 无自动化测试代码 | 中 | 全局 | 添加 `AuthService` 单元测试和 `AuthController` 集成测试 |
| I-AUTH-006 | `SaTokenConfigure` 使用 `@Slf4j` 但 `AuthController` 和 `AuthService` 未直接使用 `@Slf4j` 日志注解（登录/注册日志在 Service 层通过 log.info 记录） | 低 | 代码审查 | 确认日志使用一致（实际 Service 中使用 `@Slf4j` 的 log 变量，Controller 使用 `System.out.println` 或类似） |

---

## 8. 测试策略

### 8.1 当前状态

**当前代码库未包含任何自动化测试代码**（无 `src/test/` 目录、无测试依赖配置）。

### 8.2 建议测试分层

| 测试类型 | 范围 | 关键场景 | 对应验收场景 |
|---------|------|---------|-------------|
| 单元测试 - AuthService | `register()`, `login()`, `validateCaptcha()`, `generateCaptcha()` | Mock Mapper 和 Redis，验证业务逻辑分支 | AC-03~19 |
| 单元测试 - CaptchaUtils | `generateCaptcha()` | 验证码格式 4 位、图片非空 | AC-01~02 |
| 集成测试 - AuthController | HTTP 请求/响应 | 验证完整请求链路（含 Sa-Token 拦截器） | AC-01~19 |
| 安全测试 | 验证码防重放、密码破解防护 | 同一验证码不可用两次、BCrypt 验证耗时合理 | — |

### 8.3 关键测试用例

1. **注册-事务回滚**：Mock `userProfileMapper.insert()` 抛异常，验证 `sysUserMapper.insert()` 未生效
2. **登录-停用用户拒绝**：Mock `UserView` 返回 `status="1"`，验证异常抛出
3. **验证码-一次性消费**：同一 uuid 调用两次 `validateCaptcha()`，第二次抛出 "验证码已过期"
4. **Token-登出后失效**：调用 `StpUtil.logout()` 后，`StpUtil.checkLogin()` 应抛异常

---

## 9. 依赖关系

### 9.1 本模块的 Maven 依赖

```xml
<!-- Sa-Token 认证框架（JWT + Redis 集成） -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot4-starter</artifactId>
    <version>1.45.0</version>
</dependency>
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-jwt</artifactId>
    <version>1.45.0</version>
</dependency>
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-redis-template</artifactId>
    <version>1.45.0</version>
</dependency>

<!-- MyBatis-Plus ORM -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
    <version>3.5.17</version>
</dependency>

<!-- Hutool 工具集 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.47</version>
</dependency>

<!-- Spring Boot Starter Web (内嵌 BCrypt) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Data Redis (Lettuce) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 9.2 模块间依赖

```
002-user-auth
  ├── 依赖: config/SaTokenConfigure.java（公共基础设施 — 鉴权配置）
  ├── 依赖: model/R.java（公共基础设施 — 统一响应体）
  ├── 依赖: PostgreSQL（sys_user、tpl_user_profile、tpl_user_view）
  ├── 依赖: Redis（验证码缓存、Token 会话）
  ├── 被依赖: 002-user-auth（通过 Token 获取用户身份）
```

---

## 10. 部署注意事项

### 10.1 环境差异

| 配置 | dev | docker |
|------|-----|--------|
| 验证码 | 关闭 (`captcha.enable=false`) | 启用 (`captcha.enable=true`) |
| Redis | `localhost:36379` | `redis:6379` |
| 数据库 | `localhost:35432` | `postgres:5432` |
| JWT 密钥 | 硬编码弱密钥 | 硬编码弱密钥（⚠️ 需更换） |

### 10.2 验证码开关

开发环境关闭验证码以提升效率，但需要注意：
- CI/CD 流水线中的集成测试应在 Docker profile 下运行，确保验证码功能可正常测试
- 部署生产前 MUST 确认 `captcha.enable=true`

### 10.3 数据库依赖

- 模块依赖 `tpl_user_view` 视图，该视图由 DBA 或 tpl-manage 工程创建/维护
- 若视图不可用，注册查重和登录查询将失败
- `sys_user` 表的主键策略为 `IdType.ASSIGN_ID`（雪花算法），不依赖数据库自增序列

---

## 11. 微信登录实现（social/xcx 策略）

> 详见父工程 [../../specs/002-user-auth/plan.md](../../specs/002-user-auth/plan.md) §9 与 `docs/002-用户注册及登录设计-微信登录集成.md`（§8.2）。

- 新增类（`org.fellow99.tpl.appapi`）：`config/properties/WechatProperties` + `SocialLoginConfigProperties`、`util/WechatAuthUtils`、`util/AuthRedisStateCache`、`entity/SysSocial`、`mapper/SysSocialMapper`、`service/SysSocialService`、`service/strategy/{IAuthStrategy,SocialAuthStrategy,XcxAuthStrategy}`、`model/dto/{SocialLoginBody,XcxLoginBody,SocialLoginResult}`、`controller/SysSocialController`。
- 改造：`AuthService.login()` 的 grantType if/else 改为 `IAuthStrategy.login(body, grantType)` Bean 分发（password/sms 兜底保留）；social/xcx 跳过图形验证码。
- 依赖：`io.github.windtool:JustAuth:3.0.1`；`sys_social` 表零 DDL。
- 编译开关：`wechat.enabled`（`@ConditionalOnProperty` 门控策略 Bean + 端点手动判断）。
