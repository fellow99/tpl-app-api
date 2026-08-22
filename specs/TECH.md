# 技术选型

> 项目：tpl-app-api（tpl-workspace后端 — 用户侧API）
> 生成日期：2026-08-12

---

## 一、技术栈总览

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **语言** | Java | 21 | 主力开发语言 |
| **框架** | Spring Boot | 4.1.0 | 应用框架，提供 IoC、Web、自动配置 |
| **构建工具** | Maven | Wrapper | 依赖管理与项目构建 |
| **Web 层** | Spring Boot Starter Web | (继承自 Parent) | REST API 开发（基于嵌入式 Tomcat） |
| **ORM** | MyBatis-Plus | 3.5.17 | 数据访问层，提供通用 CRUD、代码生成 |
| **认证鉴权** | Sa-Token | 1.45.0 | 轻量级权限认证框架，支持 JWT + Redis |
| **数据库** | PostgreSQL | (JDBC Driver) | 关系型数据库 |
| **缓存** | Redis | (Lettuce 客户端) | 缓存（验证码存储、Token 会话） |
| **工具库** | Hutool | 5.8.47 | Java 通用工具集（字符串、随机数、加密等） |
| **简化代码** | Lombok | (optional) | 减少样板代码（@Data, @RequiredArgsConstructor 等） |
| **连接池** | HikariCP | (Spring Boot 默认) | 数据库连接池 |
| **Redis 连接池** | Commons Pool2 | (latest) | Redis 连接池支持 |

---

## 二、技术选型理由

### 2.1 Spring Boot 4.1.0

- **理由**：业界标准 Java 微服务框架，生态成熟，自动配置减少样板代码
- **场景**：提供 Web 容器、依赖注入、配置管理

### 2.2 MyBatis-Plus 3.5.17

- **理由**：在 MyBatis 基础上增强，提供通用 Mapper（`BaseMapper<T>`）、逻辑删除、分页等开箱即用功能
- **参考**：参考 RuoYi-Vue-Plus 的技术框架选型
- **场景**：数据持久层，所有数据库表操作均通过 `BaseMapper` 接口实现

### 2.3 Sa-Token 1.45.0

- **理由**：比 Spring Security 更轻量，API 简洁（`StpUtil.login()`），内置 JWT 和 Redis 集成
- **参考**：参考 RuoYi-Vue-Plus 的认证方案，但简化了 RBAC 权限模型
- **场景**：用户登录态管理、Token 签发与校验、接口鉴权拦截

### 2.4 PostgreSQL

- **理由**：与 tpl-manage（管理后台）共享数据库 `tpl_manage`，保持数据一致性

### 2.5 Redis

- **理由**：验证码临时存储（5 分钟 TTL）、Sa-Token 会话共享
- **场景**：高频读写、临时状态存储

### 2.6 Hutool 5.8.47

- **理由**：Java 开发中常用的工具集，减少重复造轮子
- **使用位置**：`StrUtil.isBlank()`（字符串判空）、`RandomUtil.randomString()`（验证码字符生成）

---

## 三、运行时环境

| 配置项 | 开发环境 | Docker 环境 |
|--------|---------|------------|
| 服务端口 | 8082 | 8082 |
| 数据库地址 | `localhost:35432` | `postgres:5432` |
| Redis 地址 | `localhost:36379` | `redis:6379` |
| 验证码开关 | `false`（开发关闭） | `true`（生产启用） |
| 日志级别 | `DEBUG` | `INFO` |
| 当前激活 Profile | `dev` | 由 `SPRING_PROFILES_ACTIVE=docker` 指定 |

---

## 四、关键配置参数

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `sa-token.token-name` | `Authorization` | Token 在 HTTP Header 中的 Key |
| `sa-token.timeout` | `2592000` (30天) | Token 有效期（秒） |
| `sa-token.token-style` | `tik` | Token 风格（UUID 风格） |
| `sa-token.jwt-secret-key` | `abcdefghijklmnopqrstuvwxyz` | JWT 签名密钥（**生产需更换**） |
| `captcha.char-length` | `4` | 验证码字符位数 |
| `mybatis-plus.logic-delete-field` | `delFlag` | 逻辑删除字段名 |

---

## 五、依赖关系图

```
tpl-app-api
├── Spring Boot Web          (REST API 容器)
├── Sa-Token                 (认证鉴权)
│   ├── sa-token-jwt         (JWT 模式)
│   └── sa-token-redis-template (Redis 会话存储)
├── MyBatis-Plus             (ORM)
│   └── PostgreSQL JDBC      (数据库驱动)
├── Redis (Lettuce)          (缓存)
│   └── Commons Pool2        (连接池)
├── Hutool                   (工具集)
└── Lombok                   (编译时代码生成)
```
