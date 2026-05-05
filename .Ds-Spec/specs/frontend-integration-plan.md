---
name: Frontend Integration Plan
overview: A phased plan to complete frontend-backend integration for all modules (Skill, MCP, Knowledge Base, File, Market, Model, Chat enhancements) and close the 43 design-to-implementation gaps identified in the gap analysis.
todos:
  - id: phase-1-types
    content: "Phase 1.1-1.3: Add all missing VO/DTO types to api/types.ts, create 6 new API service modules (skill, mcp, knowledge, file, market, model), extend del() in client.ts"
    status: completed
  - id: phase-2-skill
    content: "Phase 2.1: Wire SkillListPage, SkillCreatePage, SkillMarketPage to real Skill API"
    status: completed
  - id: phase-2-mcp
    content: "Phase 2.2: Wire MCPListPage, MCPAddPage, MCPMarketPage to real MCP API"
    status: completed
  - id: phase-2-kb
    content: "Phase 2.3: Wire KnowledgeBasePage, KBDocumentsPage to real Knowledge Base API"
    status: completed
  - id: phase-2-model-dash-file
    content: "Phase 2.4-2.6: Wire model selector, dashboard stats/featured, file upload"
    status: completed
  - id: phase-3-p0
    content: "Phase 3.1: Fix P0 issues -- responsive layout, delete conversation, built-in tools model, publish action"
    status: completed
  - id: phase-3-p1
    content: "Phase 3.2: Fix P1 issues -- icons, filter dropdowns, max steps, add modal, tabs, test connection, message actions"
    status: completed
  - id: phase-4-p2p3
    content: "Phase 4: Fix P2+P3 design gaps (session preview, tool call cards, context panel, column names, etc.)"
    status: completed
  - id: phase-5-cleanup
    content: "Phase 5: Remove MSW mocks, run integration tests against live backend"
    status: in_progress
isProject: false
---

# Frontend Integration Plan

## Current State

**Backend**: All 13 tasks complete (295 unit tests passing). 9 REST controllers expose 60+ endpoints across Agent, Chat, Skill, MCP, Knowledge Base, File, Model, and Market modules.

**Frontend**: React 18 + TypeScript + Vite 5 + Tailwind CSS. 22 page components exist. API integration is complete only for **Agent** and **Chat** modules. All other pages (Skill, MCP, Knowledge Base, Market, Dashboard stats) use **hardcoded mock data**.

**API layer**: Only `api/agent.ts` and `api/chat.ts` exist. Missing: `api/skill.ts`, `api/mcp.ts`, `api/knowledge.ts`, `api/file.ts`, `api/market.ts`, `api/model.ts`. Types in `api/types.ts` only cover Agent and Chat VOs.

**Gap analysis**: 43 issues identified in [feedback.md](.Ds-Spec/specs/ui-design-implementation-gap-analysis/feedback.md) (4 P0, 9 P1, 16 P2, 14 P3).

---

## Architecture Decisions

- **State management**: Use React hooks + context (no Redux). Each page manages its own data fetching with `useEffect` + local state, consistent with existing `AgentListPage` / `ChatPage` patterns.
- **SSE**: Keep existing `parseSseStream` async generator in `api/chat.ts` -- already production-ready.
- **MSW removal**: Remove MSW mock handlers as real API integration replaces them. Keep MSW only for future unit tests.
- **Error handling**: Leverage existing `ApiError` class and show Toast/error states. No new error framework.
- **No new dependencies**: Use existing stack (lucide-react, react-router-dom, tailwind). Add only what is strictly needed (e.g., a lightweight toast library if none exists).

---

## Phase 1: API Layer + Types (Foundation)

Complete the TypeScript API layer so all pages can call real endpoints.

### 1.1 Extend `api/types.ts` with missing VO/DTO types

Add types for all remaining modules. Derive field names from backend Java DTOs:

- **Skill**: `SkillCreateRequest`, `SkillUpdateRequest`, `SkillDetailVO`, `SkillSummaryVO`
- **MCP**: `McpCreateRequest`, `McpUpdateRequest`, `McpDetailVO`, `McpSummaryVO`
- **Knowledge Base**: `KnowledgeBaseCreateRequest`, `KnowledgeBaseUpdateRequest`, `KnowledgeBaseDetailVO`, `KnowledgeBaseSummaryVO`, `KbDocumentVO`, `KbSearchRequest`, `KbSearchResult`
- **File**: `FileVO`, `FileDownloadTokenVO`
- **Market**: `PublishRequest`, `VisibilityUpdateRequest`, `ReviewCreateRequest`, `MarketItemVO`, `MarketItemDetailVO`, `ReviewVO`
- **Model**: `BuiltinModelVO`, `CustomModelVO`, `CustomModelCreateRequest`, `CustomModelUpdateRequest`, `ModelInfo`

### 1.2 Create API service modules

Create one file per backend module, following the pattern in `api/agent.ts`:

