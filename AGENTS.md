# Agent Platform - AI Agent Guide

> 面向非编码场景办公人员的 Web 智能体对话平台（Excel 处理、PPT 编写、文档编写等）

## Project Overview

**模块化单体架构** - Spring Boot 3.3 / Java 21 / Virtual Threads / SSE 流式对话

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| 语言 & JDK | Java | 21 (LTS) |
| 框架 | Spring Boot | 3.3.5 |
| Web层 | Spring MVC + SseEmitter | 6.x |
| AI集成 | Spring AI | 1.0.6 |
| ORM | MyBatis-Plus | 3.5.9 |
| 数据库 | PostgreSQL + pgvector | 16 |
| 缓存 | Redis | 7.x |
| 对象存储 | MinIO | latest |
| 消息队列 | RabbitMQ | 3.13.x |

## Critical Rules

### 1. API 统一响应格式 - MANDATORY

所有 Controller 返回 `ApiResponse<T>` 包装：

```java
// ✅ CORRECT
return ApiResponse.ok(vo, RequestIdContext.current());

// ❌ INCORRECT - Never return raw entity
return agentEntity;
```

### 2. Virtual Threads - 已启用

```yaml
spring.threads.virtual.enabled: true
```

阻塞等待 LLM 响应不消耗平台线程。

### 3. SSE 流式响应 - 使用 SseEmitter

```java
// ✅ CORRECT - SseEmitter 异步推送
@PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter sendMessage(...) {
    SseEmitter emitter = new SseEmitter(300_000L);
    chatService.streamReply(..., emitter);
    return emitter;
}

// ❌ INCORRECT - Never use WebFlux
return Flux.fromStream(...);
```

## Module Structure

```
backend/
├── ap-app/                  # 启动模块（Spring Boot Application）
├── ap-module-agent/         # Agent 管理（CRUD/版本/导入导出）
├── ap-module-chat/          # 对话引擎（SSE/步骤控制/幂等性）
├── ap-module-asset/         # Skill/MCP/知识库
├── ap-module-market/        # 市场（发布/收藏/导入）
├── ap-module-file/          # 文件上传下载
└── ap-common/               # 公共组件
    ├── common-core/         # ApiResponse/ErrorCode/GlobalExceptionHandler
    ├── common-mybatis/      # BaseEntity/乐观锁/逻辑删除/Entity/Mapper
    └── common-redis/        # Redis配置
```

**依赖规则**: `ap-module-*` 仅依赖 `ap-common`，不跨模块依赖

## Build Commands

```bash
# 拉起基础设施
docker compose -f docker/docker-compose.infra.yml up -d

# 编译全量
./mvnw clean package -DskipTests

# 运行全部单元测试（仅测试，不安装 JAR 到本地仓库）
./mvnw test -pl ap-common/common-core,ap-common/common-mybatis,ap-common/common-redis,ap-module-agent,ap-module-chat,ap-module-asset,ap-module-market,ap-module-file

# 运行全部测试（含 ap-app 集成测试）— 推荐
./mvnw clean verify

# 等价替代: install 先更新本地 JAR，再 test
./mvnw install -DskipTests && ./mvnw test

# 运行集成测试
./mvnw verify -Dgroups=integration

# 启动应用（本地）
./mvnw spring-boot:run -pl ap-app -Dspring-boot.run.profiles=local
```

> **Important**: `./mvnw test` alone will fail on `ap-app` when shared entities have been moved between modules (e.g. from ap-module-agent → common-mybatis). The `ap-app` integration tests resolve module dependencies from `~/.m2`, not reactor `target/classes`. Always use `./mvnw clean verify` or `./mvnw install -DskipTests && ./mvnw test` after refactoring shared code.

## API Endpoints

| Module | Base Path | Description |
|--------|-----------|-------------|
| Agent | `/api/v1/agents` | CRUD + 版本管理 + 导入导出 |
| Chat | `/api/v1/chat` | SSE 流式对话 + 会话管理 |
| Skill | `/api/v1/skills` | Skill CRUD + 导出 |
| MCP | `/api/v1/mcps` | MCP CRUD + 连接测试 + 工具发现 |
| Knowledge | `/api/v1/knowledge-bases` | 知识库 CRUD + 文档上传 + 语义检索 |
| File | `/api/v1/files` | 文件上传下载 + Token 签发 |
| Health | `/api/v1/health` | Virtual Threads 验证 |

## API Documentation Access

### Swagger UI (任务 12.1 待实现)

```
Swagger UI:  http://localhost:8080/swagger-ui.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml
```

> **当前状态**: SpringDoc OpenAPI 依赖尚未添加，任务 12.1 计划中

### 验证命令

```bash
# 启动后访问
curl http://localhost:8080/v3/api-docs

# 验证 Swagger UI
curl http://localhost:8080/swagger-ui.html
```

### 配置路径（可选）

```properties
# application.yml
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/api-docs
```

## Testing Guidelines

- 单元测试：每个模块 `src/test/java` 目录
- 集成测试：Testcontainers (PG + Redis + RabbitMQ + MinIO)
- 覆盖率要求：核心业务逻辑 ≥ 80%

## MVP Stub Implementations

| Stub | 说明 | 替换方向 |
|------|-----|---------|
| `StubUserContext` | 固定测试用户 | W3 OAuth + SecurityContext |
| `SimplePermissionChecker` | 仅 owner + visibility 判定 | 完整 RBAC + 组织关系 |
| `PlainCredentialStore` | 明文 + 末3位脱敏 | AES-256-GCM + Jasypt |

## Quick Reference

| 操作 | 命令 |
|------|------|
| 启动基础设施 | `docker compose -f docker/docker-compose.infra.yml up -d` |
| 编译项目 | `./mvnw clean package -DskipTests` |
| 运行全部测试 | `./mvnw clean verify` |
| 运行单元测试 | `./mvnw test -pl ap-common/...,ap-module-agent,...` |
| 启动应用 | `./mvnw spring-boot:run -pl ap-app` |
| 检查健康 | `curl http://localhost:8080/api/v1/health` |
| 查看 API 文档 | `curl http://localhost:8080/v3/api-docs` |

## Detailed Documentation

- **需求文档**: `.Ds-Spec/specs/agent-platform/requirements.md`
- **设计文档**: `.Ds-Spec/specs/agent-platform/design.md`
- **任务清单**: `.Ds-Spec/specs/agent-platform/tasks.md`
- **UI/UX 设计图**: `design/agent-platform-ui.pen`（Pencil 设计文件，含设计系统组件与 Dashboard 等页面布局）
- **设计/实现差异分析**: `.Ds-Spec/specs/ui-design-implementation-gap-analysis/`
- **开发进度**: `PROGRESS.md`

---

**Total Length**: ~120 lines (符合 GitHub 最佳实践 80-150 行范围)