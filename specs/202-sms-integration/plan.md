# 202-sms-integration 模块技术方案

> 模块：短信平台 API 对接（sms-integration）
> 项目：tpl-app-api（tpl-workspace后端 — 用户侧API）
> 对应规格：[spec.md](./spec.md)
> 最后更新：2026-08-14
> 说明：本文档描述短信模块在 tpl-app-api 中的**落地实现**。

---

## 1. 技术上下文

### 1.1 模块技术栈

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | Java | 21 | 主力开发语言 |
| 框架 | Spring Boot | 4.1.0 | IoC、配置绑定 |
| HTTP 客户端 | Hutool `HttpRequest` | 5.8.47 | 发起短信 POST 请求 |
| JSON 解析 | Hutool `JSONUtil` | 5.8.47 | 解析响应 |
| 简化代码 | Lombok | optional | @Data、@RequiredArgsConstructor、@Slf4j |
| 缓存 | Spring Data Redis (Lettuce) | Spring Boot 默认 | 验证码缓存（`sms_code:{phone}`） |

### 1.2 模块文件清单

```
src/main/java/com/tpl/appapi/
├── config/
│   └── SmsProperties.java              # sms.* 配置绑定（@ConfigurationProperties）
├── service/
│   └── SmsService.java                 # 短信验证码发送、缓存去重与消费校验
├── model/
│   └── dto/
│       └── SmsSendResult.java          # 发送结果 DTO（code/msg/requestId）
└── (resources)
    └── application.yml                 # 新增 sms 配置段
```

---

## 2. 宪法合规性检查

> 对照 [constitution.md](../constitution.md) 中的 10 条原则逐项检查。

| # | 原则 | 合规状态 | 证据 |
|---|------|---------|------|
| 一 | 分层架构规范 | ✅ 合规 | 功能封装位于 `service/`（SmsService），配置位于 `config/`（SmsProperties），DTO 位于 `model/dto/`；无 Controller/Mapper（本模块为内部能力，不涉及数据库） |
| 二 | 统一响应格式 | N/A | 本模块不暴露 REST 端点，无 `R<T>` 包装；调用方自行包装 |
| 三 | 异常处理策略 | ✅ 合规 | 参数校验抛 `IllegalArgumentException`，IO 异常抛 `IllegalStateException` |
| 四 | 鉴权模型 | N/A | 本模块为内部功能类，鉴权由消费方 Controller 负责 |
| 五 | 数据库交互规范 | N/A | 不操作数据库 |
| 六 | 配置管理 | ✅ 合规 | `sms.*` 通过 `@ConfigurationProperties` 绑定，模板编码在 application.yml 配置 |
| 七 | 代码简化与风格 | ✅ 合规 | `@RequiredArgsConstructor` + `private final` 构造函数注入，`@Slf4j` 日志 |
| 八 | 数据库实体标识 | N/A | 无实体 |
| 九 | 验证码安全 | ✅ 合规 | 模板编码仅存服务端配置，验证码生成/校验由消费方负责 |
| 十 | 数据库密码安全 | N/A | 不涉及密码存储 |

---

## 3. 接口契约

### 3.1 功能类方法

```java
public SmsSendResult sendCode(String phone)               // 发送（缓存去重）
public boolean verifyCode(String phone, String code)      // 消费校验
public SmsSendResult sendVerificationCode(String phone, String code) // 底层发送
```

| 方法 | 说明 | 返回 |
|------|------|------|
| `sendCode(phone)` | 缓存去重发送：命中缓存不重发；否则生成 6 位数字验证码、缓存 10 分钟并发送 | `SmsSendResult`（命中缓存时 requestId 为空） |
| `verifyCode(phone, code)` | 消费校验：一致则清空缓存返回 true，否则 false | `boolean` |
| `sendVerificationCode(phone, code)` | 底层发送指定验证码（不做缓存） | `SmsSendResult` |

**参数校验**：`phone` 匹配 `^1\d{10}$`；`code` 匹配 `^[0-9a-zA-Z]{4,6}$`。

**异常**：
- `IllegalArgumentException`：手机号/验证码格式非法（`sendCode`/`sendVerificationCode`）
- `IllegalStateException`：HTTP 连接异常或超时
- `verifyCode` 不抛异常，非法输入统一返回 false

### 3.2 上游接口

```
POST {sms.base-url}/sms/{sms.template-code}
Content-Type: application/json
Body: {"to": phone, "code": code}
```

---

## 4. 实现策略

### 4.1 发送流程

