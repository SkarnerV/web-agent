# Chat Page UI/UX Playwright 测试指南

> 面向 Agent 的自动化浏览器测试手册 — 使用 Playwright MCP 打开 `/chat` 页面，模拟用户操作，发现 UI/UX Bug。

---

## 目录

1. [测试环境与前置条件](#1-测试环境与前置条件)
2. [测试架构概览](#2-测试架构概览)
3. [类别 A：UI 显示正确性](#3-类别-ahui-显示正确性)
4. [类别 B：功能完整性](#4-类别-b功能完整性)
5. [类别 C：刷新与数据持久化](#5-类别-c刷新与数据持久化)
6. [Bug 报告模板](#6-bug-报告模板)

---

## 1. 测试环境与前置条件

### 1.1 环境要求

| 项目 | 说明 |
|------|------|
| 前端地址 | `http://localhost:3000` (Vite dev server) |
| 后端地址 | `http://localhost:8080` (Spring Boot) |
| 基础设施 | PostgreSQL + Redis + RabbitMQ + MinIO (docker-compose) |
| 浏览器 | Chromium (Playwright 默认) |
| 视口 | Desktop Chrome 1280×720 (默认), 也需测试 1920×1080, 375×812 (移动端) |

### 1.2 前置数据

在运行测试前，确保存在至少 **一个已发布的 Agent**：

```bash
# 检查是否有可用的 Agent
curl -s http://localhost:3000/api/v1/agents?page_size=1 | jq '.data.data | length'

# 如果没有，创建一个
curl -X POST http://localhost:3000/api/v1/agents \
  -H 'Content-Type: application/json' \
  -d '{"name":"测试智能体","maxSteps":10,"systemPrompt":"你是一个友好的测试助手。"}'
```

### 1.3 Playwright 配置参考

```typescript
// playwright.config.ts 关键配置
{
  testDir: './e2e',
  fullyParallel: false,
  timeout: 30000,
  use: {
    baseURL: 'http://localhost:3000',
    headless: true,
  },
}
```

---

## 2. 测试架构概览

### 2.1 Chat 页面组件树

```
ChatPage (h-screen w-screen)
├── Sidebar (导航栏，左侧 56px)
├── Session List (左侧面板，240px, 隐藏于 md 以下)
│   ├── 新建对话按钮
│   ├── 搜索对话输入框
│   └── 对话分组 (今天/昨天/更早)
│       └── SessionItem × N (含删除按钮)
├── Chat Area (中间主区域)
│   ├── Top Bar (h-14)
│   │   ├── Agent 选择器 (Pill 样式下拉)
│   │   ├── 会话标题
│   │   └── 右侧按钮 (上下文面板切换 / 更多)
│   ├── Message Area (flex-1 overflow-y-auto)
│   │   ├── 空状态 ("选择一个会话或创建新对话")
│   │   └── MessageBubble × N
│   │       ├── 用户消息 (右对齐, bg-brand-50)
│   │       ├── 助手消息 (左对齐, 白色边框)
│   │       │   ├── ToolCallCard × N (可折叠)
│   │       │   ├── 文本内容 (含流式光标)
│   │       │   └── QuestionCard × N (提问卡片)
│   │       ├── 工具消息 (居中, 灰色)
│   │       └── 操作按钮 (复制/重新生成)
│   └── Input Area (底部固定)
│       ├── 附件按钮
│       ├── 文本输入框 (Enter 发送)
│       ├── 清空按钮
│       └── 发送按钮
└── Context Panel (右侧面板, 300px, 可选)
    ├── 当前智能体信息
    ├── 会话信息
    ├── TodoPanel (Agent 任务清单)
    ├── 等待用户选择 (pending question)
    ├── 工具执行列表
    └── 引用来源列表
```

### 2.2 SSE 事件流与 UI 状态映射

| SSE Event | UI 变化 |
|-----------|---------|
| `message_start` | 创建 assistant 占位消息，状态 → STREAMING |
| `token` | 追加文本 delta 到 assistant 消息 |
| `tool_call_start` | 创建 ToolCallCard (status: running, 动画旋转) |
| `tool_call_end` (success) | ToolCallCard status → success (绿色勾) |
| `tool_call_end` (failed) | ToolCallCard status → failed (红色叉) |
| `tool_call_end` (requires_action) | ToolCallCard status → requires_action (黄色问号) |
| `todo_updated` | 更新 TodoPanel / Context Panel 的 TodoPanel |
| `question` | 创建 QuestionCard (黄色边框) |
| `step_limit` | 显示步数限制提示 (可继续执行) |
| `message_end` | 状态 → COMPLETED，显示复制/重新生成按钮 |
| `error` (recoverable=false) | 状态 → FAILED，显示错误信息 |
| `heartbeat` | 无 UI 变化 (保持连接) |

---

## 3. 类别 A：UI 显示正确性

### A.1 页面加载与布局

#### A.1.1 初次加载空状态
```
测试用例: Chat 页面初次加载（无会话）
前置条件: 数据库无任何 chat session
步骤:
  1. 导航到 /chat
  2. 等待 networkidle
验证点:
  - 页面标题/favicon 正确
  - Sidebar 显示完整导航项
  - Session List 显示 "暂无对话，点击上方按钮开始"
  - Chat Area 显示空状态图标 + "选择一个会话或创建新对话"
  - 输入框 placeholder 为 "选择一个会话或创建新对话开始聊天"
  - 发送按钮处于禁用状态 (灰色)
  - Context Panel 在 >= 1280px 时默认显示
  - 无水平滚动条 (h-screen w-screen 布局正确)
```

#### A.1.2 响应式布局测试
```
测试用例: 不同视口下的布局正确性
步骤:
  1. 设置视口 1920×1080 → 检查 Session List 和 Context Panel 同时可见
  2. 设置视口 1280×720  → 检查 Context Panel 可见
  3. 设置视口 1024×768  → 检查 Session List 隐藏 (md:flex)，Context Panel 隐藏
  4. 设置视口 375×812   → 检查所有元素无溢出，输入框可用
验证点:
  - 元素不重叠 (使用 getBoundingClientRect 检测重叠)
  - 文字不截断 (或使用省略号 truncate)
  - 无水平溢出 (document.documentElement.scrollWidth <= window.innerWidth)
  - Agent 选择器文字在窄屏时截断 (max-w-[180px] truncate)
```

#### A.1.3 颜色与对比度
```
测试用例: 颜色主题与可访问性
验证点:
  - 发送按钮在有效输入时为 bg-brand-500 (品牌色)
  - 发送按钮在无效输入时为 bg-gray-100
  - 用户消息气泡背景为 brand-50
  - 助手消息气泡背景为 white + 灰色边框
  - 错误状态为红色 (text-red-500)
  - 成功状态为绿色 (text-green-500)
  - 进行中状态为 brand-500 + animate-spin
  - 链接/按钮有明确的 focus 样式
```

### A.2 会话列表

#### A.2.1 会话列表显示
```
测试用例: 会话列表包含多条记录
前置条件: 数据库中已有 5+ 个 chat session (含不同时间戳)
步骤:
  1. 导航到 /chat
  2. 等待 sessions API 返回
验证点:
  - 会话按 today/yesterday/earlier 正确分组
  - 每组有标签 "今天" / "昨天" / "更早"
  - 每个 SessionItem 显示:
    - 标题 (或 "新对话")
    - 最后一条消息预览 (lastMessageMap)
    - Agent 名称
    - 格式化时间 (HH:mm)
  - 活跃会话高亮 (bg-brand-50)
  - hover 时显示删除按钮 (✕)
  - 空组不显示 (groups.filter)
```

#### A.2.2 会话搜索
```
测试用例: 搜索会话
步骤:
  1. 在搜索框输入关键词
  2. 观察会话列表变化
验证点:
  - 过滤后仅显示匹配标题的会话
  - 空结果显示空分组 (无 "暂无" 提示 — 已知可能问题)
  - 清除搜索词后恢复完整列表
```

#### A.2.3 会话项键盘导航
```
测试用例: SessionItem 的键盘可访问性
步骤:
  1. Tab 聚焦到某个 SessionItem
  2. 按 Enter → 应加载该会话
  3. 按 Space → 应加载该会话
验证点:
  - role="button", tabIndex=0
  - Enter/Space 触发 onClick
  - 删除按钮在聚焦时可见
```

### A.3 消息区域

#### A.3.1 消息气泡布局
```
测试用例: 不同角色的消息气泡
前置条件: 已有含多种消息的会话 (用户消息、助手回复、工具调用)
步骤:
  1. 点击加载该会话
验证点:
  - 用户消息: 右对齐, max-w-[70%], bg-brand-50, 头像在右侧 (灰色圆形 "我")
  - 助手消息: 左对齐, max-w-[70%], 白色边框, 头像在左侧 (brand-500 圆形)
  - 工具消息: 居中, max-w-[70%], 灰色背景, 扳手图标
  - 助手消息上方显示 agentName
  - 消息时间戳在消息下方 (formatTime HH:mm)
  - 长文本正确换行 (whitespace-pre-wrap)
```

#### A.3.2 ToolCallCard 展开/折叠
```
测试用例: 工具调用卡片的交互
前置条件: 已有含 tool call 的助手消息
步骤:
  1. 找到 ToolCallCard
  2. 检查默认折叠状态 (ChevronRight)
  3. 点击展开 (ChevronDown)
  4. 检查 Arguments 和 Result 显示
验证点:
  - 默认折叠，显示工具名 + 状态图标
  - running 状态有 animate-spin
  - success 状态为绿色 CheckCircle
  - failed 状态为红色 XCircle
  - requires_action 状态为黄色 HelpCircle
  - 展开后 Arguments 为格式化的 JSON
  - 展开后 Result 显示 (如有)
  - JSON 无截断或格式错误 (try/catch fallback)
```

#### A.3.3 QuestionCard 交互
```
测试用例: 提问卡片的显示与回答
前置条件: Agent 触发了 question 工具调用 (pending 状态)
步骤:
  1. 等待 question SSE 事件 → QuestionCard 出现
  2. 检查选项渲染 (带 label 和 description)
  3. 单击单选选项 → 立即触发 onAnswer
  4. (多选) 选择多个选项 → 点击 "提交回答"
  5. 在文本框中输入补充说明 → 点击 "提交回答"
验证点:
  - 卡片为黄色边框 (border-amber-200, bg-amber-50/70)
  - 显示问题标题 "Agent 需要你选择后继续执行"
  - 选中选项高亮 (border-amber-500, bg-white)
  - 未选中选项 (border-amber-100, bg-white/70)
  - 已回答状态显示 "已回答，Agent 正在继续执行。"，所有按钮 disabled
  - allowFreeText 时显示 textarea
  - multiSelect 或 allowFreeText 时显示 "提交回答" 按钮
  - 提交按钮在 loading 时显示 "提交中..."
```

#### A.3.4 TodoPanel 显示
```
测试用例: Todo 面板正确渲染
前置条件: Agent 触发了 todo 工具调用
步骤:
  1. 等待 todo_updated SSE 事件
  2. 检查 Context Panel 中的 TodoPanel
验证点:
  - 标题显示 "Agent Todo" + 完成进度 (done/total)
  - 进度条百分比正确
  - 每个 todo item:
    - pending: Circle 灰色 + "待处理"
    - in_progress: Clock brand-500 + "进行中"
    - completed: CheckCircle 绿色 + "已完成"
    - blocked: Circle 红色 + "阻塞"
  - 空 todo 显示 "Agent 尚未创建任务。"
```

#### A.3.5 Context Panel 内容
```
测试用例: Context Panel 各区块显示
前置条件: 已有活跃会话
验证点:
  - "当前智能体" 区块: 显示头像、名称、状态
  - "会话信息" 区块: 显示消息数、会话ID (前8位)
  - TodoPanel (如有 todo)
  - "等待用户选择" (如有 pending question)
  - "工具执行" 列表 (如有 tool calls): 每项显示状态图标 + 工具名
  - "引用来源" 列表 (如有 citations): 每项显示文件图标 + 标题
  - "查看智能体详情" 按钮可点击
  - 无活跃会话时显示相应空状态
```

### A.4 元素重叠检测

#### A.4.1 通用重叠检测脚本
```javascript
// 在浏览器 console 或 Playwright evaluate 中运行
() => {
  const elements = document.querySelectorAll('button, a, input, textarea, [role="button"]');
  const results = [];
  for (let i = 0; i < elements.length; i++) {
    for (let j = i + 1; j < elements.length; j++) {
      const a = elements[i].getBoundingClientRect();
      const b = elements[j].getBoundingClientRect();
      if (a.width === 0 || a.height === 0 || b.width === 0 || b.height === 0) continue;
      const overlap = !(a.right <= b.left || a.left >= b.right || a.bottom <= b.top || a.top >= b.bottom);
      if (overlap) {
        results.push({
          el1: elements[i].tagName + '.' + elements[i].className?.split(' ').slice(0,3).join('.'),
          el2: elements[j].tagName + '.' + elements[j].className?.split(' ').slice(0,3).join('.'),
          overlapArea: Math.max(0, Math.min(a.right, b.right) - Math.max(a.left, b.left)) *
                       Math.max(0, Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top)),
        });
      }
    }
  }
  return results.filter(r => r.overlapArea > 100); // 过滤微小重叠
}
```

#### A.4.2 特定区域重叠检查
```
测试用例: Agent 选择器下拉与内容区重叠
步骤:
  1. 点击 Agent 选择器打开下拉
  2. 检查下拉是否被 Top Bar 下方内容遮挡
验证点:
  - z-10 确保下拉在最前
  - 下拉不被其他元素裁切 (overflow-y-auto 在 max-h-64 内)
  - 下拉选项全部可点击

测试用例: Context Panel 与 Message Area 不重叠
步骤:
  1. 确保视口 >= 1280px
  2. 发送长消息使 Message Area 有滚动
验证点:
  - Context Panel 有独立滚动 (overflow-y-auto)
  - 两者的滚动条不互相影响
```

---

## 4. 类别 B：功能完整性

### B.1 会话管理

#### B.1.1 创建新会话
```
测试用例: 通过 "新建对话" 按钮创建会话
步骤:
  1. 点击 "新建对话" 按钮
  2. 等待创建完成
验证点:
  - 按钮在创建中显示 Loader2 动画，文字不变
  - 创建成功后:
    - Session List 顶部新增会话项
    - 自动切换为该会话 (高亮)
    - 消息区域清空 (旧消息不残留)
    - Todo 和 PendingQuestion 状态重置
  - 如无可用 Agent，弹出 alert "请先创建一个智能体"
  - 创建失败时弹出 alert "创建会话失败"
  - creatingSession 期间按钮 disabled，防止重复点击
```

#### B.1.2 通过 URL 参数创建会话
```
测试用例: 访问 /chat?agentId=xxx 自动创建会话
步骤:
  1. 导航到 /chat?agentId=<已知 agent UUID>
  2. 等待自动创建
验证点:
  - 自动创建以该 agent 为 currentAgentId 的会话
  - 会话出现在列表中
  - 自动选中该会话
  - processedAgentIdRef 防止重复创建 (useRef 机制)
  - 如果 URL 同时有 sessionId，不执行自动创建
```

#### B.1.3 通过 URL 参数加载会话
```
测试用例: 访问 /chat?sessionId=xxx 自动加载会话
步骤:
  1. 导航到 /chat?sessionId=<已知 session UUID>
  2. 等待加载
验证点:
  - 自动加载该会话的 messages
  - 自动加载 agent 信息
  - processedSessionIdRef 防止重复加载
```

#### B.1.4 删除会话
```
测试用例: 删除活跃会话 → 自动切换到相邻会话
步骤:
  1. 在 Session List 中 hover 某个会话，点击 ✕
  2. 在 confirm 对话框中确认
  3. 观察会话列表和消息区域变化
验证点:
  - 删除前弹出 window.confirm
  - 删除中：✕ 变为 Loader2 动画
  - 如果删除的是当前活跃会话:
    - 自动切换到下一个会话 (优先) 或上一个会话
    - 如果无相邻会话，清空消息区域
  - 删除成功后从列表移除
  - 404 响应也视为删除成功 (幂等处理)
  - 失败时弹出 alert "删除对话失败，请稍后重试"
```

### B.2 消息发送与 SSE 流

#### B.2.1 基本消息发送
```
测试用例: 输入文本并发送
步骤:
  1. 在输入框输入 "你好，请介绍一下你自己"
  2. 按 Enter 发送
验证点:
  - 用户消息立即出现在消息列表 (右对齐)
  - 输入框清空
  - 发送按钮变为 Loader2 动画
  - 创建一个 STREAMING 状态的 assistant 占位消息
  - 发送过程中 Enter 键不生效 (sending=true 时 handleSendMessage 直接 return)
  - 新消息自动滚动到底部 (smooth behavior)
```

#### B.2.2 SSE Token 流式渲染
```
测试用例: LLM 逐 token 流式输出
步骤:
  1. 发送消息后观察 assistant 消息
验证点:
  - 文本逐 token 追加显示
  - 流式输出期间末尾有闪烁光标 (animate-pulse)
  - content 在 React state 中实时更新 (不卡顿)
  - 没有重复 token (seq 递增但没有重复内容)
  - 长文本正确换行
```

#### B.2.3 SSE Tool Call 流程
```
测试用例: Agent 调用工具的完整流程
前置条件: Agent 配置了工具 (如 web_search)
步骤:
  1. 发送需要工具调用的消息
  2. 观察 tool_call_start → tool_call_end 流程
验证点:
  - tool_call_start: ToolCallCard 以 running 状态出现
  - tool_call_end (success): ToolCallCard 状态变为 success，显示 result_summary
  - tool_call_end (failed): ToolCallCard 状态变为 failed
  - 工具结果正确关联到对应的 tool_call_id
  - 多步工具调用按顺序显示
```

#### B.2.4 Question 中断与继续
```
测试用例: Agent 提问 → 用户回答 → Agent 继续
步骤:
  1. 发送触发 question 工具的消息 (如 "帮我搜索并对比几个选项")
  2. 等待 question SSE 事件 → QuestionCard 出现
  3. 选择一个选项 (单选模式自动提交)
  4. 观察 Agent 继续执行
验证点:
  - question 事件后 assistant 状态变为 WAITING_USER_INPUT
  - QuestionCard 仅在未回答时显示
  - 回答后 QuestionCard 状态变为 answered (灰色禁用)
  - 新 assistant 消息出现在 QuestionCard 下方
  - Agent 继续流式输出
  - message_end 后状态变为 COMPLETED
```

#### B.2.5 Step Limit 处理
```
测试用例: 达到 maxSteps 限制后的行为
前置条件: Agent maxSteps 设置较小 (如 3)
步骤:
  1. 发送需要多步执行的复杂任务
  2. 等待 step_limit 事件
验证点:
  - 显示步数限制提示 "(已达最大步数限制 X/Y，可继续执行)"
  - 创建 ChatSessionState (保存上下文)
  - 可通过 continue API 继续执行
  - Context Panel 无异常
```

#### B.2.6 重新生成
```
测试用例: 对 assistant 消息进行 "重新生成"
步骤:
  1. 找到一条 COMPLETED 状态的 assistant 消息
  2. hover 该消息，点击 "重新生成"
验证点:
  - 原消息被新的 STREAMING 消息替换 (splice)
  - 新消息正常流式输出
  - 失败时显示错误信息并状态为 FAILED
  - 原来的 regenerate 操作不影响其他消息
```

#### B.2.7 复制消息
```
测试用例: 复制 assistant 消息内容
步骤:
  1. 找到一条有内容的 COMPLETED/FAILED assistant 消息
  2. 点击 "复制"
验证点:
  - 剪贴板中写入正确内容
  - 按钮文字变为 "已复制" (1.5s)
  - 1.5s 后恢复为 "复制"
```

### B.3 Agent 切换

#### B.3.1 切换当前会话的 Agent
```
测试用例: 在对话中切换 Agent
步骤:
  1. 点击 Top Bar 的 Agent 选择器
  2. 在下拉中选择不同的 Agent
  3. 发送新消息
验证点:
  - 切换后 Session 的 currentAgentId 更新
  - 会话中插入分隔消息 "— 已切换到新 Agent —"
  - 后续 LLM 上下文仅包含分隔符之后的消息 (loadRelevantHistory)
  - 切换前发送中时不应允许切换 (或正确处理)
  - 没有活跃会话时切换按钮不可操作 (handleSwitchAgent 直接 return)
```

#### B.3.2 Agent 下拉菜单
```
测试用例: Agent 下拉菜单的交互
步骤:
  1. 点击 Agent 选择器打开下拉
  2. 点击页面空白区域
  3. 选择一个 Agent
验证点:
  - 下拉显示所有可用 Agent
  - 当前 Agent 高亮 (bg-brand-50 text-brand-500)
  - 无 Agent 时显示 "暂无可用智能体"
  - 选择后下拉关闭
  - 下拉有 max-h-64 overflow-y-auto (长列表可滚动)
  - 下拉 z-10 确保覆盖其他元素
```

### B.4 输入框行为

#### B.4.1 键盘快捷键
```
测试用例: Enter 和 Shift+Enter
步骤:
  1. 输入文本，按 Enter → 发送消息
  2. 输入文本，按 Shift+Enter → 换行不发送
  3. 空输入，按 Enter → 不发送 (!content 判断)
  4. 发送中 (sending=true)，按 Enter → 不发送
验证点:
  - Enter 触发 handleSendMessage
  - Shift+Enter 不触发，浏览器默认换行
  - 空内容不触发发送
  - 发送按钮 disabled 状态与按键逻辑一致
```

#### B.4.2 清空按钮
```
测试用例: 清空输入框
步骤:
  1. 输入文本
  2. 点击 "清空" 按钮
验证点:
  - 按钮仅在 messageInput 非空时显示
  - 点击后输入框清空
  - 清空后按钮隐藏
```

#### B.4.3 无会话时发送
```
测试用例: 在没有活跃会话时发送消息
步骤:
  1. 确保 activeSessionId 为空
  2. 输入文本并发送
验证点:
  - 自动创建新会话 (使用第一个可用 Agent)
  - 创建成功后继续发送消息
  - 如无 Agent，弹出 alert "请先创建一个智能体"
  - 创建失败弹出 alert "创建会话失败"
```

### B.5 SSE 连接异常

#### B.5.1 网络断开
```
测试用例: SSE 连接中断的处理
步骤:
  1. 发送消息，正在流式输出
  2. 模拟网络断开 (或关闭后端)
验证点:
  - 前端不崩溃 (不白屏)
  - assistant 消息停留在最后收到的内容
  - 状态可能为 FAILED 或保持 STREAMING
  - 发送按钮恢复可用状态 (finally 块)
```

#### B.5.2 错误事件处理
```
测试用例: 后端返回 error SSE 事件
前置条件: 触发某些会导致 recoverable error 的场景
验证点:
  - recoverable=false: 消息状态 → FAILED，显示错误信息
  - recoverable=true: 消息可能保持流式状态或重试
  - 发送按钮恢复可用 (sending → false)
```

#### B.5.3 Reconnect (Last-Event-ID)
```
测试用例: 断线重连
步骤:
  1. 发送消息
  2. 记录最后一个收到的 event id
  3. 模拟重连 (带 Last-Event-ID header)
验证点:
  - 重连后回放断线期间的事件 (从 SseEventCacheService)
  - 不重复发送已收到的事件 (getEventsAfter 从 lastEventId 之后开始)
  - 缓存过期 (5分钟) 后重连返回空流并立即 complete
```

---

## 5. 类别 C：刷新与数据持久化

### C.1 基本刷新行为

#### C.1.1 刷新后会话列表不变
```
测试用例: F5 刷新后会话列表完整
前置条件: 有 3+ 个会话，部分有消息
步骤:
  1. 在 /chat 页面确认会话列表显示正常
  2. 按 F5 刷新页面
  3. 等待加载完成
验证点:
  - 所有会话仍在列表中 (GET /chat/sessions 返回一致)
  - 会话顺序按 updatedAt 降序
  - 分组 (今天/昨天/更早) 正确
  - lastMessageMap 正确显示最后一条消息预览 (从 messages.content 重建)
```

#### C.1.2 刷新后消息历史完整
```
测试用例: F5 刷新后消息不丢失
前置条件: 活跃会话有 5+ 条历史消息
步骤:
  1. 在 /chat 页面查看消息列表
  2. 按 F5 刷新
  3. 等待 GET /chat/sessions/{id} 完成
验证点:
  - 所有消息重新加载 (GET 返回完整的 ChatSessionDetailVO.messages)
  - 用户消息、助手消息、工具调用全部显示
  - 消息顺序与刷新前一致 (按 createdAt 升序)
  - 工具调用详情 (ToolCallCard) 正确展开/折叠
  - QuestionCard 如未过期仍可交互
```

#### C.1.3 刷新后流式状态重置
```
测试用例: 刷新页面不保留流式输出状态
前置条件: 正在进行 SSE 流式输出
步骤:
  1. 发送消息，确认正在流式输出
  2. 在输出过程中按 F5 刷新
验证点:
  - SSE 连接随页面刷新自动断开
  - 刷新后 assistant 消息状态为 COMPLETED 或 INCOMPLETE (来自后端持久化)
  - 不会出现 STREAMING 状态的旧消息
  - 后端已持久化的部分内容会显示
```

### C.2 不应持久化的数据

#### C.2.1 重复会话检查
```
测试用例: 刷新不应产生重复会话
步骤:
  1. 记录当前会话列表数量和 ID
  2. 多次刷新页面 (10次)
  3. 再次检查会话列表
验证点:
  - 会话数量不变
  - 无重复 ID 的会话
  - 不会因 URL 参数 (agentId) 在每次刷新时创建新会话 (processedAgentIdRef 防护)
```

#### C.2.2 Todo 状态恢复
```
测试用例: 刷新后 Todo 状态从消息历史恢复
前置条件: Agent 在对话中创建了 todo
步骤:
  1. 确认 Context Panel 显示 TodoPanel
  2. F5 刷新
  3. 等待消息加载
验证点:
  - Todo 从最新消息的 toolResults 中恢复 (latestTodoFromMessages)
  - 如果没有 todo 相关的消息，activeTodo 为 null
  - 刷新后不会凭空生成 todo
```

#### C.2.3 Pending Question 恢复
```
测试用例: 刷新后 Pending Question 状态
前置条件: Agent 在对话中提出了 question (未回答)
步骤:
  1. 确认 QuestionCard 显示 (pending 状态)
  2. F5 刷新
  3. 等待消息加载
验证点:
  - QuestionCard 重新显示
  - 如 sessionStateId 仍有效 (创建后 1 小时内)，可以继续回答
  - 如 sessionStateId 已过期，回答会失败并提示 "该问题缺少恢复状态，请刷新会话后重试"
  - 已回答的问题 (status: answered) 不会以 pending 状态重新出现
```

#### C.2.4 输入框内容
```
测试用例: 刷新后输入框应清空
步骤:
  1. 在输入框输入 "test message"
  2. F5 刷新
验证点:
  - 输入框为空 (messageInput 是 React state，刷新后重置)
```

#### C.2.5 Context Panel 展开状态
```
测试用例: 刷新后 Context Panel 默认状态
步骤:
  1. 手动关闭 Context Panel
  2. F5 刷新
验证点:
  - 视口 >= 1280px 时 Context Panel 重新显示 (matchMedia 逻辑)
  - 视口 < 1280px 时 Context Panel 隐藏
  - 状态由视口决定，而非记住用户上一次选择 (这是当前实现，如产品需求变更需更新)
```

### C.3 URL 参数持久化

#### C.3.1 带 sessionId 的 URL 刷新
```
测试用例: /chat?sessionId=xxx 刷新后正确加载
步骤:
  1. 导航到 /chat?sessionId=<已知 session UUID>
  2. F5 刷新
验证点:
  - 刷新后自动重新加载该会话
  - 不会创建新会话 (processedSessionIdRef)
  - 不因 agentId 参数同时存在而产生冲突
```

#### C.3.2 带 agentId 的 URL 刷新
```
测试用例: /chat?agentId=xxx 刷新后行为
步骤:
  1. 导航到 /chat?agentId=<已知 agent UUID>
  2. 确认自动创建了会话
  3. F5 刷新
验证点:
  - 刷新后不创建第二个会话 (processedAgentIdRef)
  - 新创建的会话仍在列表中
  - 如果手动删除该会话后刷新，会创建新的 (ref 已重置)
```

### C.4 数据一致性

#### C.4.1 消息与后端一致
```
测试用例: 刷新后消息内容与后端数据库一致
步骤:
  1. 发送消息 "Hello World"
  2. 等待 COMPLETED
  3. 通过 API 直接查询消息:
     curl http://localhost:3000/api/v1/chat/sessions/{sessionId}
  4. F5 刷新
  5. 对比刷新后的消息列表
验证点:
  - 刷新后的消息内容与 API 直接查询结果完全一致
  - toolCalls JSON 解析正确
  - toolResults JSON 解析正确
  - 消息顺序不改变
```

#### C.4.2 Agent 切换后刷新
```
测试用例: 切换 Agent 后刷新
步骤:
  1. 在会话中切换 Agent
  2. 发送一条消息
  3. F5 刷新
验证点:
  - currentAgentId 保持为切换后的 Agent
  - 分隔符消息 "— 已切换到新 Agent —" 在消息列表中显示
  - 刷新后分隔符仍然存在
```

#### C.4.3 消息清空后刷新
```
测试用例: 清空消息后刷新
步骤:
  1. 对某会话调用 DELETE /chat/sessions/{id}/messages
  2. F5 刷新
验证点:
  - 消息列表为空
  - 会话仍在 Session List 中
  - 会话标题不变
```

---

## 6. Bug 报告模板

### 6.1 模板格式

```markdown
## Bug: [简短描述]

**严重级别**: [Critical / High / Medium / Low]
**发现时间**: YYYY-MM-DD HH:mm
**测试用例**: [对应的测试用例编号]

### 复现步骤
1. 导航到 /chat
2. ...
3. ...

### 预期行为
[应该发生什么]

### 实际行为
[实际发生了什么，附截图]

### 技术分析 (可选)
- 涉及文件: ChatPage.tsx:XX-YY
- 可能原因: [初步分析]

### 环境信息
- 浏览器: Chromium XX
- 视口: 1280×720
- 后端状态: [Redis 可用/不可用, DB 正常/异常]
```

### 6.2 Playwright 测试代码模板

```typescript
import { test, expect } from '@playwright/test';

test.describe('Chat Page - [Category]', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/chat');
    await page.waitForLoadState('networkidle');
  });

  test('[Test Case ID]: [Description]', async ({ page }) => {
    // 1. Arrange: 准备前置条件
    // ...

    // 2. Act: 模拟用户操作
    // ...

    // 3. Assert: 验证结果
    // ...

    // 4. 截图用于 Bug 报告
    await page.screenshot({ path: `test-results/${test.info().title}.png` });
  });
});
```

### 6.3 常用 Playwright 选择器参考

| 元素 | 推荐选择器 |
|------|-----------|
| 新建对话按钮 | `button:has-text("新建对话")` |
| 发送按钮 | `button:has-text("发送")` |
| 消息输入框 | `textarea[placeholder*="输入消息"]` |
| 会话列表项 | `[role="button"]` within session list |
| Agent 选择器 | `button:has(.lucide-bot)` 或 `button:has-text("选择智能体")` |
| ToolCallCard | `.border-border-subtle.rounded-lg.bg-white` |
| QuestionCard | `.border-amber-200` |
| Context Panel 切换 | `button:has(.lucide-panel-right)` |
| 复制按钮 | `button:has-text("复制")` |
| 重新生成按钮 | `button:has-text("重新生成")` |
| 删除按钮 (会话) | `button[aria-label="删除对话"]` |
| 清空输入按钮 | `button:has-text("清空")` |
| 消息气泡 (用户) | `.bg-brand-50` |
| 消息气泡 (助手) | `.bg-white.border` |
| TodoPanel | 包含 `ListTodo` 图标的区域 |

---

## 附录：关键后端 API 速查

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/chat/sessions` | 创建会话 (body: `{agentId}`) |
| GET | `/api/v1/chat/sessions` | 会话列表 (`?page=1&page_size=50`) |
| GET | `/api/v1/chat/sessions/{id}` | 会话详情 (含 messages) |
| DELETE | `/api/v1/chat/sessions/{id}` | 删除会话 |
| DELETE | `/api/v1/chat/sessions/{id}/messages` | 清空消息 |
| POST | `/api/v1/chat/sessions/{id}/messages` | 发送消息 (SSE, body: `{content}`) |
| POST | `/api/v1/chat/sessions/{id}/messages/{msgId}/regenerate` | 重新生成 (SSE) |
| POST | `/api/v1/chat/sessions/{id}/continue` | 继续执行 (SSE, body: `{sessionStateId}`) |
| POST | `/api/v1/chat/sessions/{id}/questions/{stateId}/answer` | 回答问题 (SSE) |
| POST/PUT | `/api/v1/chat/sessions/{id}/switch-agent` | 切换 Agent |

---

> **文档版本**: v1.0 | **最后更新**: 2026-05-08 | **维护者**: Sisyphus (OhMyOpenCode)
