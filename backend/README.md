# Agent Platform — 后端

模块化单体（Modular Monolith），Spring Boot 3.3 / Java 21 / Virtual Threads / Spring MVC + SseEmitter。

## 模块布局

```
backend/
├── ap-app/                  # 启动模块（Spring Boot Application）
├── ap-module-agent/         # Agent 管理（任务 3.x）
├── ap-module-chat/          # 对话引擎（任务 4.x）
├── ap-module-asset/         # Skill / MCP / 知识库（任务 6/7/8）
├── ap-module-market/        # 市场（任务 11.x）
├── ap-module-file/          # 文件（任务 9.x）
├── ap-common/
│   ├── common-core/         # ApiResponse / ErrorCode / GlobalExceptionHandler / UserContext / PermissionChecker / CredentialStore
│   ├── common-mybatis/      # BaseEntity / 乐观锁 / 逻辑删除 / 分页 / MetaObjectHandler
│   └── common-redis/        # Redis 配置（占位）
└── docker/
    └── docker-compose.infra.yml   # PostgreSQL 16 + pgvector / Redis 7 / RabbitMQ / MinIO
```

## 一键命令

```bash
# 拉起基础设施（PostgreSQL+pgvector / Redis / RabbitMQ / MinIO）
docker compose -f docker/docker-compose.infra.yml up -d

# 编译全量
./mvnw clean package -DskipTests

# 运行单元测试
./mvnw test

# 启动应用（本地）
./mvnw spring-boot:run -pl ap-app -Dspring-boot.run.profiles=local
```

## 任务 1（项目脚手架与基础设施）已交付

- 1.1 Maven 多模块骨架
- 1.2 Docker Compose 基础设施 + `application-local.yml`
- 1.3 Spring MVC + Virtual Threads + 冒烟端点
  - `GET /api/v1/health` — 验证 Virtual Thread
  - `GET /api/v1/health/me` — 验证 `@CurrentUser`
  - `GET /api/v1/health/sse` — 推送 3 个 SSE 事件
- 1.4 `common-core`：`ApiResponse` / `ErrorCode` / `BizException` / `GlobalExceptionHandler` / `RequestIdFilter`
- 1.5 `common-mybatis`：`BaseEntity` / `AuditMetaObjectHandler` / 分页 + 乐观锁拦截器
- 1.6 MVP Stub `StubUserContext` + `@CurrentUser` ArgumentResolver
- 1.7 MVP Stub `SimplePermissionChecker`
- 1.8 MVP Stub `PlainCredentialStore`

> Auth / Audit / 凭据加密 等能力按 MVP 范围延后；所有 stub 通过接口契约保证后续替换零侵入。

## MVP 排除项与 TODO 索引

| 关注点          | 当前实现                            | 替换方向                                  |
| ------------ | ------------------------------- | ------------------------------------- |
| 用户上下文        | `StubUserContext` 固定测试用户        | W3 OAuth + SecurityContext 解析         |
| 权限矩阵         | `SimplePermissionChecker`：仅 owner + visibility 判定 | 完整 §3.6 含组织成员关系 + 角色 + admin 旁路 |
| 凭据加密         | `PlainCredentialStore` 明文 + 末 3 位脱敏 | AES-256-GCM + Jasypt，密钥环境变量注入       |
| 审计日志         | 调用点写 `// TODO` 注释                | `ap-module-audit` 模块                  |
