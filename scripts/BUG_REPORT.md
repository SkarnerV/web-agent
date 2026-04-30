# 后端 Bug 修改列表（任务 1–4）

> 生成时间：2026-04-30  
> 验证范围：任务 1（公共组件）、任务 3（Agent 模块）、任务 4（对话引擎）

---

## 严重程度说明

| 级别 | 含义 |
|------|------|
| 🔴 Critical | 功能完全无法工作，阻塞前端对接 |
| 🟠 High | 主要功能行为错误，不符合需求规范 |
| 🟡 Medium | 契约违反或边界情况错误 |
| 🟢 Low | 代码质量/类型描述问题，不影响主流程 |

---

## Bug #1 — 🔴 幂等性服务未集成（Task 4.9 完全失效）

**文件**：`ap-module-chat/…/orchestrator/ChatOrchestrator.java`  
**问题**：`IdempotencyService` 已实现，但从未在 `handleSendMessage()` 中调用。`SendMessageRequest.idempotencyKey` 字段被接收但被完全忽略。

**当前行为**：
- 同一 `idempotency_key` + 同一 body → 每次都创建新消息（未检测重复）
- 同一 `idempotency_key` + 不同 body → 不返回 409，照常执行

**期望行为**（§3.8.3）：
- 相同 key + 相同 body → 返回已存在的 `message_id`，不创建新消息
- 相同 key + 不同 body → 返回 409 `CHAT_IDEMPOTENCY_CONFLICT`

**修改方案**：
```java
// ChatOrchestrator.handleSendMessage() 开头添加：
if (request.getIdempotencyKey() != null) {
    String existingMsgId = idempotencyService.checkAndRegister(
        sessionId, request.getIdempotencyKey(), serializeRequest(request));
    if (existingMsgId != null) {
        // 重复请求：重放缓存的 SSE 事件
        return replayCachedEvents(existingMsgId);
    }
}
```

还需在构造函数注入 `IdempotencyService`，并在消息完成后调用 `idempotencyService.markComplete()`。

---

## Bug #2 — 🔴 Regenerate 插入新消息而非更新原消息（Task 4.7 行为错误）

**文件**：`ap-module-chat/…/orchestrator/ChatOrchestrator.java:95–107` 和 `persistMessage():321–348`  
**问题**：`handleRegenerate()` 先将原消息设为 `status="incomplete"`，然后调用 `persistMessage()`，而 `persistMessage()` 总是执行 `messageMapper.insert(msg)`。这会在会话历史中插入一条**新的**助手消息，而原来的 `incomplete` 消息依然残留。

**当前行为**：重新生成后，同一轮对话有 2 条助手消息（一条 incomplete + 一条新的）。

**期望行为**（任务 4.7）："原消息内容被新生成内容替换"，消息数量不变。

**修改方案**：
1. `handleRegenerate()` 中将 `messageId` 设为原消息的实际 UUID：
   ```java
   // 已正确：String messageId = original.getId().toString();
   ```
2. `persistMessage()` 需要区分"新建"和"更新"两种场景。建议增加一个 `targetMessageId` 参数：
   ```java
   private void persistMessage(UUID sessionId, String targetMessageId, ...) {
       if (targetMessageId != null) {
           // UPDATE 模式：更新已有消息
           ChatMessageEntity msg = messageMapper.selectById(UUID.fromString(targetMessageId));
           if (msg != null) {
               msg.setContent(content);
               msg.setStatus(status);
               // ... 更新其他字段
               messageMapper.updateById(msg);
               return;
           }
       }
       // INSERT 模式：新建消息
       messageMapper.insert(newMsg);
   }
   ```
3. `handleSendMessage()` 传入 `null` 作为 targetMessageId（新建），`handleRegenerate()` 传入 `original.getId().toString()`（更新）。

---

## Bug #3 — 🔴 ToolDispatcher 始终路由为内置工具（Task 4.5 MCP/知识库工具无法调用）

**文件**：`ap-module-chat/…/orchestrator/ChatOrchestrator.java:187–188`  
**问题**：调用 `toolDispatcher.dispatch()` 时第四个参数传入空 Map `Map.of()`，导致 `ToolDispatcher.resolveSourceType()` 始终返回 `"builtin"`。

```java
// 当前（错误）
ToolResult result = toolDispatcher.dispatch(
    tcc.toolCallId(), tcc.toolName(), tcc.arguments(), Map.of());  // ← 空 Map
```