- `**api/skill.ts` -- CRUD + export (6 endpoints)
- `**api/mcp.ts` -- CRUD + toggle + test + discover + export (9 endpoints)
- `**api/knowledge.ts` -- CRUD + document ops + search (9 endpoints)
- `**api/file.ts` -- upload + download/preview token (4 endpoints)
- `**api/market.ts` -- publish + browse + favorites + reviews + import (10 endpoints)
- `**api/model.ts` -- builtin + custom CRUD + list all (6 endpoints)

### 1.3 Add `del` with params support in `api/client.ts`

Some DELETE endpoints need query params (e.g., `?force=true`). Extend `del()` to accept optional params.

---

## Phase 2: Core Page Integration (Replace Mock Data)

Wire each page to real backend APIs. Priority order based on user-facing value.

### 2.1 Skill Module

- `**SkillListPage.tsx`: Replace `skillsData` hardcoded array with `listSkills()` API call. Add search, pagination, status tabs, delete/export actions.
- `**SkillCreatePage.tsx`: Wire `createSkill()` / `updateSkill()` API. Add edit mode (load skill by ID via route param).
- `**SkillMarketPage.tsx`: Wire `listMarketItems(type='SKILL')` + `importItem()`.

### 2.2 MCP Module

- `**MCPListPage.tsx`: Replace `mcpData` with `listMcps()`. Wire toggle (PUT `/{id}/toggle`), test connection (POST `/{id}/test`), delete.
- `**MCPAddPage.tsx`: Wire `createMcp()` + test connection. Replace market mock data with `listMarketItems(type='MCP')`.
- `**MCPMarketPage.tsx`: Wire `listMarketItems(type='MCP')` + `importItem()`.

### 2.3 Knowledge Base Module

- `**KnowledgeBasePage.tsx`: Replace `kbData` with `listKnowledgeBases()`. Wire create, delete, search.
- `**KBDocumentsPage.tsx`: Wire `listDocuments()`, `uploadDocument()`, `deleteDocument()`, `reindexDocument()`.

### 2.4 Model Selector

- Wire `listAll()` from `api/model.ts` into `AgentCreatePage` and `AgentEditPage` model dropdown (replacing any hardcoded options).

### 2.5 Dashboard

- `**DashboardPage.tsx`: Wire "recent agents" to `listAgents(sort_by=updated_at, page_size=4)`. Wire stats counters from actual list totals. Wire "market featured" to `getFeatured()`.

### 2.6 File Upload

- Wire `upload()` from `api/file.ts` into ChatPage attachment flow and Knowledge Base document upload.

---

## Phase 3: Design Gap Closure (P0 + P1 Issues)

Address the 13 highest-priority issues from the gap analysis.

### 3.1 P0 -- Critical (4 items)

- **O1 - Responsive layout**: Remove all fixed `w-[1440px]` / `w-[260px]` widths. Use `max-w-`, `flex-grow`, responsive breakpoints. Sidebar: collapsible to 56px icon bar.
- **C1 - Delete conversation**: Add delete action to `SessionItem` (hover X icon or context menu). Wire `DELETE /chat/sessions/{id}` (add endpoint to `api/chat.ts` if missing from backend, or use existing endpoint).
- **T1 - Built-in tools model**: `AgentCreateToolsPage` should distinguish built-in tools, Skills, MCP tools, and Knowledge Bases. Show actual tool sources from ToolRegistry.
- **B5 - Publish action**: Make the "发布" button trigger a publish dialog (visibility + version + release notes), not `handleSaveDraft`.

### 3.2 P1 -- High (9 items)

- **O2 - Icons**: Replace `agent.name[0]` text avatars with Lucide icons (`Bot`, `Wand2`, `Plug`, `BookOpen`) everywhere (`AssetCard`, `SessionItem`, dashboard cards, skill cards, KB cards).
- **A1 - Agent card icons**: Use `Bot` icon in brand-colored box.
- **A2 - Filter dropdowns**: Make "筛选" functional (filter by status, model, date range). Remove non-functional "更新时间" dropdown.
- **B1 - Max steps field**: Replace "当前步骤" read-only field with editable "最大步骤数" numeric input.
- **T2 - Single add modal**: Replace per-section "添加" links with one "添加" button opening a modal with 4 category tabs (built-in, Skill, MCP, Knowledge Base).
- **S1 - Skill tabs**: Add status tabs (全部/草稿/已发布) to `SkillListPage`, matching `AgentListPage` pattern.
- **S2 - Skill card icons**: Use Lucide `Wand2` icon instead of text chars.
- **M1 - Test connection**: Add "测试连接" button to each MCP row, wire POST `/{id}/test`.
- **C3 - Message actions**: Add "复制", "重新生成" action buttons below each AI message.

---

## Phase 4: Design Gap Closure (P2 + P3 Issues)

### 4.1 P2 -- Medium (16 items)

