# 整体技术方案

> 项目：tpl-app-api（tpl-workspace后端 — 用户侧API）
> 对应规格：[overall-spec.md](./overall-spec.md)
> 最后更新：2026-08-12
> 说明：本文档为回溯性技术方案，描述系统**实际构建**的技术实现策略。

---

## 1. 技术上下文

### 1.1 运行时环境

| 项目 | 值 |
|------|-----|
| Java 版本 | 21 |
| 框架版本 | Spring Boot 4.1.0 |
| 构建工具 | Maven Wrapper |
| 嵌入式容器 | Tomcat（Spring Boot 默认） |
| 服务端口 | 8082 |
| 启动类 | `org.fellow99.tpl.appapi.TplAppApiApplication` |

### 1.2 核心依赖

| 依赖 | 版本 | 集成方式 |
|------|------|---------|
| Sa-Token | 1.45.0 | `sa-token-spring-boot4-starter` + `sa-token-jwt` + `sa-token-redis-template` |
| MyBatis-Plus | 3.5.17 | `mybatis-plus-spring-boot4-starter` |
| PostgreSQL | JDBC Driver | `spring-boot-starter-data-redis` 间接引入 |
| Redis (Lettuce) | Spring Boot 默认 | `spring-boot-starter-data-redis` |
| Hutool | 5.8.47 | `hutool-all` |

---

## 2. 宪法合规性检查

| 原则 | 合规状态 | 说明 |
|------|---------|------|
| 分层架构规范 | ✅ 合规 | Controller → Service → Mapper 严格分层 |
| 统一响应格式 | ✅ 合规 | 所有接口返回 `R<T>` |
| 异常处理策略 | ✅ 合规 | Service 抛 `IllegalArgumentException`，Controller 捕获 |
| 轻量 Token 认证 | ✅ 合规 | 仅登录校验，无 RBAC |
| 数据库交互规范 | ⚠️ 部分 | 自定义 SQL 用注解，无 XML（符合）；但 `mapper-locations` 配置了但未使用 |
| 配置管理 | ✅ 合规 | dev / docker 两个 Profile |
| 代码简化与风格 | ✅ 合规 | Lombok + 构造函数注入 |
| 数据库实体标识 | ✅ 合规 | `@TableId` 显式声明 |
| 验证码安全 | ✅ 合规 | Redis TTL + 一次性消费 |
| 数据库密码安全 | ✅ 合规 | BCrypt 哈希 |
| JWT 密钥安全 | ❌ 违规 | 密钥 `abcdefghijklmnopqrstuvwxyz` 为硬编码弱密钥 |

---

## 3. 实现策略概述

### 3.1 应用入口

- 启动类 `TplAppApiApplication` 使用 `@SpringBootApplication` 和 `@MapperScan("org.fellow99.tpl.appapi.mapper")`
- 无自定义 `CommandLineRunner` 或 `ApplicationRunner`

### 3.2 鉴权实现

**拦截器配置**（`SaTokenConfigure.java`）：
- 全局拦截所有路径 `/**`，默认要求登录
- 排除路径：`/auth/**`、`/error`、`/swagger-ui/**`、`/v3/api-docs/**`

**注解放行**：
- `/auth/code`、`/auth/register`、`/auth/login` 使用 `@SaIgnore` 注解标记无需登录

**Token 签发与校验**：
- 登录时调用 `StpUtil.login(userId)` 签发 Token
- 业务接口通过 `StpUtil.getLoginIdAsLong()` 获取当前用户 ID
- 登出时调用 `StpUtil.logout()` 使 Token 失效

### 3.3 异常处理模式

```java
// Controller 层模式
try {
    service.doSomething(req);
    return R.ok();
} catch (IllegalArgumentException e) {
    return R.fail(e.getMessage());
}
```

无全局异常处理器（`@ControllerAdvice`），每个 Controller 方法自行捕获。当前实现简单有效，但随着 Controller 增多可能需要抽取全局异常处理。

### 3.4 数据访问模式

- 所有 Mapper 继承 `BaseMapper<T>`，获得 `insert()`、`selectById()` 等通用方法
- 自定义 SQL 使用 `@Select` 注解：
  - `UserViewMapper.selectByUserName(String)` — 按用户名查询用户视图
- 无 XML Mapper 文件（`mapper-locations: classpath*:mapper/**/*.xml` 配置保留但未使用）

### 3.5 事务管理

- `AuthService.register()` 方法使用 `@Transactional(rollbackFor = Exception.class)`
- 确保 `SysUser` 和 `UserProfile` 同时写入成功或同时回滚

---

## 4. 横切关注点

### 4.1 CORS 跨域

- `CorsConfig.java`：允许所有来源、所有方法、所有 Header、凭证、预检缓存 1 小时
- 配置方式：`CorsFilter` Bean + `UrlBasedCorsConfigurationSource`

### 4.2 日志

- 使用 Lombok `@Slf4j` 注解
- 登录和注册成功时打印 `log.info()` 记录 userId 和 userName
- 开发环境日志级别：`org.fellow99.tpl.appapi: DEBUG`
- Docker 环境日志级别：`org.fellow99.tpl.appapi: INFO`

### 4.3 JSON 序列化

- 日期格式全局配置：`yyyy-MM-dd HH:mm:ss`
- 时区：`GMT+8`
- `write-dates-as-timestamps: false`（禁用时间戳格式）

### 4.4 逻辑删除

- MyBatis-Plus 全局配置：
  - 逻辑删除字段：`delFlag`
  - 已删除值：`"1"`
  - 未删除值：`"0"`
- 所有查询自动附加 `del_flag = '0'` 条件

---

## 5. 测试策略

当前代码库**未包含任何自动化测试代码**（无 `src/test/` 目录、无测试依赖配置）。

**建议测试分层**：

| 测试类型 | 范围 | 优先级 |
|---------|------|--------|
| 单元测试 | Service 层（AuthService） | 高 |
| 集成测试 | Controller 层（HTTP 请求/响应） | 中 |
| 安全测试 | Token 过期、验证码防重放、SQL 注入 | 高 |

---

## 6. 部署策略

### 6.1 构建

```bash
./mvnw clean package -DskipTests
```
输出：`target/tpl-app-api-1.0.0-SNAPSHOT.jar`

### 6.2 Docker 部署

通过 `application-docker.yml` Profile 运行，数据库和 Redis 通过 Docker 服务名连接：
```bash
java -jar tpl-app-api.jar --spring.profiles.active=docker
```

### 6.3 开发环境

通过 `application-dev.yml` Profile 运行（默认），数据库和 Redis 连接本地映射端口。

---

## 7. 已知问题与改进项

| # | 问题 | 严重度 | 建议 |
|---|------|--------|------|
| I-001 | JWT 密钥硬编码为弱密钥 | 高 | 改为环境变量或配置中心的强随机密钥 |
| I-002 | 无全局异常处理器 | 低 | 使用 `@ControllerAdvice` 统一处理 `IllegalArgumentException` |
| I-003 | 无自动化测试 | 中 | 添加 JUnit 5 + Mockito 测试 |
| I-004 | `mapper-locations` 配置冗余 | 低 | 无 XML 文件时移除该配置 |
| I-005 | 开发环境验证码关闭 | 低 | 确保 Docker/生产环境 `captcha.enable=true` |