**期望行为**：应传入当前 Agent 的工具绑定配置，让 Dispatcher 能正确区分 builtin / mcp / knowledge 来源。

**修改方案**：
```java
// 在 streamReply() 中加载 Agent 的工具绑定
Map<String, Map<String, Object>> toolBindingMap = loadAgentToolBindings(agentId);

// 调用时传入对应工具的绑定配置
Map<String, Object> bindingConfig = toolBindingMap.getOrDefault(tcc.toolName(), Map.of());
ToolResult result = toolDispatcher.dispatch(
    tcc.toolCallId(), tcc.toolName(), tcc.arguments(), bindingConfig);
```

其中 `loadAgentToolBindings()` 需从 `AgentToolBindingMapper` 查询并构建 `toolName → bindingConfig` 映射。

---

## Bug #4 — 🔴 Agent 的 model_id 和 max_steps 被硬编码，Agent 配置被完全忽略（Tasks 4.2, 4.4）

**文件**：`ap-module-chat/…/orchestrator/ChatOrchestrator.java:85, 107, 305–308`  

```java
// Bug #4a：max_steps 硬编码为 10
return startStreamReply(session, requestId, messageId, DEFAULT_MAX_STEPS, 0);  // DEFAULT_MAX_STEPS=10

// Bug #4b：model_id 硬编码
private String resolveModelId(UUID agentId) {
    // TODO: Load from AgentService
    return "gpt-4o";  // ← 固定返回
}
```

**当前行为**：无论 Agent 配置了什么模型和步骤上限，所有对话都用 `gpt-4o` 和 10 步。

**期望行为**：从数据库加载 Agent 的 `model_id` 和 `max_steps` 字段。

**修改方案**：
```java
// 注入 AgentMapper（或通过 ap-module-agent 接口）
private String resolveModelId(UUID agentId) {
    AgentEntity agent = agentMapper.selectById(agentId);
    if (agent == null || agent.getModelId() == null) {
        return defaultModelId;  // 从配置读取默认值
    }
    return agent.getModelId();
}

private int resolveMaxSteps(UUID agentId) {
    AgentEntity agent = agentMapper.selectById(agentId);
    return (agent != null && agent.getMaxSteps() != null) ? agent.getMaxSteps() : DEFAULT_MAX_STEPS;
}
```

注意：ap-module-chat 依赖 ap-module-agent 违反了"ap-module-* 仅依赖 ap-common"原则。建议通过 `ap-common` 中的接口 `AgentConfigProvider` 解耦，或在 `chat_sessions` 表中冗余存储 agent 配置快照。

---

## Bug #5 — 🟠 SSE 断线重连未实现（Task 4.10 完全失效）

**文件**：`ap-module-chat/…/orchestrator/ChatOrchestrator.java`  
**问题**：`SseEventCacheService` 已实现，但未在 `ChatOrchestrator` 中集成。所有 SSE 事件都未缓存到 Redis，客户端携带 `Last-Event-ID` 重连时无法恢复。

**影响**：任务 4.10 声明的"断线重连后从正确位置续传"完全不可用。

**修改方案**：
1. 在 `buildEvent()` 发送前同时调用 `sseEventCacheService.appendEvent(messageId, eventId, eventType, json)`
2. `ChatController` 的 `sendMessage()` 接收 `@RequestHeader(value = "Last-Event-ID", required = false)` 参数
3. 如果 `lastEventId` 不为空，先从 Redis 取出缓存事件并重放，再继续新的流

---

## Bug #6 — 🟠 Export 端点未遵守 ApiResponse 包装契约

**文件**：`ap-module-agent/…/controller/AgentController.java:90–94`  

```java
// 当前（违反 AGENTS.md Rule #1）
@GetMapping(value = "/{id}/export", produces = MediaType.APPLICATION_JSON_VALUE)
public Map<String, Object> export(...)  // ← 裸 Map

// 期望
public ApiResponse<Map<String, Object>> export(...)
```

**影响**：前端无法用统一方式解析 export 响应（没有 `success/data/requestId` 外层）。