- **C2**: Session list shows last message preview instead of agent name.
- **C4**: Input area matches design (labeled buttons, "清空" text button).
- **C6**: Tool call cards with header, status icon, expandable JSON.
- **C7**: Context panel: add tool execution history + reference sources sections.
- **T3**: Add remove/delete icon alongside toggle for tool items.
- **T5**: Add colored icons per tool category (database, plug, book-open).
- **S3**: Card actions as text links instead of buttons.
- **S5**: Add search box to `SkillListPage`.
- **S7**: Replace hardcoded Skill mock data (covered in Phase 2.1).
- **M3**: Fix column names (协议 not 类型, add server URL).
- **M5**: Add enable/disable toggle per MCP row.
- **M8**: Replace hardcoded MCP mock data (covered in Phase 2.2).
- **D1**: Dashboard agent card icons (covered in P1).
- **D2**: `AssetCard` responsive width (flex-1 instead of `w-[300px]`).
- **SC1**: `SkillCreatePage` top bar consistent with other create pages.
- **K2**: Replace hardcoded KB mock data (covered in Phase 2.3).

### 4.2 P3 -- Low (14 items)

- C5 (user avatar color), A3 (tab keys), A4 (emoji removal), B2 (avatar picker), B3 (insert variable button), T4 (count badges), T6 (MCP tool list), T7 (KB stats), S4 (create placeholder card), S6 (remove 关联智能体), M4 (dot status), M6 (server URL), K1 (BookOpen icon), AD1 (debug mock data).

---

## Phase 5: MSW Cleanup + Testing

### 5.1 Remove MSW mock handlers

- Delete `frontend/src/mocks/handlers/agent.ts`, `chat.ts`, `index.ts`
- Delete `frontend/src/mocks/data.ts`, `browser.ts`
- Remove MSW initialization from `main.tsx`
- Remove `public/mockServiceWorker.js`
- Remove `msw` from `package.json` dependencies

### 5.2 Integration testing

- Verify each page loads correctly with backend running (`./mvnw spring-boot:run -pl ap-app`)
- Test all CRUD flows end-to-end through the UI
- Test SSE chat streaming with real LLM calls
- Test file upload/download flows

---

## File Map

| New/Modified File                             | Phase    | Purpose                                  |
| --------------------------------------------- | -------- | ---------------------------------------- |
| `frontend/src/api/types.ts`                   | 1.1      | Add Skill/MCP/KB/File/Market/Model types |
| `frontend/src/api/skill.ts`                   | 1.2      | Skill API service (new)                  |
| `frontend/src/api/mcp.ts`                     | 1.2      | MCP API service (new)                    |
| `frontend/src/api/knowledge.ts`               | 1.2      | Knowledge Base API service (new)         |
| `frontend/src/api/file.ts`                    | 1.2      | File API service (new)                   |
| `frontend/src/api/market.ts`                  | 1.2      | Market API service (new)                 |
| `frontend/src/api/model.ts`                   | 1.2      | Model API service (new)                  |
| `frontend/src/api/client.ts`                  | 1.3      | Extend `del()` with params               |
| `frontend/src/pages/SkillListPage.tsx`        | 2.1, 3.2 | API integration + tabs + icons           |
| `frontend/src/pages/SkillCreatePage.tsx`      | 2.1      | API integration + edit mode              |
| `frontend/src/pages/SkillMarketPage.tsx`      | 2.1      | API integration                          |
| `frontend/src/pages/MCPListPage.tsx`          | 2.2, 3.2 | API integration + toggle + test          |
| `frontend/src/pages/MCPAddPage.tsx`           | 2.2      | API integration                          |
| `frontend/src/pages/MCPMarketPage.tsx`        | 2.2      | API integration                          |
| `frontend/src/pages/KnowledgeBasePage.tsx`    | 2.3      | API integration                          |
| `frontend/src/pages/KBDocumentsPage.tsx`      | 2.3      | API integration                          |
| `frontend/src/pages/DashboardPage.tsx`        | 2.5      | API integration                          |
| `frontend/src/pages/AgentCreatePage.tsx`      | 2.4, 3.1 | Model selector + publish dialog          |
| `frontend/src/pages/AgentEditPage.tsx`        | 2.4, 3.1 | Model selector + max steps fix           |
| `frontend/src/pages/AgentCreateToolsPage.tsx` | 3.1      | Built-in tools + add modal               |
| `frontend/src/pages/ChatPage.tsx`             | 3.1, 3.2 | Delete session + message actions         |
| `frontend/src/components/ui/AssetCard.tsx`    | 3.2      | Icons + responsive width                 |
| `frontend/src/components/layout/Sidebar.tsx`  | 3.1      | Collapsible responsive                   |
| `frontend/src/components/layout/Layout.tsx`   | 3.1      | Responsive shell                         |

---

## Execution Order

```
Phase 1 (API layer)     ──▶ Phase 2 (page integration)  ──▶ Phase 3 (P0/P1 gaps)
                                                          ──▶ Phase 4 (P2/P3 gaps)
                                                          ──▶ Phase 5 (cleanup)
```

Phase 1 must complete first. Phases 2 and 3 can partially overlap (e.g., integrate a page then fix its design gaps). Phase 4 and 5 are independent and can run in parallel.

---

## Verification

After each phase:

- `npm run build` -- zero TypeScript errors
- `npm run lint` -- zero lint errors
- Manual testing against running backend (`./mvnw spring-boot:run -pl ap-app -Dspring-boot.run.profiles=local`)
- Each page: verify loading state, data state, empty state, error state
