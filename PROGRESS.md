# Development Progress

> Agent Platform - Web 智能体对话平台开发进度追踪

## Current Status

| Module | Status | Tests |
|--------|--------|-------|
| Tasks 1-4 (Scaffold, DB, Agent, Chat) | ✅ Complete | — |
| Task 5 (ToolRegistry & ModelRegistry) | ✅ Complete | 40 (common-core) |
| Task 6 (Skill CRUD) | ✅ Complete | 20 (SkillCrudTest) |
| Task 7 (MCP CRUD + Client) | ✅ Complete | 18 (McpCrudTest + McpToolCallTest) |
| Task 8 (Knowledge Base CRUD) | ✅ Complete | 19 (KnowledgeBaseCrudTest) |
| Task 9 (File) | ✅ Complete | 17 (FileServiceTest) |
| Task 10 (Model Config) | ✅ Complete | 14 (ModelRegistryImplTest) |
| Task 11 (Market) | 🚧 Partial (entity/mapper/DTO) | — |
| Task 12 (OpenAPI, CI) | 📋 Planned | — |
| Task 13 (E2E) | 📋 Planned | — |
| Frontend (React + Vite) | 🚧 In Progress | — |

**Total Unit Tests**: 236 ✅ all passing (40 common-core + 31 agent + 36 chat + 69 asset + 17 file + 43 ap-app)

---

## Milestones

| Milestone | Status | Completion |
|-----------|--------|------------|
| **v1.0 MVP Core** (Tasks 1-4) | ✅ Shipped | 100% |
| **v1.1 Asset Modules** (Tasks 5-10) | ✅ Complete | 100% |
| **v1.2 Market & File** (Tasks 9, 11) | 🚧 In Progress (File ✅, Market 🚧) | — |
| **v2.0 Security & Auth** | 📋 Planned | — |

---

## Completed Tasks

### ✅ Tasks 1-4: Core Foundation

See previous sessions. Key commits:
- `d26e82d` — Scaffold + infrastructure
- `6347e73` — DB migration + Flyway
- `21a2b14` — Agent CRUD
- `4cd854c` — Chat engine (SSE, Orchestrator, ToolDispatcher)

---

### ✅ Task 5: ToolRegistry & ModelRegistry

**Commits**: `933a504` `b3a1001`

| Subtask | Description | Status |
|---------|-------------|--------|
| 5.1 | ToolRegistry (builtin/MCP/knowledge three-source registration) | ✅ |
| 5.2 | ModelRegistry (builtin + custom model merge, ChatClient builder) | ✅ |

**Files**:
- `common-core/tool/ToolRegistry.java`, `ToolDefinition.java`, `ToolResult.java`, `McpToolInvoker.java`
- `common-core/model/ModelRegistry.java`, `ModelInfo.java`
- `ap-module-agent/provider/ModelRegistryImpl.java`
- `ap-module-asset/provider/ToolRegistryImpl.java`

**Tests**: ToolRegistryTest (18), ModelRegistryTest (12), ToolRegistryImplTest (12), ModelRegistryImplTest (14)

---

### ✅ Task 6: Skill CRUD

**Commits**: `1a8c23e` `b3a1001`

| Subtask | Description | Status |
|---------|-------------|--------|
| 6.1 | Skill CRUD (create/list/detail/update/delete/export) | ✅ |

**Files**:
- `ap-module-asset/controller/SkillController.java`
- `ap-module-asset/service/SkillService.java`

**Tests**: SkillCrudTest (20) — create validation, list, detail, update, delete with dependency check, export

---

### ✅ Task 7: MCP CRUD + Connection + Tool Invocation

**Commits**: `1b37d4d` `b3a1001`

| Subtask | Description | Status |
|---------|-------------|--------|
| 7.1 | MCP CRUD (create/test/update/delete/toggle) | ✅ |
| 7.2 | MCP connection test & tool discovery | ✅ |
| 7.3 | MCP tool runtime invocation (SSE/Streamable HTTP) | ✅ |

**Files**:
- `ap-module-asset/controller/McpController.java`
- `ap-module-asset/service/McpService.java`
- `ap-module-asset/client/McpClient.java`
- `common-core/tool/McpToolInvoker.java`

**Tests**: McpCrudTest (18), McpToolCallTest (6) — CRUD, connection, toggle, retry, error handling

---

### ✅ Task 8: Knowledge Base CRUD

**Commits**: `b3a1001`

| Subtask | Description | Status |
|---------|-------------|--------|
| 8.1 | Knowledge Base CRUD | ✅ |
| 8.2 | Document upload | ✅ |
| 8.3 | Async index pipeline | ✅ |
| 8.4 | Semantic search (pgvector) | ✅ |
| 8.5 | Document delete & cleanup | ✅ |

**Files**:
- `ap-module-asset/controller/KnowledgeController.java`
- `ap-module-asset/service/KnowledgeBaseService.java`

**Tests**: KnowledgeBaseCrudTest (19) — create, list, detail, update, delete, document ops, index/search

---

### ✅ Task 9: File Module

**Commit**: `46f3838`

