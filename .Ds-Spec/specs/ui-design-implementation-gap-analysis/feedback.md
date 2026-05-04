# UI Design-to-Implementation Gap Analysis

**Date:** 2026-05-04
**Design file:** `design/agent-platform-ui.pen`
**Implementation:** `frontend/src/pages/`, `frontend/src/components/`

---

## Overall / Cross-Cutting Issues

### O1. No Responsive Design
- **Current:** All pages use fixed pixel widths (e.g., `w-[1440px]`, `w-[260px]`). The design file also uses fixed 1440×900 artboards, but the implementation should be fluid for different screen sizes.
- **Expected:** Layouts should adapt to viewport width using responsive breakpoints (flex-grow, max-width, etc.). Sidebar, content areas, and cards should reflow on narrower screens.
- **Severity:** High — blocks use on laptops/tablets.

### O2. Avatar/Icons Use Text Instead of Proper Icons
- **Current:** Many places display `agent.name[0]` or `iconText` as a plain text character in a colored box (e.g., AssetCard, SessionItem, Skill cards, KnowledgeBase cards).
- **Design:** Uses Lucide icons everywhere — `bot`, `wand`, `plug`, `book-open`, `database`, etc.
- **Affected pages:** AgentListPage, SkillListPage, KnowledgeBasePage, DashboardPage, AgentDetailPage, ChatPage (context panel).
- **Severity:** Medium — visual quality regression.

### O3. Emoji Usage in UI
- **Current:** AssetCard uses emoji (🔧, 🧩) for tool count and collaboration count metadata.
- **Design:** Uses clean text labels without emoji.
- **Severity:** Low — inconsistent with design system tone.

### O4. Font Consistency
- **Design:** Uses Inter font family consistently with specific weights (400 regular, 500 medium, 600 semibold, 700 bold).
- **Current:** Tailwind config may not load Inter. Verify `fontFamily` configuration.
- **Severity:** Low.

---

## Chat/Conversation Page (`ChatPage.tsx` vs P15-Chat)

### C1. Cannot Delete Conversations
- **Current:** Session items in the sidebar (`SessionItem` component, line 76) have no delete/remove action. No right-click menu, no hover delete button.
- **Design:** Not explicitly shown in design, but expected UX for a chat application. Each session item should support deletion via an `X` icon or context menu.
- **Severity:** High — core session management is incomplete.

### C2. Session List Layout Mismatch
- **Current:** Each `SessionItem` shows title + agent name + time in a compact row (ChatPage.tsx:76-90).
- **Design:** Each session item in the design (e.g., `si1` node `Mb80a`) shows title + a one-line preview of the last message ("好的，让我帮你分析..."). No agent name is shown per item.
- **Severity:** Medium — information hierarchy differs from design.

### C3. Missing Message Action Buttons
- **Current:** AI messages only show content. No action buttons below the response.
- **Design:** Below each AI response, there are action chips: `复制` · `分享` · `重新生成` (nodes `7xKbW`, `ADx31`, `d7INv`, `Btrgp` inside `msgActions` frame).
- **Severity:** Medium — missing key user interactions.

### C4. Input Area Differences
- **Current:** Paperclip button → textarea + clear X button inside → send button (ChatPage.tsx:597-650). Clear button is an X inside the textarea.
- **Design:** Attachment button ("附件") → text input → "清空" text button → "发送" primary button, all inside a white card with border (`1uW7a` inputBox).
- **Key differences:**
  - Design has an explicit "附件" labeled button, implementation only shows a paperclip icon.
  - Design has "清空" as a visible text button, implementation has an X icon inside the textarea.
  - Design send button includes both text "发送" and a `send` icon. Implementation is icon-only.
- **Severity:** Medium.

### C5. User Avatar Color
- **Current:** User avatar background is `bg-gray-200` (ChatPage.tsx:154).
- **Design:** User avatar uses `fill: $gray-700` (node `e5mMb`).
- **Severity:** Low.

