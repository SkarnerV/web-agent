# Chat Page Agent 工具测试用例

> 测试范围：Chat 页面用户与配置了 todo/question 工具的 Agent 对话流程
> 测试环境：前端 localhost:3000，后端 localhost:8080

---

## TC-001 用户在 Chat 页面可以正常和已配置的 Agent 对话

- **测试步骤：**
  1. 导航到 `/chat` 页面
  2. 在左侧会话列表中点击一个已有会话（或点击"新建对话"创建新会话）
  3. 在输入框中输入消息内容（例如："你好，请简单介绍一下你自己"）
  4. 按 Enter 或点击"发送"按钮
  5. 等待 Agent 回复完成（观察 SS E 流式输出过程）

- **预期结果：**
  1. 用户消息以右对齐气泡显示，背景色为 brand-50
  2. Agent 回复以左对齐气泡显示，左侧有 Agent 头像，上方显示 Agent 名称
  3. Agent 回复过程中显示流式光标动画（`animate-pulse`），表示正在输出
  4. 回复完成后光标消失，气泡下方显示"复制"和"重新生成"按钮
  5. 消息状态从 STREAMING 变为 COMPLETED
  6. 右侧 Context Panel 显示当前 Agent 信息和会话信息（消息数更新）
  7. 会话列表中的最后一条消息预览更新

- **实际结果：**

- **执行结果：**

---

## TC-002 Agent 调用 todo 工具后必须完成全部任务才能结束

- **测试步骤：**
  1. 在 Chat 页面（或 Debug 页面）向 Agent 发送消息，要求其使用 todo 工具创建任务清单
  2. 示例输入："请使用 todo 工具列出以下3个任务：1.分析需求 2.编写方案 3.输出总结。然后逐步完成它们。"
  3. 观察 SSE 事件流和 UI 变化：
     - `tool_call_start` → 显示 ToolCallCard（状态：running）
     - `todo_updated` → 右侧 Context Panel 出现 TodoPanel
     - `tool_call_end` (todo, success) → ToolCallCard 状态变为 success
     - Agent 继续输出文字或调用其他工具
  4. 观察 Agent 是否在 todo 任务未全部完成时就给出最终总结
  5. 观察 TodoPanel 中各任务的状态变化（pending → in_progress → completed）

- **预期结果：**
  1. TodoPanel 在右侧 Context Panel 中正确显示，包含：
     - 标题（Agent Todo）
     - 进度条（如 1/3, 33%）
     - 每个任务的状态图标（pending=空心圆, in_progress=时钟, completed=绿色勾, blocked=红色）
  2. Agent 在所有 todo 任务完成前**不会**发出 `message_end` 并给出最终总结
  3. 当 LLM 输出文字但仍有未完成 todo 时，后端 Orchestrator 会自动注入 continu ation prompt，让 Agent 继续执行
  4. 每个任务的状态变化会通过新的 `todo_updated` SSE 事件实时推送到前端
  5. 只有当所有 todo 项目状态都变为 `completed` 后，Agent 才会发出 `messag e_end` 结束本次对话
  6. 如果达到 maxSteps 限制但 todo 仍未完成，会发送 `step_limit` 事件，状态 变为 WAITING_CONTINUE，用户可手动继续

- **实际结果：**

- **执行结果：**

---

## TC-003 Agent 调用 question 工具后，用户点击答案时 Agent 继续对话不中断

- **测试步骤：**
  1. 向 Agent 发送消息，触发其调用 question 工具
  2. 示例输入："请使用 question 工具询问我一个选择题"
  3. 等待 Agent 发出 question SSE 事件，UI 显示 QuestionCard
  4. 观察 QuestionCard 的样式和状态：
     - 黄色边框（border-amber-200）和黄色背景（bg-amber-50/70）
     - 问题文本 + 帮助图标
     - 选项按钮列表（3-6 个选项）
     - "Agent 需要你选择后继续执行。" 提示文本
  5. 在 QuestionCard 中点击一个选项
  6. 观察 Agent 是否立即创建新的 continuation 消息并继续流式输出
  7. 观察原问题所在的消息和 QuestionCard 状态变化

