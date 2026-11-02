# AGENTS.md — tpl-app-api（用户端核心业务 API）

> 本文件为本子工程（`tpl-app-api/`）的 AI Agent 工作规范，是父工程 `tpl-workspace/AGENTS.md` 在本工程的具体化。
> 所有规范均基于本工程实际存在的技能、README 与 specs 编写。

---

## 一、项目简介

tpl-app-api 是「tpl-workspace」产品的**用户侧核心业务 API 服务**（用户端），基于 **Spring Boot 4.1.0 + JDK 21**
构建，为前端（tpl-app-web / tpl-app-android / tpl-app-harmony / tpl-app-mini）提供 RESTful API。
采用经典三层架构（Controller → Service → Mapper），参考 RuoYi-Vue-Plus，实现轻量级 Token 认证（非 RBAC）。
端口 **8082**。

## 二、技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 4.1.0 | REST API 容器，嵌入式 Tomcat |
| JDK | Java 21 | LTS |
| 认证授权 | Sa-Token 1.45.0 | JWT 模式 + Redis 会话 |
| ORM | MyBatis-Plus 3.5.17 | BaseMapper 通用 CRUD |
| 数据库 | PostgreSQL 15+ | 与 tpl-manage 共享 `tpl_manage` |
| 缓存 | Redis 7.x (Lettuce) | 验证码、Token 会话 |
| 工具 | Hutool 5.8.47 + Lombok | 工具与样板代码 |
| 构建 | Maven Wrapper | 依赖管理与构建 |

## 三、代码结构

经典三层 + 认证：
- `controller/` — REST 端点（`/auth/register`、`/auth/login`、`/auth/logout`、`/auth/code`）
- `service/` — 业务逻辑（含内部 `SmsService`）
- `mapper/` — MyBatis-Plus 数据访问层

## 四、规格文档（specs）

`specs/` 下存放模块规格：`002-user-auth`（用户认证）、`004-i18n`、`202-sms-integration`（短信对接），
及 `ARCHITECTURE.md`、`TECH.md`、`STRUCTURE.md`、`overall-api.md`、`constitution.md` 等系统级文档。

## 五、常用开发命令

```bash
./mvnw spring-boot:run        # 启动（默认 8082）
./mvnw clean package          # 打包
```

## 六、代码库探索规范（codegraph）

本工程已配置 codegraph 代码地图（`.codegraph/` 索引），并在 git `post-commit` 钩子中
自动增量更新（`codegraph sync`）。

- **提交后**无需手动操作，索引自动与 HEAD 保持同步。
- **探索代码**优先使用 codegraph，而非 grep/Read 循环：
  - `codegraph status` — 检查索引状态
  - `codegraph context "<任务>"` — 生成任务相关代码简报
  - `codegraph query <符号>` — 查找符号定义
  - `codegraph callers <符号>` / `codegraph callees <符号>` — 调用关系
  - `codegraph impact <符号>` — 变更影响范围
  - `codegraph files --filter <目录>` — 索引文件树
- 索引过期（切换分支/外部 pull）时手动 `codegraph sync` 增量更新。
- `codegraph` 查询不到时先 `sync` 再重查；确属 codegraph 不支持的语法才回退 grep。

> 索引目录 `.codegraph/` 已在 `.gitignore` 排除，不入库。

## 七、Git 提交规范

遵循父工程 `tpl-workspace/AGENTS.md` 的 Conventional Commits 规范，提交前加载 `git-commit` 技能。

## 八、开发流程与技能

- Java/Spring 开发遵循父工程开发流程（`specs-based-devflow`、`requesting-code-review`、`receiving-code-review`、`systematic-debugging`）。
- 代码库探索一律走 **codegraph**（见第六节）。

---

> 维护者：fellow99 · 属「tpl-workspace 多端业务应用框架」