| Subtask | Description | Status |
|---------|-------------|--------|
| 9.1 | File upload (type/size/Tika MIME validation, MinIO) | ✅ |
| 9.2 | Download/preview token (64-byte SecureRandom) | ✅ |
| 9.3 | File download & preview (Content-Disposition) | ✅ |
| 9.4 | File expired cleanup (@Scheduled hourly) | ✅ |

**Files**:
- `ap-module-file/controller/FileController.java`
- `ap-module-file/service/FileService.java`
- `ap-module-file/cleaner/FileCleaner.java`

**Tests**: FileServiceTest (17) — upload, download, token, cleanup

---

### ✅ Task 10: Model Configuration

**Subtask of Task 5** — builtin + custom model queries, built-in ModelRegistryImpl.

| Subtask | Description | Status |
|---------|-------------|--------|
| 10.1 | Builtin model query | ✅ |
| 10.2 | Custom model CRUD (connectivity, delete reassign) | ✅ |

---

### 🚧 Task 11: Market Module

| Subtask | Description | Status |
|---------|-------------|--------|
| 11.1-11.5 | Entity/Mapper/DTO layer | ✅ |
| — | Controller & Service | 📋 Pending |

**Files**: MarketItemEntity, FavoriteEntity, ReviewEntity, Mappers, DTOs, Converter

---

## Pending Tasks

| Task | Description | Priority |
|------|-------------|----------|
| **Task 11** | Market controller + service (publish/search/favorite/import) | Medium |
| **Task 12** | OpenAPI docs (SpringDoc), error contract tests, CI pipeline | Low |
| **Task 13** | End-to-end smoke tests | Low |
| **Task 12** | OpenAPI docs (SpringDoc), error contract tests, CI pipeline | Low |
| **Task 13** | End-to-end smoke tests | Low |

---

## Frontend Status

| Page | Status |
|------|--------|
| ChatPage | ✅ API integration, SSE streaming, agent switching |
| AgentListPage | ✅ Search, filter, pagination |
| AgentCreatePage | ✅ Multi-step wizard |
| AgentDetailPage | ✅ API binding |
| AgentEditPage | ✅ API binding |
| DashboardPage | ✅ Stats, agent cards |
| SkillListPage | ✅ API binding |
| MCPListPage | ✅ API binding |
| SkillMarketPage | 🚧 In progress |
| MCPMarketPage | 🚧 In progress |
| MCPAddPage | 🚧 In progress |
| AgentCreateToolsPage | ✅ Tool/MCP/Skill binding |
| AgentCreateCollabPage | ✅ Collaboration config |
| AgentCreatePublishPage | ✅ Publish config |

Design system (17 reusable components): `design/agent-platform-ui.pen`

---

## Test Results Summary

| Module | Tests | Status |
|--------|-------|--------|
| common-core | 40 | ✅ Pass |
| ap-module-agent | 31 | ✅ Pass |
| ap-module-chat | 36 | ✅ Pass |
| ap-module-asset | 69 | ✅ Pass |
| ap-module-file | 17 | ✅ Pass |
| ap-app (Smoke/Flyway/Index) | 43 | ✅ Pass |
| **Total** | **236** | **✅ All Pass** |

---

## Error Log

| Timestamp | Error | Resolution |
|-----------|-------|------------|
| — | No blocking errors | All tasks completed successfully |

---

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Modular Monolith | 预留微服务拆分路径，模块边界清晰 |
| Virtual Threads | 阻塞等待LLM不消耗平台线程，高并发对话场景优化 |
| pgvector | 百万级文档无需独立Vector DB，同库事务一致 |
| SSE via SseEmitter | 不引入WebFlux栈，避免Servlet/Reactive混用问题 |
| MVP Stubs | Auth/Audit/凭据加密延后，接口契约保证零侵入替换 |

---

## Git History

| Commit | Description | Files |
|--------|-------------|-------|
| `b3a1001` | MCP module, move entities to common-mybatis, clean designs | +1445 / -30575 |
| `1b37d4d` | ToolResult → common-core, McpToolInvoker, ToolDispatcher | +381 / -21 |
| `9ed2ed2` | Frontend: MSW mocks, refactor MCP/Skill pages, AssetCard | +2442 / -391 |
| `1a8c23e` | AssetReference to common-mybatis, SkillController, test fix | +859 / -42 |
| `933a504` | ToolRegistryImpl, ModelRegistryImpl (+11 files) | +1798 / -1 |
| `949f885` | Frontend refactor: 12 pages | +1018 / -656 |
| `902585c` | Fix task numbering in tasks.md | — |
| `4cd854c` | Chat module (task 4.x) | — |
| `21a2b14` | Agent CRUD, duplicate, export/import | — |
| `46f3838` | File module: upload/download/token/cleanup | +652 / — |

---

**Last Updated**: 2026-05-04  
**Total Commits**: 26  
**Unit Tests**: 236 passing  
**Controllers**: 6 (Agent, Chat, Skill, MCP, Knowledge, File)  
**Services**: 7 (Agent, ChatSession, Skill, MCP, KnowledgeBase, File, FileCleaner)  
**Design Components**: 17 reusable (agent-platform-ui.pen)