### C6. Missing Tool Call Inline Display in Messages
- **Current:** Tool calls shown as a small inline text (`调用工具: tool_name` or `工具结果: ...` in ChatPage.tsx:141-149).
- **Design:** Tool calls shown in a distinct gray card with a header row ("调用 query_sales_data") and result summary, styled as a bordered box inside the message (node `ANg7j`).
- **Severity:** Medium — reduces clarity of tool execution visibility.

### C7. Context Panel Simplified
- **Current:** Context panel shows: current agent info + session info + "查看智能体详情" button (ChatPage.tsx:655-715).
- **Design:** Context panel has 3 sections: (1) "当前使用 Agent" with agent info, (2) "工具执行" with tool execution log list, (3) "引用来源" with reference links.
- **Missing:** Tool execution history panel and reference sources section.
- **Severity:** Medium.

---

## Agent List Page (`AgentListPage.tsx` vs P04-AgentList)

### A1. Agent Avatar/Card Icon Shows Text Instead of Icon
- **Current:** `AssetCard` receives `iconText={agent.avatar ?? agent.name[0]}` — displays the first character of the agent's name as a text glyph (AgentListPage.tsx:205).
- **Design:** Cards use a Lucide `bot` icon in a colored box (e.g., `$brand-50` background with `$brand-500` icon).
- **Severity:** High — visual mismatch, text overflow possible for long names.

### A2. Filter Dropdown: Remove "更新时间", Keep Only "筛选" with Working Functionality
- **Current:** Two static button-like dropdowns: "筛选" and "更新时间" (AgentListPage.tsx:171-178). Neither has any functional dropdown or filtering logic.
- **Expected:** Keep only "筛选" dropdown with functional options (e.g., filter by status, date range, model type).
- **Design:** Shows similar filter dropdowns in the tab/filter row (`5pWK4` frame).
- **Severity:** High — non-functional UI elements.

### A3. Tabs Label Mismatch
- **Current:** Tab labels are: 全部 / 草稿 / 已发布 / 已归档 with key names `all | created | published | collab` (AgentListPage.tsx:104-109).
- **Design:** The `tabs` row in the design shows pill-style tab buttons.
- **Issue:** Tab key 'created' for 草稿 and 'collab' for 已归档 are semantically misleading. "Created" should map to "我创建的" and "Collab" should not map to "已归档".
- **Severity:** Low.

### A4. Card Metadata Uses Emoji
- **Current:** `AssetCard` renders: `🔧 工具 {toolCount}  🧩 协作 {collabCount} · {updatedAt}` (AssetCard.tsx:54-55).
- **Design:** Clean text without emoji glyphs.
- **Severity:** Low.

---

## Agent Create/Edit Page (`AgentCreatePage.tsx`, `AgentEditPage.tsx` vs P05/P05e)

### B1. "当前步骤" Field Should Be "最大步骤数"
- **Current:** The second field in the two-column row shows "当前步骤" with a read-only value "基本信息" (AgentEditPage.tsx:247-249, AgentCreatePage.tsx:217-222).
- **Design:** The corresponding field is labeled "最大步骤数" and is a numeric input with value "10" (`stepF` containing `stepIn` with content "10").
- **Expected:** Replace the read-only "当前步骤" field with an editable "最大步骤数" (max steps) numeric input. This configures how many reasoning steps the agent can take per turn.
- **Severity:** High — wrong configuration field exposed.

### B2. Avatar Picker Approach Differs
- **Current:** 4-icon grid picker (Bot, Sparkles, Zap, MessageSquare) (AgentCreatePage.tsx:12-17, lines 158-176).
- **Design:** Shows a large 64×64 avatar preview with the chosen icon, plus a "更换头像" (change avatar) button next to it. Not a grid of icon choices.
- **Assessment:** The current approach is more user-friendly than the design. Consider keeping the icon grid but ensure the selected icon is previewed prominently.
- **Severity:** Low — design divergence that may actually improve UX.

### B3. Prompt Area Missing "Insert Variable" Button
- **Current:** Prompt textarea has a character counter but no "插入变量" action (AgentEditPage.tsx:253-265).
- **Design:** Next to the "系统提示词" label, there is a "插入变量" button with a `+` icon (`s64C5` frame inside `pHead`). This suggests template variable injection.
- **Severity:** Medium — missing feature.