**修改方案**：
```java
@GetMapping(value = "/{id}/export", produces = MediaType.APPLICATION_JSON_VALUE)
public ApiResponse<Map<String, Object>> export(@PathVariable UUID id,
                                               @CurrentUser UserPrincipal user) {
    Map<String, Object> data = agentService.export(id, user.id());
    return ApiResponse.ok(data, RequestIdContext.current());
}
```

---

## Bug #7 — 🟠 Export JSON 缺少 source_name 和 source_url 字段（Task 3.3 导入解析失败）

**文件**：`ap-module-agent/…/service/AgentService.java:288–296`  
**问题**：导出的 `tool_bindings` 数组中每条记录仅有 `source_type/source_id/tool_name/tool_schema_snapshot/enabled`，缺少 `source_name` 和 `source_url` 字段。

**影响**（任务 3.3 验收标准）：导入时按优先级 `source_id → source_url+tool_name → source_name+tool_name` 的解析链路，第 2、3 优先级完全无法工作。

**修改方案**：
```java
List<Map<String, Object>> toolBindingsExport = bindings.stream().map(b -> {
    Map<String, Object> tb = new LinkedHashMap<>();
    tb.put("source_type", b.getSourceType());
    tb.put("source_id", b.getSourceId());
    tb.put("source_name", resolveSourceName(b));   // ← 新增
    tb.put("source_url",  resolveSourceUrl(b));    // ← 新增
    tb.put("tool_name", b.getToolName());
    tb.put("tool_schema_snapshot", parseJson(b.getToolSchemaSnapshot()));
    tb.put("enabled", b.getEnabled());
    return tb;
}).toList();
```

`resolveSourceName/Url` 对于 MCP 工具需查询 `mcps` 表获取名称和 URL；对于内置工具可留空。

---

## Bug #8 — 🟡 SSE message_id 格式与持久化消息 UUID 不一致

**文件**：`ap-module-chat/…/orchestrator/ChatOrchestrator.java:82–83`  

```java
// 发送 SSE 时 message_id 为 "msg_xxxxxx" (12位hex前缀字符串)
String messageId = "msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

// 但 persistMessage() 插入新行时 id 由 @TableId(ASSIGN_UUID) 生成真正的 UUID
messageMapper.insert(msg);  // msg.id = 新 UUID
```

**影响**：客户端通过 SSE 得到的 `message_id` 无法直接用于调用 `POST /regenerate/{msgId}`（该接口接收 `@PathVariable UUID msgId`）。客户端必须通过查询会话历史才能得到真实 UUID。

**修改方案**：在 `persistMessage()` 中复用传入的 `messageId` 参数（若是合法 UUID），或在发送 SSE 前预先生成 UUID 并赋值给 Entity：
```java
ChatMessageEntity msg = new ChatMessageEntity();
msg.setId(UUID.fromString(messageId));  // 复用 SSE 中的 messageId（前提：messageId 是合法 UUID）
```
同时将 `handleSendMessage()` 中的 messageId 生成改为标准 UUID：
```java
// 改为
UUID messageUUID = UUID.randomUUID();
String messageId = messageUUID.toString();
```

---

## Bug #9 — 🟡 验证失败错误码为 INVALID_REQUEST 而非 AGENT_VALIDATION_FAILED

**文件**：`ap-common/…/error/GlobalExceptionHandler.java:43–53`  
**问题**：当 `AgentCreateRequest`/`AgentUpdateRequest` 校验失败时（如 name 超 30 字符、maxSteps 超范围），`GlobalExceptionHandler` 统一返回 `INVALID_REQUEST`，但任务 3.1 验收标准要求返回 `AGENT_VALIDATION_FAILED`。

**修改方案**：在 `AgentService.create()` 和 `update()` 中手动做业务级验证，或定制 Agent Controller 的 `@ExceptionHandler`，将 Agent 相关的 `MethodArgumentNotValidException` 映射为 `AGENT_VALIDATION_FAILED`。

简单做法：在 Controller 层加局部处理：
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleAgentValidation(MethodArgumentNotValidException ex) {
    // build error with AGENT_VALIDATION_FAILED code
}
```

---

## Bug #10 — 🟡 handleContinue 对过期会话状态使用错误的错误码

**文件**：`ap-module-chat/…/orchestrator/ChatOrchestrator.java:117–119`  

```java
// 当前
throw new BizException(ErrorCode.ASSET_NOT_FOUND, Map.of("reason", "session_state expired"));