```
sendVerificationCode(phone, code)
  1. validatePhone(phone)     → 非法抛 IllegalArgumentException("手机号格式不正确")
  2. validateCode(code)       → 非法抛 IllegalArgumentException("验证码格式不正确，应为 4-6 位数字或字母")
  3. buildUrl()               → baseUrl + "/sms/" + templateCode
  4. buildBody(phone, code)   → {"to":..., "code":...}
  5. HttpRequest.post(url).contentType("application/json").body(body)
        .setConnectionTimeout(connectTimeout).timeout(readTimeout).execute()
  6. parseResult(responseBody) → SmsSendResult{code, msg, requestId}
  7. 捕获异常 → log.error → 抛 IllegalStateException("短信发送失败，请稍后重试", e)
```

### 4.2 响应解析

```java
JSONObject json = JSONUtil.parseObj(responseBody);
result.setCode(json.getInt("code", -1));
result.setMsg(json.getStr("msg"));
result.setRequestId(json.getStr("request_id"));
```

### 4.3 缓存与消费流程

```
sendCode(phone)
  1. validatePhone(phone)              → 非法抛 IllegalArgumentException
  2. GET sms_code:{phone}              → 命中（未过期）→ 返回成功（不生成、不发送）
  3. 生成 6 位数字验证码（RandomUtil.randomNumbers(6)）
  4. SET sms_code:{phone} = code, TTL 10 分钟
  5. sendVerificationCode(phone, code) → 触发短信平台发送
  6. 发送失败 → DEL 缓存（回滚）→ 抛出 IllegalStateException

verifyCode(phone, code)
  1. phone/code 为空 → return false
  2. GET sms_code:{phone}              → null 或 != code → return false
  3. DEL sms_code:{phone}              → 消费
  4. return true
```

---

## 5. 配置参数

```yaml
# application.yml
sms:
  base-url: https://push.spug.cc
  template-code: 5nI_gjqEQV-dxQWrW5aIMg
  connect-timeout: 5000
  read-timeout: 10000
```

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `sms.base-url` | `https://push.spug.cc` | 短信平台基础地址 |
| `sms.template-code` | 无（必填） | 短信模板编码 |
| `sms.connect-timeout` | `5000` | 连接超时（毫秒） |
| `sms.read-timeout` | `10000` | 读取超时（毫秒） |
| 验证码缓存 Key | `sms_code:{phone}` | 一个手机号一个验证码 |
| 验证码 TTL | `10` 分钟 | 硬编码 `SMS_CODE_TTL_MINUTES` |

---

## 6. 测试策略

| 测试类型 | 范围 | 关键场景 | 对应验收场景 |
|---------|------|---------|-------------|
| 单元测试 - SmsService | 参数校验 | 非法手机号/验证码抛异常 | AC-02、AC-03 |
| 单元测试 - SmsService | 缓存去重 | 命中缓存不重复发送 | AC-05 |
| 单元测试 - SmsService | 消费校验 | 一致消费/不一致拒绝 | AC-06、AC-07、AC-08 |
| 单元测试 - SmsProperties | 配置默认值 | base-url/超时默认值 | — |
| 集成测试（可选） | 真实发送 | 需真实手机号与额度 | AC-01 |

> 真实短信发送（AC-01）依赖外部平台与短信额度，不在 CI 中执行。

---

## 7. 依赖关系

### 7.1 Maven 依赖

无需新增依赖：`hutool-all` 已包含 `HttpRequest`、`JSONUtil`。

### 7.2 模块间依赖

```
202-sms-integration
  ├── 依赖: hutool-all（HttpRequest + JSONUtil + RandomUtil）
  ├── 依赖: spring-boot（@ConfigurationProperties）
  ├── 依赖: spring-data-redis（验证码缓存）
  ├── 被依赖: 002-user-auth（后续注册/登录发送短信验证码）
```

---

## 8. 已知问题与改进项

| # | 问题 | 严重度 | 建议 |
|---|------|:--:|------|
| I-SMS-001 | `sendCode` get-then-set 非原子，并发下可能重复发送 | 低 | 使用 `setIfAbsent`（SETNX）原子占位后再发送 |
| I-SMS-002 | `verifyCode` get-then-delete 非原子，并发下可能重复消费 | 低 | 使用 Lua 脚本或 `getAndDelete` 原子消费（与 AuthService 现状一致） |
| I-SMS-003 | 验证码使用 `RandomUtil.randomNumbers`（非加密安全随机） | 低 | 生产环境改用 `SecureRandom` |
```