- **预期结果：**
  1. QuestionCard 正确渲染，选项可点击：
     - 单选模式：点击选项后立即触发 onAnswer（QuestionCard 内部 toggleOption 逻辑）
     - 多选/自由文本模式：显示"提交回答"按钮，需手动点击提交
  2. 用户点击选项后：
     a. QuestionCard 立即变为 disabled 状态，选项不可再点击
     b. QuestionCard 状态变为 "answering"（"提交中..."）
     c. 原包含 question 的 assistant 消息状态从 WAITING_USER_INPUT 变为 COMPLETED
     d. 在对话区中**紧接着**出现一条新的 assistant continuation 消息（STREAMING 状态）
     e. 新的 continuation 消息开始接收 SSE 流式文本输出
  3. Agent 继续执行后续任务，对话流程不中断：
     a. 后端 Orchestrator 从保存的 session state 恢复上下文
     b. 用户的回答作为 tool_result 注入到 LLM 上下文
     c. Agent 基于用户回答继续推理和工具调用
  4. QuestionCard 最终状态变为 "answered"，提示文本变为"已回答，Agen t 正在继续执行。"
  5. 右侧 Context Panel 中的"等待用户选择"区域消失
  6. 控制台无 error 级别日志

- **实际结果：**

- **执行结果：**

---

## TC-004 Agent 同时使用 todo 和 question 工具时的协作流程

- **测试步骤：**
  1. 向 Agent 发送消息，要求其先创建 todo 任务，然后使用 question 向用户提问
  2. 示例输入："请创建3个开发任务，然后问我一个问题确认优先级"
  3. 观察 todo 创建 → question 提问的完整流程
  4. 观察 TodoPanel 和 QuestionCard 同时在页面上的表现
  5. 点击 question 答案后，观察 Agent 是否继续执行并完成 todo

- **预期结果：**
  1. TodoPanel 和 QuestionCard 同时出现在页面上：
     - TodoPanel 在右侧 Context Panel
     - QuestionCard 在对话区的 assistant 消息气泡下方
  2. 当 question 触发时：
     a. Session state 被保存（包含当前的 todo 状态和完整 LLM 上下文）
     b. SSE 流正常结束（emitter.complete），不保持挂起
     c. 原 assistant 消息状态变为 WAITING_USER_INPUT
  3. 用户回答 question 后：
     a. Agent 从保存的 session state 恢复，**保留**之前的 todo 进度
     b. 新的 continuation 消息创建并开始新的 SSE 流
     c. Agent 继续执行未完成的 todo 任务
  4. 整个流程中对话不会中断，用户体验连续
  5. todo 任务全部完成后 Agent 才发出 message_end

- **实际结果：**

- **执行结果：**

---

## 附录：关键代码路径

| 功能 | 前端 | 后端 |
|------|------|------|
| 消息发送与流式接收 | `ChatPage.tsx:1011-1089` `handleSendMessage` | `ChatOrchestrator.java:106-138` `handleSendMessage` |
| SSE 事件消费 | `ChatPage.tsx:875-1007` `consumeAssistantStream` | `ChatOrchestrator.java:287-538` `streamReply` |
| Todo 创建与更新 | `ChatPage.tsx:191-247` `TodoPanel` | `BuiltinToolExecutor.java:35-75` `applyTodo` |
| Todo 循环（必须全部完成） | — | `ChatOrchestrator.java:362-381` `hasOpenTodoItems` + `todoContinuat ionPrompt` |
| Question 解析与验证 | `ChatPage.tsx:249-327` `QuestionCard` | `BuiltinToolExecutor.java:77-156` `parseQuestion` |
| Question 回答处理 | `ChatPage.tsx:1129-1231` `handleAnswerQuestion` | `ChatOrchestrator.java:214-258` `handleQuestionAnswer` |
| Session State 保存/恢复 | — | `ChatOrchestrator.java:850-868` `saveSessionState` |
| 消息持久化（标记已回答） | `ChatPage.tsx:1187-1204` | `ChatOrchestrator.java:917-946` `markQuestionAnswered` |
