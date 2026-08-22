# 数据模型

> 项目：tpl-app-api（tpl-workspace 后端 — 用户侧 API）
> 生成日期：2026-08-22
> 数据库：PostgreSQL，Schema：tpl_manage（与 tpl-manage 共享）

---

## 一、实体关系图

```
┌─────────────┐        ┌──────────────────┐
│   sys_user  │        │  tpl_user_profile │
├─────────────┤        ├──────────────────┤
│ user_id (PK)│◄───────│ user_id (FK)     │
│ user_name   │   1:1  │ birth_date       │
│ nick_name   │        └──────────────────┘
│ password    │
│ phone_number│
│ status      │
│ del_flag    │
└──────┬──────┘
       │
       ▼
┌──────────────────────┐
│    tpl_user_view      │  (数据库视图，只读)
├──────────────────────┤
│ user_id              │
│ user_name            │
│ password             │
│ status               │
│ del_flag             │
│ birth_date           │
│ ...                  │
└──────────────────────┘
```

---

## 二、实体定义

### 2.1 SysUser — 系统用户

| 属性 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `userId` | `Long` | PK, 雪花算法生成 | 用户唯一标识 |
| `userName` | `String` | NOT NULL, UNIQUE | 用户名 |
| `nickName` | `String` | | 昵称（默认与用户名相同） |
| `phoneNumber` | `String` | | 手机号（映射至 `phone_number` 列） |
| `password` | `String` | NOT NULL | BCrypt 加密后的密码 |
| `avatar` | `Long` | | 头像（文件 ID） |
| `status` | `String` | '0'=正常, '1'=停用 | 账号状态 |
| `delFlag` | `String` | '0'=正常, '1'=已删除 | 逻辑删除标记 |
| `loginIp` | `String` | | 最后登录 IP |
| `loginDate` | `LocalDateTime` | | 最后登录时间 |
| `createTime` | `LocalDateTime` | | 创建时间 |
| `createBy` | `Long` | | 创建人 |
| `updateBy` | `Long` | | 更新人 |
| `updateTime` | `LocalDateTime` | | 更新时间 |
| `remark` | `String` | | 备注 |

**表名**：`sys_user`

---

### 2.2 UserProfile — 用户档案

| 属性 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `profileId` | `Long` | PK, 自增 | 档案唯一标识 |
| `userId` | `Long` | FK → sys_user.user_id | 关联用户 |
| `birthDate` | `LocalDate` | | 出生日期 |
| `createTime` | `LocalDateTime` | | 创建时间 |
| `updateTime` | `LocalDateTime` | | 更新时间 |

**表名**：`tpl_user_profile`

---

### 2.3 UserView — 用户视图（只读）

| 属性 | 类型 | 说明 |
|------|------|------|
| `userId` | `Long` | 用户 ID |
| `userName` | `String` | 用户名 |
| `nickName` | `String` | 昵称 |
| `phoneNumber` | `String` | 手机号 |
| `password` | `String` | 加密密码 |
| `avatar` | `Long` | 头像 |
| `status` | `String` | 状态 |
| `delFlag` | `String` | 逻辑删除 |
| `loginIp` | `String` | 登录 IP |
| `loginDate` | `LocalDateTime` | 登录时间 |
| `createTime` | `LocalDateTime` | 创建时间 |
| `birthDate` | `LocalDate` | 生日（来自 user_profile） |

**数据来源**：数据库视图 `tpl_user_view`，联表 `sys_user` + `tpl_user_profile`  
**用途**：登录时一次查询获取用户完整信息

---

## 三、数据传输对象（DTO）

### 3.1 LoginRequest — 登录请求

| 字段 | 类型 | 说明 |
|------|------|------|
| `username` | `String` | 用户名 |
| `password` | `String` | 明文密码 |
| `code` | `String` | 图形验证码 |
| `uuid` | `String` | 验证码唯一标识 |

### 3.2 LoginResponse — 登录响应

| 字段 | 类型 | 说明 |
|------|------|------|
| `access_token` | `String` | JWT 访问令牌 |
| `token_type` | `String` | 令牌类型（固定 "Bearer"） |

### 3.3 RegisterRequest — 注册请求

| 字段 | 类型 | 说明 |
|------|------|------|
| `username` | `String` | 用户名 |
| `password` | `String` | 明文密码 |
| `phoneNumber` | `String` | 手机号（可选） |
| `code` | `String` | 图形验证码 |
| `uuid` | `String` | 验证码唯一标识 |

### 3.4 UserInfoVO — 用户信息

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `Long` | 用户 ID |
| `userName` | `String` | 用户名 |
| `nickName` | `String` | 昵称 |
| `phoneNumber` | `String` | 手机号 |
| `email` | `String` | 邮箱 |
| `birthDate` | `LocalDate` | 生日 |

---

## 四、统一响应体 `R<T>`

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `int` | 状态码，200=成功，500=业务异常 |
| `msg` | `String` | 提示信息 |
| `data` | `T` | 业务数据（泛型） |

**静态工厂方法**：
- `R.ok(T data)` → code=200, msg="success"
- `R.ok()` → code=200, msg="success", data=null
- `R.fail(String msg)` → code=500
- `R.fail(int code, String msg)` → 自定义错误码

---

## 五、验证规则

### 5.1 注册

| 字段 | 规则 | 错误消息 |
|------|------|----------|
| `username` | 非空，且在 `tpl_user_view` 中唯一 | "用户名已存在" |
| `password` | 非空 | 无显式校验（依赖 BCrypt） |
| `code` | 非空 + 与 Redis 中的值相等（不区分大小写） | "验证码不能为空" / "验证码已过期" / "验证码错误" |
| `uuid` | 非空 | "验证码不能为空" |

### 5.2 登录

| 字段 | 规则 | 错误消息 |
|------|------|----------|
| `username` | 非空 | "用户名和密码不能为空" |
| `password` | 非空 | "用户名和密码不能为空" |
| `code` | 非空（captcha 启用时） | "验证码不能为空" / "验证码已过期" / "验证码错误" |
| `uuid` | 非空（captcha 启用时） | "验证码不能为空" |

### 5.3 业务校验

| 校验项 | 条件 | 错误消息 |
|--------|------|----------|
| 用户存在性 | `UserViewMapper.selectByUserName()` 非 null | "用户不存在" |
| 密码正确性 | `BCrypt.checkpw()` 返回 false | "密码错误" |
| 账号状态 | `status == "1"` | "用户已被停用" |
