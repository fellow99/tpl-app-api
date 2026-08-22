# 项目目录结构

> 项目：tpl-app-api（tpl-workspace 后端 — 用户侧 API）
> 生成日期：2026-08-22
> 源码根目录：`src/main/java/org/fellow99/tpl/appapi/`

---

## 一、顶层目录结构

```
tpl-app-api/
├── .git/                     # Git 版本控制
├── .gitattributes            # Git 属性配置
├── .gitignore                # Git 忽略规则
├── mvnw                      # Maven Wrapper (Unix)
├── mvnw.cmd                  # Maven Wrapper (Windows)
├── pom.xml                   # Maven 项目配置（Spring Boot 4.1.0 + Java 21）
├── README.md                 # 项目说明
├── specs/                    # 规范文档目录
│   ├── README.md
│   ├── SPECS_CHECKLIST.md
│   ├── STRUCTURE.md
│   ├── TECH.md
│   ├── ARCHITECTURE.md
│   ├── constitution.md
│   ├── overall-spec.md
│   ├── overall-plan.md
│   ├── overall-data-model.md
│   ├── overall-api.md
│   ├── overall-test-cases.md
│   ├── 002-user-auth/        # 用户认证模块规范文档
│   ├── 004-i18n/             # 国际化模块规范文档
│   └── 202-sms-integration/  # 短信平台对接模块规范文档
├── src/                      # 源代码目录
│   └── main/
│       ├── java/org/fellow99/tpl/appapi/
│       │   ├── TplAppApiApplication.java          # Spring Boot 启动入口
│       │   ├── config/                            # 配置类
│       │   │   ├── CorsConfig.java                # CORS 跨域配置
│       │   │   └── SaTokenConfigure.java          # Sa-Token 鉴权拦截器配置
│       │   ├── controller/                        # REST 控制器
│       │   │   ├── AuthController.java            # 认证接口（注册/登录/登出/验证码）
│       │   │   └── SystemUserController.java      # 用户信息接口
│       │   ├── service/                           # 业务逻辑层
│       │   │   ├── AuthService.java               # 认证业务逻辑
│       │   │   └── UserService.java               # 用户业务逻辑
│       │   ├── mapper/                            # MyBatis-Plus 数据访问层
│       │   │   ├── SysUserMapper.java             # 系统用户表
│       │   │   ├── UserProfileMapper.java         # 用户档案表
│       │   │   └── UserViewMapper.java            # 用户视图（含自定义SQL）
│       │   ├── entity/                            # 数据库实体
│       │   │   ├── SysUser.java                   # 系统用户
│       │   │   ├── UserProfile.java               # 用户档案
│       │   │   └── UserView.java                  # 用户视图
│       │   ├── model/                             # 通用模型
│       │   │   ├── R.java                         # 统一响应包装类
│       │   │   └── dto/                           # 数据传输对象
│       │   │       ├── LoginRequest.java          # 登录请求参数
│       │   │       ├── LoginResponse.java         # 登录响应（含 token）
│       │   │       └── RegisterRequest.java       # 注册请求参数
│       │   └── util/                              # 工具类
│       │       └── CaptchaUtils.java              # 图形验证码生成工具
│       └── resources/                             # 配置文件
│           ├── application.yml                    # 主配置（端口/MyBatis-Plus/Sa-Token/Captcha）
│           ├── application-dev.yml                # 开发环境配置（本地DB/Redis/日志）
│           └── application-docker.yml             # Docker 环境配置
└── target/                   # Maven 编译输出（gitignore）
```

---

## 二、功能模块划分

| 编号 | 模块名 | 包路径 | 职责 |
|------|--------|--------|------|
| 000 | 公共基础设施 | `config/`, `model/` | CORS 配置、鉴权拦截器、统一响应体 |
| 002 | 用户认证 | `controller/AuthController`, `service/AuthService`, `util/CaptchaUtils` | 注册、登录、登出、图形验证码 |

---

## 三、API 路由清单

| Path | Method | 鉴权 | 用途 | 入口文件 |
|------|--------|------|------|----------|
| `/auth/code` | GET | 无需登录 | 获取图形验证码（Base64 图片） | `AuthController.java` |
| `/auth/register` | POST | 无需登录 | 用户注册（含验证码校验） | `AuthController.java` |
| `/auth/login` | POST | 无需登录 | 用户登录（返回 JWT Token） | `AuthController.java` |
| `/auth/logout` | POST | 需登录 | 用户登出 | `AuthController.java` |
| `/system/user/getInfo` | GET | 需登录 | 查询用户信息 | `SystemUserController.java` |

---

## 四、数据库表映射

| 表名 | 实体类 | Mapper | 说明 |
|------|--------|--------|------|
| `sys_user` | `SysUser.java` | `SysUserMapper.java` | 系统用户表（ID、用户名、密码、状态等） |
| `tpl_user_profile` | `UserProfile.java` | `UserProfileMapper.java` | 用户档案表（生日等） |
| `tpl_user_view` | `UserView.java` | `UserViewMapper.java` | 用户信息视图（sys_user + tpl_user_profile 联合） |

---

## 五、关联工程

### tpl-app-web（用户端前端）

| 项目 | 路径 | 技术栈 | 规范文档 |
|------|------|--------|---------|
| tpl-app-web | `..\tpl-app-web\` | Vue 3.5 + Vite 6 + TS 5.6 + Pinia 2 | [README](../tpl-app-web/README.md) / [specs](../tpl-app-web/specs/README.md) |

**页面-API 对应关系**：

| 前端页面 | 路由 | 调用的后端 API | 前端模块 |
|---------|------|---------------|---------|
| 登录页 | `/login` | `POST /auth/login`、`GET /auth/code` | 002-user-auth |
| 注册页 | `/register` | `POST /auth/register`、`GET /auth/code` | 002-user-auth |
| 个人中心 | `/profile` | `GET /system/user/getInfo` | 002-user-auth |

### tpl-manage / tpl-manage-ui（管理后台）

| 工程 | 路径 | 关系 |
|------|------|------|
| `tpl-manage` | `..\tpl-manage\` | 管理后台后端（RuoYi-Vue-Plus），共享数据库 |
| `tpl-manage-ui` | `..\tpl-manage-ui\` | 管理后台前端（Vue 3 + Element Plus） |