### B4. Form Uses Grey Background for Prompt Input
- **Current:** Prompt textarea uses `bg-gray-50` (AgentEditPage.tsx:263).
- **Design:** Prompt input also uses `fill: $gray-50` — this matches.
- **Severity:** None — matches.

### B5. TopBar Has Both "保存草稿" and "发布" Buttons
- **Current:** Two buttons: "保存草稿" (secondary) and "发布" (primary) — both call `handleSaveDraft` (AgentCreatePage.tsx:126-131).
- **Design:** "保存草稿" (secondary/outline) and "发布" (primary/filled). This matches the structure.
- **Issue:** Both buttons trigger `handleSaveDraft`. The "发布" button should trigger publish flow.
- **Severity:** High — publish action missing.

---

## Agent Tools Configuration Page (`AgentCreateToolsPage.tsx` vs P05b)

### T1. Tools Should Be Built-in Types, Not User-Created
- **Current:** Lists user-created tools like "代码格式化", "JSON 解析", "Playwright MCP" with enable/disable toggles (AgentCreateToolsPage.tsx:18-25).
- **Expected:** Built-in tool primitives like `web_search`, `ask_question`, `read_file`, `write_file`, `execute_code`. These are the fundamental tools an agent can invoke — not user-authored skills or MCP servers.
- **Design:** Shows Skill items like "query_sales_data" and "generate_chart" — but these appear to be Skill references, not raw built-in tools. The user's requirement is that built-in primitive tools should also be available.
- **Severity:** High — core functionality mismatch.

### T2. Single Add Button → Modal with 4 Categories
- **Current:** Each section (Skill, MCP, KB) has its own "添加 Skill"/"添加 MCP"/"添加知识库" text link at the bottom (AgentCreateToolsPage.tsx:149-152).
- **Expected:** A single "添加" button that opens a modal. The modal supports searching across 4 categories simultaneously: **内置工具, Skill, MCP, 知识库**. User can select items from any/all categories in one flow.
- **Design:** Has per-section "添加 Skill" / "添加 MCP" / "关联知识库" buttons. But user requirement overrides design here — consolidated add with modal.
- **Severity:** High — UX flow completely different.

### T3. Toggle Switches vs Delete Buttons
- **Current:** Each tool item has a toggle switch to enable/disable (AgentCreateToolsPage.tsx:87-100).
- **Design:** Each item has a trash/delete icon (`trash-2`) to remove the tool from the agent's configuration.
- **Expected:** The enable/disable toggle may still be useful, but the design shows remove capability. Consider having both.
- **Severity:** Medium.

### T4. Missing Count Badges Per Section
- **Current:** Section headers show only icon + label (e.g., "Skill", "MCP 服务", "知识库").
- **Design:** Each section header includes a count badge: "2 个已添加", "1 个已添加", "1 个已引用" (nodes `G5ESf`, `Bxi55`, `A6Ioy`).
- **Severity:** Low.

### T5. Tool Items Missing Proper Icons
- **Current:** Tool items show only text (name + description). No icon per tool.
- **Design:** Each tool item has a left-side icon in a colored box (e.g., `database` icon in brand-50 box for skills, `plug` icon in success-50 for MCP, `book-open` in warning-50 for knowledge base).
- **Severity:** Medium — visual hierarchy weaker.

### T6. MCP Items Show Enumerated Tools
- **Current:** MCP items show description text from tool definition.
- **Design:** MCP items show a list of tools provided: "添加了 3 个工具：query_sales_data · list_tables · describe_table" (node `K7qTw`).
- **Severity:** Low.

### T7. Knowledge Base Items Show Stats
- **Current:** KB items show description text.
- **Design:** KB items show document count, size, and last index date: "412 文档 · 18.2MB · 上次重建索引 2026-04-19" (node `SLbHr`).
- **Severity:** Low.

---

## Skill List Page (`SkillListPage.tsx` vs P09-SkillList)

### S1. Missing Tabs for Status Filtering (已发布 / 草稿)
- **Current:** No tabs. All skills shown in one grid (SkillListPage.tsx:127-176).
- **Expected:** Tab bar with two options: **已发布** and **草稿**. This provides quick filtering by publish status.
- **Design:** The design does not show tabs, but the user is specifically requesting this feature. The AgentListPage already implements tabs — reuse the same pattern.
- **Severity:** High — missing core filtering.

