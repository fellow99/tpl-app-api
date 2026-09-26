# tpl-app-api

> **tpl-workspace** 用户侧核心业务 API — 基于 Spring Boot 4.1.0 构建

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen)](./pom.xml)
[![JDK](https://img.shields.io/badge/JDK-21-orange)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-336791?logo=postgresql)](./)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis)](./)
[![License](https://img.shields.io/badge/license-MIT-blue)](./LICENSE)

---

## 一、项目简介

tpl-app-api 是「tpl-workspace」产品的**用户侧核心业务 API 服务**（用户端），基于 Spring Boot 4.1.0 构建，为前端应用（tpl-app-web）提供 RESTful API。采用经典三层架构（Controller → Service → Mapper），参考 RuoYi-Vue-Plus 技术框架，实现轻量级 Token 认证（非 RBAC）和核心业务查询功能。

### 产品关系

```
tpl-workspace 产品线
├── tpl-app-api      ← 核心业务服务（本项目：用户端 API，端口 8082）
├── tpl-app-web      ← 用户端 Web 应用（Vue 3 + TypeScript）
├── tpl-manage       ← 管理后台服务端（基于 RuoYi-Vue-Plus，端口 8081）
├── tpl-manage-ui    ← 管理后台前端（Vue 3 + Element Plus）
```

### 核心定位

- **用户认证：** 用户名+密码注册与登录，JWT Token 签发与登出，图形验证码

### 当前实现范围（MVP）

| 模块 | 编号 | 端点 | 状态 |
|------|------|------|:--:|
| 用户认证 | 002-user-auth | `POST /auth/register`、`POST /auth/login`、`POST /auth/logout`、`GET /auth/code` | ✅ 已实现 |
| 短信平台对接 | 202-sms-integration | 内部功能类 `SmsService`（无对外端点） | ✅ 已实现 |

---

## 二、技术架构

### 2.1 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **后端框架** | Spring Boot | 4.1.0 | REST API 容器，嵌入式 Tomcat |
| **JDK** | Java | 21 | LTS 版本 |
| **认证授权** | Sa-Token | 1.45.0 | JWT 模式 + Redis 会话存储 |
| **ORM** | MyBatis-Plus | 3.5.17 | BaseMapper 通用 CRUD + 自定义注解 SQL |
| **数据库** | PostgreSQL | 15+ | 与 tpl-manage 共享数据库 `tpl_manage` |
| **缓存** | Redis | 7.x (Lettuce) | 验证码临时存储、Token 会话 |
| **工具库** | Hutool | 5.8.47 | 字符串处理、随机数生成 |
| **简化代码** | Lombok | — | @Data、@RequiredArgsConstructor |
| **构建工具** | Maven | Wrapper | 依赖管理与构建 |
| **前端（关联）** | Vue 3 + TypeScript + Vite | — | tpl-app-web 独立工程 |

### 2.2 架构设计

```
┌──────────────┐     HTTP (JSON)
│  tpl-app-web  │     Authorization: Bearer <token>
│  (Vue 3)     │──────────────────────────────┐
└──────────────┘                              │
                                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     tpl-app-api :8082                         │
│                                                              │
│  ┌─ 拦截器层 ──────────────────────────────────────────────┐ │
│  │  CorsFilter (跨域)  │  SaInterceptor (鉴权)              │ │
│  │  允许所有来源         │  全局登录校验，/auth/** 放行      │ │
│  └───────────────────────┬─────────────────────────────────┘ │
│                          ▼                                    │
│  ┌─ Controller ────────────────────────────────────────────┐ │
│  │  AuthController (/auth/*)  │  ProfileController   │ │
│  │  register/login/logout/code│  /api/profile/*     │ │
│  └───────────────────────┬─────────────────────────────────┘ │
│                          ▼                                    │
│  ┌─ Service ───────────────────────────────────────────────┐ │
│  │  AuthService              │  ProfileService       │ │
│  └───────────────────────┬─────────────────────────────────┘ │
│                          ▼                                    │
│  ┌─ Mapper ────────────────────────────────────────────────┐ │
│  │  MyBatis-Plus BaseMapper + @Select 自定义 SQL           │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
         │                          │
         ▼                          ▼
   ┌──────────┐              ┌──────────┐
   │PostgreSQL│              │  Redis   │
   │tpl_manage │              │ 验证码    │
   └──────────┘              └──────────┘
```

### 2.3 与 RuoYi-Vue-Plus 的关系

**参考但不依赖**：tpl-app-api 参考 RuoYi-Vue-Plus 的技术框架选型（Sa-Token + MyBatis-Plus + PostgreSQL），但作为独立 Spring Boot 工程构建，不引入 RuoYi 模块依赖。

| 维度 | RuoYi-Vue-Plus (tpl-manage) | tpl-app-api（本项目） |
|------|---------------------------|---------------------|
| 构建方式 | Maven 聚合 RVP 模块 | 独立 Maven 工程 |
| 认证模型 | Sa-Token + 完整 RBAC | Sa-Token JWT（仅登录校验） |
| 端口 | 8081 | **8082** |
| 包命名 | `org.fellow99.tpl.manage` | `org.fellow99.tpl.appapi` |
| 用户表 | `sys_user` + RBAC 扩展 | `sys_user` + `tpl_user_profile` 扩展 |
| 权限控制 | 角色-菜单-按钮三级 | 仅登录态/非登录态 |
| 验证码 | 图形 + 短信 | 图形验证码（支持开关） |

---

## 三、快速开始

### 3.1 环境要求

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 21 | LTS 版本 |
| Maven | 3.9+ | 或使用 `mvnw` Wrapper |
| PostgreSQL | 15+ | 数据库 `tpl_manage`（由 tpl-manage 初始化） |
| Redis | 7.x | 验证码存储 + Session |

### 3.2 数据库

本工程依赖 tpl-manage 初始化的数据库，无需独立建表脚本。业务扩展表（`tpl_user_profile` 等）由 tpl-manage 的 SQL 脚本创建。

### 3.3 启动服务

```bash
# 在 tpl-app-api 根目录执行
./mvnw spring-boot:run

# 或编译后运行
./mvnw clean package -DskipTests
java -jar target/tpl-app-api-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

服务启动后：
- **API 服务：** http://localhost:8082
- **验证码接口：** `GET /auth/code`
- **登录接口：** `POST /auth/login` — `{"username":"user001","password":"xxx","code":"...","uuid":"..."}`

### 3.4 环境配置

| Profile | 文件 | 数据库 | Redis | 验证码 | 日志 |
|---------|------|--------|-------|:--:|------|
| dev（默认） | `application-dev.yml` | `localhost:35432` | `localhost:36379` | 关闭 | DEBUG |
| docker | `application-docker.yml` | `postgres:5432` | `redis:6379` | 开启 | INFO |

关键配置项：
```yaml
# application.yml
server.port: 8082
sa-token.token-name: Authorization
sa-token.timeout: 2592000          # Token 有效期 30 天
sa-token.jwt-secret-key: abc...     # ⚠️ 生产环境需更换为强随机密钥
captcha.enable: true                # dev 覆盖为 false
```

---

## 四、项目结构

```
tpl-app-api/
├── pom.xml                                    # Maven 项目配置
├── README.md                                  # 本文件
├── mvnw / mvnw.cmd                            # Maven Wrapper
└── src/main/
    ├── java/org/fellow99/tpl/appapi/
    │   ├── TplAppApiApplication.java           # Spring Boot 启动入口
    │   ├── config/
    │   │   ├── CorsConfig.java                # CORS 跨域配置
    │   │   ├── SaTokenConfigure.java          # Sa-Token 拦截器配置
    │   │   └── SmsProperties.java             # 短信平台配置（sms.*）
    │   ├── controller/
    │   │   ├── AuthController.java            # 认证接口（/auth/*）
    │   │   └── ProfileController.java  # 个人中心接口
    │   ├── service/
    │   │   ├── AuthService.java               # 认证业务逻辑
    │   │   ├── ProfileService.java     # 个人中心业务逻辑
    │   │   └── SmsService.java                # 短信验证码发送封装
    │   ├── mapper/                            # MyBatis-Plus 数据访问层（5 个接口）
    │   ├── entity/                            # 数据库实体（5 个实体）
    │   ├── model/
    │   │   ├── R.java                         # 统一响应包装 {code, msg, data}
    │   │   └── dto/                           # 数据传输对象（含 SmsSendResult）
    │   └── util/
    │       └── CaptchaUtils.java              # 图形验证码生成工具
    └── resources/
        ├── application.yml                    # 主配置
        ├── application-dev.yml                # 开发环境
        └── application-docker.yml             # Docker 环境
```

---

## 五、API 接口

### 5.1 公开接口（无需 Token）

| 方法 | 路径 | 说明 | 请求体 |
|------|------|------|--------|
| GET | `/auth/code` | 获取图形验证码（Base64 PNG） | — |
| POST | `/auth/login` | 用户登录（返回 JWT Token） | `{username, password, code, uuid}` |

### 5.2 需 Token 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/logout` | 用户登出（Token 作废） |

### 5.3 统一响应格式

```json
{
  "code": 200,        // 200=成功, 500=业务异常
  "msg": "success",   // 提示信息
  "data": { ... }     // 业务数据（泛型）
}
```

完整 API 清单与契约见：[specs/overall-api.md](./specs/overall-api.md)

---

## 六、数据库表使用

### 6.1 读写的表

| 表 | 用途 | 操作 |
|------|------|:--:|
| `sys_user` | 用户核心信息（注册时写入） | 读写 |
| `tpl_user_profile` | 用户扩展信息（生日） | 读写 |
| `tpl_user_view` | 用户信息视图（sys_user + tpl_user_profile 联表，登录时查询） | 只读 |

### 6.2 预留表

| 表 | 用途 | 状态 |
|------|------|:--:|

---

## 七、文档导航

### 7.1 产品设计文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 产品概念设计 | `../docs/产品概念设计.md` | 产品整体规划、核心功能 |
| 技术选型 | `../docs/技术选型.md` | 整体技术栈决策 |
| 数据模型设计 | `../docs/数据模型设计.md` | 数据库详细设计 |
| 用户注册及登录设计 | `../docs/002-用户注册及登录设计.md` | 用户模块详细设计 |
| 个人中心设计 | `../docs/101-个人中心.md` | 个人中心页面与 API 设计 |

### 7.2 工程规格文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 文档索引 | [specs/README.md](./specs/README.md) | 全部规范文档导航 |
| 架构设计 | [specs/ARCHITECTURE.md](./specs/ARCHITECTURE.md) | 系统分层架构、数据流、部署拓扑 |
| 技术选型 | [specs/TECH.md](./specs/TECH.md) | 12 项技术栈版本与选型理由 |
| 宪法原则 | [specs/constitution.md](./specs/constitution.md) | 10 条编码规范与架构约束 |
| 项目结构 | [specs/STRUCTURE.md](./specs/STRUCTURE.md) | 源码目录树、API 路由、数据表映射 |
| 整体规格 | [specs/overall-spec.md](./specs/overall-spec.md) | 5 个用户故事、18 条功能需求 |
| 整体方案 | [specs/overall-plan.md](./specs/overall-plan.md) | 技术实现方案、5 个改进项 |
| 数据模型 | [specs/overall-data-model.md](./specs/overall-data-model.md) | 实体 ER 图、DTO、验证规则 |
| API 接口 | [specs/overall-api.md](./specs/overall-api.md) | 5 个 API 端点完整契约 |
| 测试用例 | [specs/overall-test-cases.md](./specs/overall-test-cases.md) | 测试策略与覆盖说明 |
| 模块规格-认证 | [specs/002-user-auth/spec.md](./specs/002-user-auth/spec.md) | 注册/登录/登出/验证码功能规格 |
| 模块方案-认证 | [specs/002-user-auth/plan.md](./specs/002-user-auth/plan.md) | 认证模块技术实现分析 |
| 模块方案-个人中心 | [specs/101-profile/plan.md](./specs/101-profile/plan.md) | 个人中心技术实现分析 |
| 模块规格-短信对接 | [specs/202-sms-integration/spec.md](./specs/202-sms-integration/spec.md) | 短信验证码发送功能规格 |
| 模块方案-短信对接 | [specs/202-sms-integration/plan.md](./specs/202-sms-integration/plan.md) | 短信发送封装技术实现分析 |

### 7.3 关联工程

| 工程 | 路径 | 说明 |
|------|------|------|
| **tpl-app-web** | `../tpl-app-web/` | **用户端 Web 前端工程**（本 API 的唯一消费方），Vue 3.5 + Vite 6 |
| tpl-manage | `../tpl-manage/` | 管理后台服务端（RuoYi-Vue-Plus），共享 `tpl_manage` 数据库 |
| tpl-manage-ui | `../tpl-manage-ui/` | 管理后台前端（Vue 3 + Element Plus） |
| 产品文档 | `../docs/` | 产品概念设计、模块设计文档（002-用户注册、101-个人中心 等） |

**tpl-app-web 规范文档关键索引**：

| 文档 | 路径 | 说明 |
|------|------|------|
| 前端工程 README | [../tpl-app-web/README.md](../tpl-app-web/README.md) | 工程说明、技术栈、功能模块 |
| 前端规范文档索引 | [../tpl-app-web/specs/README.md](../tpl-app-web/specs/README.md) | 全部 20 份前端规范文档导航 |
| 前端 API 清单 | [../tpl-app-web/specs/API.md](../tpl-app-web/specs/API.md) | HTTP 客户端、拦截器、API 代理映射 |
| 前端整体规格 | [../tpl-app-web/specs/overall-spec.md](../tpl-app-web/specs/overall-spec.md) | 前端系统级功能需求 |
| 002-用户认证（前端） | [../tpl-app-web/specs/002-user-auth/](../tpl-app-web/specs/002-user-auth/) | spec + plan + test-cases（31 条测试用例） |
| 101-个人中心（前端） | [../tpl-app-web/specs/101-profile/](../tpl-app-web/specs/101-profile/) | spec + plan + test-cases（19 条测试用例） |

---

## 八、已知问题与改进项

| # | 问题 | 严重度 | 建议 |
|---|------|:--:|------|
| I-001 | JWT 密钥为硬编码弱密钥 `abcdefghijklmnopqrstuvwxyz` | 高 | 改为环境变量或配置中心注入强随机密钥 |
| I-002 | 无全局异常处理器 | 低 | 使用 `@ControllerAdvice` 统一处理 `IllegalArgumentException` |
| I-003 | 无自动化测试（无 `src/test/` 目录） | 中 | 添加 JUnit 5 + Mockito 单元测试与集成测试 |
| I-004 | 验证码开发环境关闭 | 低 | 确保生产（docker）环境 `captcha.enable=true` |

---

## 九、参考

- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/documentation.html)
- [Sa-Token 文档](https://sa-token.cc)
- [MyBatis-Plus 文档](https://baomidou.com)
- [RuoYi-Vue-Plus 文档](https://plus-doc.dromara.org)
- [PostgreSQL 文档](https://www.postgresql.org/docs/)

---

## License

本项目基于 [MIT License](./LICENSE) 开源。
