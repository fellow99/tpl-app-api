# 对外接口模型

> 项目：tpl-app-api（tpl-workspace 后端 — 用户侧 API）
> 生成日期：2026-08-22
> 基础路径：`http://host:8082`
> API 消费方：`tpl-app-web`（Vue 3 前端 SPA，`..\tpl-app-web\`）
> 前端 API 文档：[../tpl-app-web/specs/API.md](../tpl-app-web/specs/API.md)（HTTP 客户端、拦截器、代理映射）
> 前端整体 API 规格：[../tpl-app-web/specs/overall-api.md](../tpl-app-web/specs/overall-api.md)

---

## 一、接口总览

| # | Path | Method | 鉴权 | 用途 |
|---|------|--------|------|------|
| 1 | `/auth/code` | GET | 否 | 获取图形验证码 |
| 2 | `/auth/register` | POST | 否 | 用户注册 |
| 3 | `/auth/login` | POST | 否 | 用户登录 |
| 4 | `/auth/logout` | POST | 是 | 用户登出 |
| 5 | `/system/user/getInfo` | GET | 是 | 查询用户信息 |

---

## 二、接口详细定义

### 2.1 获取图形验证码

```
GET /auth/code
```

**请求参数**：无

**响应示例**（验证码启用时）：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "captchaEnabled": true,
    "uuid": "a1b2c3d4e5f6...",
    "img": "data:image/png;base64,iVBORw0KGgo..."
  }
}
```

**响应示例**（验证码关闭时）：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "captchaEnabled": false,
    "uuid": "",
    "img": ""
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `captchaEnabled` | `boolean` | 验证码是否启用 |
| `uuid` | `String` | 验证码唯一标识（用于后续注册/登录） |
| `img` | `String` | Base64 编码的 PNG 验证码图片 |

**说明**：
- 验证码有效期为 5 分钟
- 每个验证码仅可使用一次
- 开发环境（dev profile）默认关闭验证码

---

### 2.2 用户注册

```
POST /auth/register
Content-Type: application/json
```

**请求体**：
```json
{
  "username": "user001",
  "password": "mypassword123",
  "phoneNumber": "13800138000",
  "code": "A3B9",
  "uuid": "a1b2c3d4e5f6..."
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | `String` | 是 | 用户名（唯一） |
| `password` | `String` | 是 | 明文密码（服务端 BCrypt 加密存储） |
| `phoneNumber` | `String` | 否 | 手机号 |
| `code` | `String` | 条件必填 | 图形验证码（captcha.enable=true 时必填） |
| `uuid` | `String` | 条件必填 | 验证码唯一标识（captcha.enable=true 时必填） |

**成功响应**（200）：
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

**错误响应**（500）：
```json
{
  "code": 500,
  "msg": "用户名已存在",
  "data": null
}
```

| 错误消息 | 触发条件 |
|---------|---------|
| "验证码不能为空" | uuid 或 code 为空 |
| "验证码已过期" | Redis 中无对应验证码 |
| "验证码错误" | 验证码不匹配 |
| "用户名已存在" | username 在 tpl_user_view 中已存在 |

**说明**：
- 注册操作在事务中执行，同时写入 `sys_user` 和 `tpl_user_profile` 两张表
- 昵称（nickName）默认与用户名相同
- 密码使用 BCrypt 哈希后存储

---

### 2.3 用户登录

```
POST /auth/login
Content-Type: application/json
```

**请求体**：
```json
{
  "username": "user001",
  "password": "mypassword123",
  "code": "A3B9",
  "uuid": "a1b2c3d4e5f6..."
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | `String` | 是 | 用户名 |
| `password` | `String` | 是 | 明文密码 |
| `code` | `String` | 条件必填 | 验证码（captcha.enable=true 时） |
| `uuid` | `String` | 条件必填 | 验证码唯一标识（captcha.enable=true 时） |

**成功响应**（200）：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiJ9...",
    "token_type": "Bearer"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `access_token` | `String` | JWT 访问令牌 |
| `token_type` | `String` | 令牌类型，固定 "Bearer" |

**错误响应**：

| 错误消息 | 触发条件 |
|---------|---------|
| "用户名和密码不能为空" | username 或 password 为空 |
| "用户不存在" | username 在 tpl_user_view 中未找到 |
| "密码错误" | BCrypt 比对失败 |
| "用户已被停用" | sys_user.status = '1' |
| "验证码不能为空" / "验证码已过期" / "验证码错误" | 验证码校验失败 |

**说明**：
- Token 有效期：2592000 秒（30 天）
- Token 使用方式：请求头 `Authorization: Bearer <access_token>`
- Token 策略：允许同账号多端同时登录（`is-concurrent: true`）

---

### 2.4 用户登出

```
POST /auth/logout
Authorization: Bearer <access_token>
```

**请求参数**：无

**成功响应**（200）：
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

**说明**：
- 需要携带有效 Token
- 登出后 Token 立即失效

---

### 2.5 查询用户信息

```
GET /system/user/getInfo
Authorization: Bearer <access_token>
```

**请求参数**：无

**成功响应**（200）：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "userId": 1,
    "userName": "user001",
    "nickName": "user001",
    "phoneNumber": "13800138000",
    "email": "a@b.com",
    "birthDate": "2010-01-01"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `Long` | 用户 ID |
| `userName` | `String` | 用户名 |
| `nickName` | `String` | 昵称 |
| `phoneNumber` | `String` | 手机号 |
| `email` | `String` | 邮箱 |
| `birthDate` | `LocalDate` | 生日 |

**说明**：
- 需要携带有效 Token
- 用户身份从 Token 中自动获取
- 不含 `password` 字段

---

## 三、通用规范

### 3.1 认证方式

- 认证 Token 通过 HTTP Header `Authorization` 传递
- 格式：`Bearer <access_token>`
- 登录后的所有接口（除 `/auth/**` 路径外）均需携带

### 3.2 响应格式

所有接口统一使用以下 JSON 格式：

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

| code | 含义 |
|------|------|
| 200 | 请求成功 |
| 500 | 业务异常（msg 字段包含中文错误描述） |

### 3.3 错误处理

- 业务层异常：返回 HTTP 200 + `code: 500` + 中文 msg
- 框架层异常（如 404、405）：由 Spring Boot 默认处理，返回标准 HTTP 错误状态码
- 鉴权失败（未携带 Token 或 Token 无效）：由 Sa-Token 拦截器处理