### S2. Skill Card Avatars Use Text Instead of Icons
- **Current:** Each card renders `iconText` as a single character (代, 反, A, 测, G) in a colored background (SkillListPage.tsx:90-92).
- **Design:** Each card has a proper Lucide icon (wand, database, file-text, mail, shield) in a colored rounded box.
- **Severity:** High — visual quality regression.

### S3. Card Actions Differ
- **Current:** "使用" and "编辑" buttons at the bottom (SkillListPage.tsx:115-116).
- **Design:** Text-link style actions: "编辑 · 测试 · 复制" on a divider line. No primary-colored buttons — all are inline text links in brand-500 color.
- **Severity:** Medium.

### S4. Missing "Create New Skill" Placeholder Card
- **Current:** The grid only shows existing skill cards.
- **Design:** The last position in the second row is a placeholder card with a `+` icon and "创建新 Skill" text (node `f7Ira`). This serves as an inline creation affordance.
- **Severity:** Low.

### S5. Missing Search Box
- **Current:** No search input on the Skill List page.
- **Design:** Top row includes a search box (`srch` frame `g2UKF`) next to the title.
- **Severity:** Medium.

### S6. "关联智能体" Field Not in Design
- **Current:** Cards show "关联智能体: {agentCount}" in metadata (SkillListPage.tsx:106).
- **Design:** Cards show type (Prompt 型 / Chain 型) and last updated time. No "关联智能体" count.
- **Severity:** Low.

### S7. Hardcoded Mock Data
- **Current:** `skillsData` is a static array hardcoded in the component (SkillListPage.tsx:9-60).
- **Expected:** Should fetch from API like AgentListPage does.
- **Severity:** Medium — ready for API integration.

---

## MCP List Page (`MCPListPage.tsx` vs P12-MCPList)

### M1. Missing "Test Connection" Button
- **Current:** Each row has Edit and Delete icon buttons only (MCPListPage.tsx:127-141).
- **Design:** Each row has three actions: **"测试连接"** (gray pill button), **"编辑"** (gray pill button), and a **toggle switch** (enable/disable). See nodes `v0sJS`, `UZR2P`, `eRfLg` in `trow1`.
- **Severity:** High — critical operational feature missing.

### M2. Table Headers Should Support Filtering
- **Current:** Static text headers (名称, 类型, 状态, 工具数, 操作) — MCPListPage.tsx:90-95.
- **Expected:** Clickable headers that allow sorting/filtering by column values (e.g., filter by status: connected/disconnected/error, filter by protocol type).
- **Design:** Table headers are also static in the design, but the user is requesting filterable headers.
- **Severity:** Medium.

### M3. Column Definitions Mismatch
- **Current columns:** 名称 | 类型 | 状态 | 工具数 | 操作
- **Design columns:** 服务器名称 | 状态 | 协议 | 工具 | 操作
- **Differences:**
  - "类型" (npm/stdio/http) → should be "协议" (SSE/Streamable HTTP)
  - Missing server address/URL below server name
- **Severity:** Medium.

### M4. Status Display Uses Badges Instead of Dot + Text
- **Current:** `Badge` component shows "已连接" (green), "未连接" (gray), "错误" (red) (MCPListPage.tsx:51-62).
- **Design:** Colored dot (8×8 circle) + text: green dot + "在线", gray dot + "离线", red dot + "错误".
- **Severity:** Low.

### M5. Missing Toggle Switch Per Row
- **Current:** No enable/disable toggle per MCP server.
- **Design:** Each row has a toggle switch (pill-shaped, brand-500 when active) at the end of the actions column.
- **Severity:** Medium — cannot disable a server without deleting it.

### M6. Server Address Not Shown
- **Current:** Only server name shown in the name column.
- **Design:** Server name + server address URL shown together (e.g., "GitHub MCP" + "github-mcp.internal:8080" in smaller gray text).
- **Severity:** Low.

