# 202-sms-integration 模块规格文档

> 模块：短信平台 API 对接（sms-integration）
> 项目：tpl-app-api（tpl-workspace后端 — 用户侧API）
> 状态：实现中
> 最后更新：2026-08-14
> 父工程规格引用：需求基准见 [../../specs/202-sms-integration/spec.md](../../specs/202-sms-integration/spec.md)，本文档仅补充本工程（后端）特有规格

---

## 1. 模块概述

### 1.1 模块目的

在 tpl-app-api 中对接 Spug Push 短信平台，实现**短信验证码发送、缓存去重与消费校验**功能封装类（`SmsService`），供用户认证（002）等业务模块调用。

### 1.2 模块边界

**在范围内**：
- 短信验证码发送功能封装（`SmsService.sendCode` / `sendVerificationCode`）
- 短信验证码生成、缓存（Redis）与消费校验（`SmsService.verifyCode`）
- 短信平台配置绑定（`SmsProperties`）
- 发送结果封装（`SmsSendResult`）

**不在范围内**：
- 对外 REST 端点（本模块为内部功能类，不暴露 Controller）
- 发送状态/记录查询

---

## 2. 用户故事

> 与父工程 [spec.md](../../specs/202-sms-integration/spec.md) §2 一致，本工程视角补充如下：

### US-001：调用发送验证码

> 作为后端开发者，我可以注入 `SmsService` 并调用 `sendVerificationCode(phone, code)` 向指定手机号发送验证码，获得包含 `request_id` 的结果对象。

**验收标准**：
- 传入合法手机号（11 位）与合法验证码（4-6 位数字/字母）
- 返回 `SmsSendResult`，成功时 `code == 200` 且 `requestId` 非空

---

## 3. 功能需求

> 需求基准见父工程 [spec.md](../../specs/202-sms-integration/spec.md) §3（FR-202-001 ~ FR-202-011）。本工程落地补充如下：

### 3.1 发送封装

- **FR-SMS-001**：`SmsService` MUST 提供 `sendVerificationCode(String phone, String code)` 方法
- **FR-SMS-002**：`sendVerificationCode` MUST 先校验 phone 再校验 code，任一非法抛 `IllegalArgumentException`，且 MUST NOT 发起 HTTP 请求
- **FR-SMS-003**：`sendVerificationCode` MUST 使用 Hutool `HttpRequest` 向 `{base-url}/sms/{template-code}` 发起 POST，`Content-Type: application/json`
- **FR-SMS-004**：请求体 MUST 为 `{"to": phone, "code": code}`
- **FR-SMS-005**：HTTP 连接异常/超时 MUST 捕获并抛出 `IllegalStateException`（含原始异常），MUST NOT 静默吞掉

### 3.2 配置

- **FR-SMS-006**：`SmsProperties` MUST 使用 `@ConfigurationProperties(prefix="sms")` 绑定 `base-url`、`template-code`、`connect-timeout`、`read-timeout`
- **FR-SMS-007**：`template-code` 无默认值，MUST 在 `application.yml` 显式配置
- **FR-SMS-008**：`base-url` 默认 `https://push.spug.cc`，`connect-timeout` 默认 5000ms，`read-timeout` 默认 10000ms

### 3.3 验证码缓存与消费

- **FR-SMS-009**：`sendCode(phone)` MUST 实现缓存去重——一个手机号仅缓存一个验证码，Redis Key `sms_code:{phone}`
- **FR-SMS-010**：验证码缓存 MUST 10 分钟失效（`SMS_CODE_TTL_MINUTES = 10`）
- **FR-SMS-011**：`sendCode` 命中缓存（未过期）时 MUST NOT 重新生成、MUST NOT 重复发送，直接返回成功
- **FR-SMS-012**：`sendCode` 未命中缓存时 MUST 生成 6 位数字验证码、写入缓存并触发发送
- **FR-SMS-013**：`verifyCode(phone, code)` MUST 在手机号+输入验证码与缓存一致时清空缓存并返回 true，否则返回 false
- **FR-SMS-014**：`verifyCode` 对空手机号/空验证码 MUST 返回 false，不抛异常
- **FR-SMS-015**：`sendCode` 发送失败时 MUST 回滚（删除）该手机号验证码缓存，避免残留未送达的验证码

---

## 4. 关键实体

| 实体 | 类型 | 用途 |
|------|------|------|
| `SmsProperties` | 配置类（`config/`） | 绑定 `sms.*` 配置项 |
| `SmsService` | 服务类（`service/`） | 短信验证码发送、缓存去重与消费校验 |
| `SmsSendResult` | DTO（`model/dto/`） | 发送结果：code/msg/requestId |
| 验证码缓存 | Redis | Key `sms_code:{phone}`，TTL 10 分钟 |

---

## 5. 验收场景

| # | 场景 | 输入 | 期望结果 |
|---|------|------|----------|
| AC-01 | 正常发送 | phone="13800000000", code="153146" | 返回 code=200，requestId 非空 |
| AC-02 | 手机号非法 | phone="123", code="1234" | 抛 IllegalArgumentException（"手机号格式不正确"），不发起 HTTP |
| AC-03 | 验证码非法 | phone="13800000000", code="123" | 抛 IllegalArgumentException（"验证码格式不正确..."），不发起 HTTP |
| AC-04 | 上游不可达 | 连接超时 | 抛 IllegalStateException |
| AC-05 | 缓存去重 | phone 已缓存且未过期，再次 sendCode | 不重新生成/发送，返回成功（requestId 为空） |
| AC-06 | 消费成功 | phone+code 与缓存一致 | 清空缓存，返回 true |
| AC-07 | 消费失败 | phone+code 与缓存不一致 | 返回 false |
| AC-08 | 消费-无缓存 | phone 无缓存验证码 | 返回 false |

---

## 6. 非功能需求

- **NFR-SMS-001**：发送调用 MUST 设置连接超时与读取超时
- **NFR-SMS-002**：模板编码 MUST 仅存于服务端配置，不得下发客户端
- **NFR-SMS-003**：发送失败 MUST 记录日志

---

## 7. 假设与约束

| # | 假设/约束 | 说明 |
|---|-----------|------|
| A-SMS-001 | Spug Push 平台可用 | 发送依赖外部平台，需公网可达 + IP 白名单 |
| A-SMS-002 | 模板编码已就绪 | `5nI_gjqEQV-dxQWrW5aIMg` 由平台生成 |
| A-SMS-003 | 验证码生成由消费方负责 | 本模块仅负责发送，不生成/存储/校验验证码 |

---

## 8. 依赖

### 8.1 框架/库依赖

| 库 | 用途 |
|----|------|
| Hutool (5.8.47) | HttpRequest 发起 HTTP、JSONUtil 解析响应 |
| Lombok | @Data、@RequiredArgsConstructor、@Slf4j |
| Spring Boot | @ConfigurationProperties 配置绑定 |

### 8.2 依赖本模块的外部系统

| 系统 | 依赖方式 | 说明 |
|------|---------|------|
| 用户认证（002）等业务模块 | Java 注入 `SmsService` | 注册/登录/找回密码时发送验证码 |
