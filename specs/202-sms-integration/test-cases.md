# 202-sms-integration 测试用例

> 模块：短信平台 API 对接（sms-integration）
> 项目：tpl-app-api
> 对应规格：[spec.md](./spec.md)
> 最后更新：2026-08-14

---

## 一、测试策略

| 层级 | 类型 | 说明 |
|------|------|------|
| 单元测试 | SmsService 参数校验 | 无需网络，纯逻辑断言 |
| 单元测试 | SmsService 缓存去重/消费校验 | Mock Redis，纯逻辑断言 |
| 单元测试 | SmsProperties 默认值 | 配置绑定默认值断言 |
| 集成测试 | 真实发送（可选） | 依赖外部平台，不在 CI 执行 |

---

## 二、单元测试用例

### TC-01 手机号非法 — 非 11 位

- **Given** phone="123"，code="1234"
- **When** 调用 `sendVerificationCode(phone, code)`
- **Then** 抛出 `IllegalArgumentException`，消息 "手机号格式不正确"

### TC-02 手机号非法 — 空值

- **Given** phone=null（或空串），code="1234"
- **When** 调用 `sendVerificationCode(phone, code)`
- **Then** 抛出 `IllegalArgumentException`

### TC-03 手机号非法 — 不以 1 开头

- **Given** phone="23800000000"，code="1234"
- **When** 调用 `sendVerificationCode(phone, code)`
- **Then** 抛出 `IllegalArgumentException`

### TC-04 验证码非法 — 长度不足

- **Given** phone="13800000000"，code="123"
- **When** 调用 `sendVerificationCode(phone, code)`
- **Then** 抛出 `IllegalArgumentException`，消息 "验证码格式不正确，应为 4-6 位数字或字母"

### TC-05 验证码非法 — 长度超限

- **Given** phone="13800000000"，code="1234567"
- **When** 调用 `sendVerificationCode(phone, code)`
- **Then** 抛出 `IllegalArgumentException`

### TC-06 验证码非法 — 含非法字符

- **Given** phone="13800000000"，code="12ab!"
- **When** 调用 `sendVerificationCode(phone, code)`
- **Then** 抛出 `IllegalArgumentException`

### TC-07 SmsProperties 默认值

- **Given** 仅设置 `templateCode`
- **When** 读取 `base-url`/`connect-timeout`/`read-timeout`
- **Then** 分别为 `https://push.spug.cc`、`5000`、`10000`

### TC-08 sendCode 命中缓存跳过发送

- **Given** 手机号已缓存验证码（未过期）
- **When** 调用 `sendCode(phone)`
- **Then** 不重新生成、不重新写入缓存、不发送，返回成功（requestId 为空）

### TC-09 sendCode 未命中缓存则缓存并发送

- **Given** 手机号无缓存验证码
- **When** 调用 `sendCode(phone)`
- **Then** 生成 6 位数字验证码、写入缓存（TTL 10 分钟）并触发发送

### TC-10 verifyCode 一致则消费

- **Given** 手机号缓存验证码为 "123456"
- **When** 调用 `verifyCode(phone, "123456")`
- **Then** 清空缓存，返回 true

### TC-11 verifyCode 不一致则拒绝

- **Given** 手机号缓存验证码为 "123456"
- **When** 调用 `verifyCode(phone, "654321")`
- **Then** 不清空缓存，返回 false

### TC-12 verifyCode 无缓存/空参则拒绝

- **Given** 手机号无缓存（或手机号/验证码为空）
- **When** 调用 `verifyCode(phone, code)`
- **Then** 返回 false

---

## 三、集成测试用例（可选，非 CI）

### TC-13 正常发送

- **Given** 真实手机号 + 合法验证码 + 有效额度
- **When** 调用 `sendCode`
- **Then** 返回 `code=200`，`requestId` 非空

### TC-14 上游失败

- **Given** 额度不足 / 模板变量错误
- **When** 调用 `sendCode`
- **Then** 返回 `code != 200`，`msg` 说明原因