// 期望（任务 4.6："session_state 已过期，When POST /continue，Then 返回 404"）
// 使用更语义化的错误码，或新增 CHAT_SESSION_STATE_EXPIRED
throw new BizException(ErrorCode.CHAT_SESSION_NOT_FOUND, Map.of("reason", "session_state_expired"));
```

**建议**：在 `ErrorCode` 中新增 `CHAT_SESSION_STATE_NOT_FOUND(HttpStatus.NOT_FOUND, "会话状态不存在或已过期")`。

---

## Bug #11 — 🟢 switchAgent 返回类型误导（ApiResponse\<ChatMessageVO\> 返回 null data）

**文件**：`ap-module-chat/…/controller/ChatController.java:86–92`  

```java
// 当前（data 始终为 null，泛型类型误导前端）
public ApiResponse<ChatMessageVO> switchAgent(...) {
    sessionService.switchAgent(...);
    return ApiResponse.ok(null, RequestIdContext.current());
}

// 修改为
public ApiResponse<Void> switchAgent(...) {
    sessionService.switchAgent(...);
    return ApiResponse.ok(null, RequestIdContext.current());
}
```

---

## Bug #12 — 🟢 SwitchAgentRequest 缺少 @Valid 注解

**文件**：`ap-module-chat/…/controller/ChatController.java:89`  

```java
// 当前
public ApiResponse<ChatMessageVO> switchAgent(@PathVariable UUID id,
                                              @RequestBody SwitchAgentRequest request,  // ← 无 @Valid

// 修改为
                                              @Valid @RequestBody SwitchAgentRequest request,
```

---

## 修改优先级汇总

| 优先级 | Bug | 所属任务 | 预估工作量 |
|--------|-----|----------|----------|
| P0 | #2 Regenerate 插入新消息 | 4.7 | 1h |
| P0 | #3 ToolDispatcher 路由错误（空 bindings） | 4.5 | 2h |
| P0 | #4 model_id/max_steps 硬编码 | 4.2/4.4 | 3h |
| P1 | #1 幂等性服务未集成 | 4.9 | 2h |
| P1 | #5 SSE 断线重连未实现 | 4.10 | 4h |
| P1 | #6 Export 未包 ApiResponse | 3.3 | 0.5h |
| P1 | #7 Export 缺 source_name/url | 3.3 | 1h |
| P2 | #8 message_id 格式不一致 | 4.4/4.7 | 1h |
| P2 | #9 验证错误码不符合规范 | 3.1 | 1h |
| P3 | #10 错误码语义不准确 | 4.6 | 0.5h |
| P3 | #11 返回类型误导 | 4.8 | 0.5h |
| P3 | #12 缺少 @Valid | 4.8 | 0.25h |

---

## 已实现功能（正常）

以下功能经过代码审查，逻辑正确：

- ✅ Task 1.4 `ApiResponse` / `ErrorCode` / `GlobalExceptionHandler` / `RequestIdFilter` / `BizException`
- ✅ Task 1.5 `BaseEntity` 乐观锁（`@Version`）、逻辑删除（`@TableLogic`）、自动填充
- ✅ Task 1.6 `StubUserContext` + `@CurrentUser` ArgumentResolver
- ✅ Task 1.7 `SimplePermissionChecker`（owner/private 判定）
- ✅ Task 1.8 `PlainCredentialStore`（恒等 encrypt/decrypt + 末3位脱敏）
- ✅ Task 3.1 Agent CRUD 主体流程（创建/列表/详情/更新/软删除 + 乐观锁）
- ✅ Task 3.2 Agent 复制（深拷贝 bindings + references）
- ✅ Task 3.3 Agent 导入/导出（单向，受 #7 影响不完整）
- ✅ Task 3.4 版本列表 + 回滚
- ✅ Task 4.1 会话 CRUD（创建/列表/详情/清空消息）
- ✅ Task 4.3 `SseEventBuilder`（全 8 种事件类型，seq 单调递增）
- ✅ Task 4.4 SSE 基础流程（message_start → token* → message_end）
- ✅ Task 4.6 step_limit 事件 + `chat_session_states` 持久化（逻辑正确）
- ✅ Task 4.8 switchAgent（separator 消息 + 上下文边界）
- ✅ Task 4.9 `IdempotencyService`（实现完整，仅集成缺失）
- ✅ Task 4.10 `SseEventCacheService`（实现完整，仅集成缺失）