### M7. Summary Footer Not in Design
- **Current:** Shows "共 X 个 MCP Server · Y 个已连接 · Z 个可用工具" (MCPListPage.tsx:162-165).
- **Design:** No summary footer present.
- **Assessment:** Useful addition, keep it.
- **Severity:** None — implementation enhancement.

### M8. Hardcoded Mock Data
- **Current:** `mcpData` is a static array (MCPListPage.tsx:9-45).
- **Expected:** Should fetch from API.
- **Severity:** Medium.

---

## Dashboard Page (`DashboardPage.tsx` vs P02-Dashboard)

### D1. Agent Cards in Dashboard Use Text Avatars
- **Current:** `AssetCard` with `iconText={agent.name[0]}` (DashboardPage.tsx:195).
- **Design:** Cards use proper Lucide icons.
- **Severity:** Medium.

### D2. AssetCard Width Fixed to 300px
- **Current:** `AssetCard` has `w-[300px]` fixed width (AssetCard.tsx:34).
- **Design:** Cards in dashboard are flex-1 within a flex row, allowing them to fill available space.
- **Severity:** Medium.

---

## Skill Create Page (`SkillCreatePage.tsx` vs P10-SkillCreate)

### SC1. TopBar Layout Differs
- **Current:** TopBar has name input on left + save button on right (SkillCreatePage.tsx:43-53).
- **Design:** Should follow the same pattern as AgentCreatePage: breadcrumb + back button + title + action buttons. The current layout is inconsistent with other create/edit pages.
- **Severity:** Medium.

### SC2. Code Editor is Primary Content
- **Current:** Code editor occupies the main left area with a preview panel on the right (SkillCreatePage.tsx:56-189).
- **Design:** Should be verified against the design file P10.
- **Severity:** Low — needs design verification.

---

## MCP Add Page (`MCPAddPage.tsx` vs P13-MCPAdd)

### MA1. Design Match — Minor Differences
- **Current:** Tab switcher (从市场安装 / 手动配置), market grid, JSON editor with validation (MCPAddPage.tsx).
- **Design:** Matches the general structure well.
- **Issues:**
  - Market cards show "下载 X" count but design may show different metadata.
  - Hardcoded market data should be API-driven.
- **Severity:** Low.

---

## Agent Debug Page (`AgentDebugPage.tsx` vs P06-AgentDebug)

### AD1. Design Structure Match — Minor Issues
- **Current:** Debug chat + monitor panel layout is consistent with the design.
- **Issues:**
  - Mock data hardcoded (messages, tool calls, metrics).
  - Chart placeholder ("图表占位") in monitor panel should be a real chart.
  - Warning banner color: `bg-warning-50 border-warning-500` — verify against design.
- **Severity:** Low.

---

## Knowledge Base Page (`KnowledgeBasePage.tsx` vs P16-KnowledgeBase)

### K1. Cards Use Folder Icon Instead of Custom Icons
- **Current:** All KB cards use `Folder` icon (KnowledgeBasePage.tsx:70).
- **Design:** Each card should use `BookOpen` icon or a custom icon per knowledge base.
- **Severity:** Low.

### K2. Hardcoded Mock Data
- **Current:** `kbData` static array (KnowledgeBasePage.tsx:8-49).
- **Expected:** API-driven data.
- **Severity:** Medium.

---

## Summary: Priority Matrix

| Priority | Count | Key Items |
|----------|-------|-----------|
| **P0 — Critical/Blocking** | 4 | O1 (responsive), C1 (delete conversation), T1 (built-in tools), B5 (publish action) |
| **P1 — High** | 9 | A2 (filter dropdowns), B1 (max steps field), T2 (add modal), S1 (skill tabs), M1 (test connection), A1 (agent card icons), S2 (skill card icons), C3 (message actions), O2 (avatar icons) |
| **P2 — Medium** | 16 | C2, C4, C6, C7, T3, T5, S3, S5, M3, M5, D1, D2, SC1, K2, S7, M8 |
| **P3 — Low** | 14 | C5, A3, A4, B2, B3, T4, T6, T7, S4, S6, M4, M6, K1, AD1, MA1 |

**Total issues identified:** 43
