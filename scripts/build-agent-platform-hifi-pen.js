const fs = require("fs");
const path = require("path");

const outputPath = path.join(__dirname, "..", "chatgpt-hifi.pen");

const PAGE_W = 1440;
const PAGE_H = 1024;
const PAGE_GAP_X = 96;
const PAGE_GAP_Y = 120;
const GRID_COLS = 4;

const COLORS = {
  canvas: "#EEF3F9",
  page: "#F7FAFE",
  surface: "#FFFFFF",
  surfaceAlt: "#F2F6FC",
  surfaceMuted: "#FAFCFF",
  surfaceDark: "#0F172A",
  text: "#152033",
  textSub: "#5A6881",
  textSoft: "#8090A7",
  line: "#D7E0EC",
  lineStrong: "#C5D2E3",
  blue: "#0B63F6",
  blueSoft: "#EAF1FF",
  green: "#169B62",
  greenSoft: "#EAF8F0",
  amber: "#D97706",
  amberSoft: "#FFF5E8",
  red: "#DC2626",
  redSoft: "#FDECEC",
  purple: "#7C3AED",
  purpleSoft: "#F2ECFF",
  teal: "#0F9F9A",
  tealSoft: "#E8FBFA",
  slate: "#334155",
};

const FONT = {
  heading: "Geist",
  body: "Funnel Sans",
  mono: "IBM Plex Mono",
};

const REQUIREMENTS = [
  {
    code: "R1",
    title: "智能体管理",
    color: COLORS.blue,
    accentSoft: COLORS.blueSoft,
    screens: [
      ["R1-1", "创建智能体入口引导", "系统应展示智能体创建的引导流程。", "wizard"],
      ["R1-2", "创建智能体基础信息", "系统应提供名称、描述、模型选择、最大步骤数配置表单。", "form"],
      ["R1-3", "协作智能体与工具配置", "系统应允许用户配置协作智能体和可用工具。", "split-config"],
      ["R1-4", "工具分组与参数配置", "系统应提供工具分组列表，支持刷新工具列表和参数配置。", "tool-groups"],
      ["R1-5", "保存并调试智能体", "系统应保存智能体并提供调试功能进行测试验证。", "debug"],
      ["R1-6", "智能体列表页面", "系统应展示用户拥有的所有智能体卡片，包括状态和操作按钮。", "card-list"],
      ["R1-7", "编辑智能体", "系统应跳转到智能体配置界面进行修改。", "edit-form"],
      ["R1-8", "复制智能体", "系统应创建智能体副本并添加到列表。", "duplicate"],
      ["R1-9", "删除智能体确认", "系统应弹出确认对话框，确认后删除智能体。", "confirm-delete"],
      ["R1-10", "导出智能体 JSON", "系统应生成 JSON 配置文件供下载。", "export"],
      ["R1-11", "导入并校验智能体配置", "系统应验证文件格式并创建智能体副本。", "import-validate"],
    ],
  },
  {
    code: "R2",
    title: "智能体市场发布与发现",
    color: COLORS.purple,
    accentSoft: COLORS.purpleSoft,
    screens: [
      ["R2-1", "调试完成后可发布", "系统应提供发布按钮。", "publish-ready"],
      ["R2-2", "发布确认对话框", "系统应弹出发布确认对话框，包含公开 / 私有选项。", "publish-modal"],
      ["R2-3", "公开发布成功", "系统应将智能体发布到公开市场，状态变更为已发布。", "publish-success"],
      ["R2-4", "智能体市场列表", "系统应展示所有公开发布的智能体列表，支持分类浏览。", "market-grid"],
      ["R2-5", "搜索智能体", "系统应返回匹配的智能体结果。", "search-results"],
      ["R2-6", "智能体详情", "系统应展示名称、描述、作者、工具列表和使用统计。", "detail"],
      ["R2-7", "点击使用启动会话", "系统应创建对话会话并启动该智能体。", "chat-launch"],
      ["R2-8", "收藏智能体", "系统应将智能体添加到用户收藏列表。", "favorite"],
      ["R2-9", "版本历史与回滚", "系统应支持版本管理，允许查看历史版本和回滚。", "version-history"],
      ["R2-10", "发布新版本提示", "系统应提示用户是否发布新版本。", "update-prompt"],
    ],
  },
  {
    code: "R3",
    title: "Skill 管理",
    color: COLORS.green,
    accentSoft: COLORS.greenSoft,
    screens: [
      ["R3-1", "Skill 列表", "系统应展示用户拥有的所有 Skill 卡片列表。", "card-list"],
      ["R3-2", "创建 Skill 表单", "系统应提供 Skill 创建表单，包括名称、描述、触发条件和能力定义。", "form"],
      ["R3-3", "YAML / Markdown 编辑器", "系统应提供 YAML / Markdown 格式编辑器用于定义 Skill 内容。", "editor"],
      ["R3-4", "保存 Skill 并校验", "系统应保存 Skill 并验证格式正确性。", "save-success"],
      ["R3-5", "编辑 Skill", "系统应打开 Skill 编辑界面。", "edit-form"],
      ["R3-6", "删除 Skill", "系统应弹出确认对话框后删除。", "confirm-delete"],
      ["R3-7", "导出 Skill 配置", "系统应生成 Skill 配置文件。", "export"],
    ],
  },
  {
    code: "R4",
    title: "Skill 市场发布与发现",
    color: COLORS.amber,
    accentSoft: COLORS.amberSoft,
    screens: [
      ["R4-1", "Skill 市场列表", "系统应展示所有公开发布的 Skill 列表，支持分类和搜索。", "market-grid"],
      ["R4-2", "Skill 详情", "系统应展示 Skill 详细信息，包括描述、使用场景和示例。", "detail"],
      ["R4-3", "导入 Skill", "系统应将 Skill 复制到用户资产列表。", "import-success"],
      ["R4-4", "发布 Skill", "系统应将 Skill 发布到公开市场。", "publish-success"],
      ["R4-5", "次数与评分统计", "系统应展示使用次数和评分统计。", "stats-detail"],
      ["R4-6", "用户评分", "系统应记录评分并更新统计。", "rating"],
    ],
  },
  {
    code: "R5",
    title: "MCP 管理",
    color: "#E11D48",
    accentSoft: "#FFEAF0",
    screens: [
      ["R5-1", "MCP 列表", "系统应展示用户配置的所有 MCP 服务器列表。", "server-list"],
      ["R5-2", "添加 MCP 方式选择", "系统应提供 URL 和 JSON 两种添加方式。", "choice"],
      ["R5-3", "URL 方式添加 MCP", "系统应提供 Server 名称和 URL 输入框，支持 SSE 和 Streamable HTTP。", "url-form"],
      ["R5-4", "JSON 方式添加 MCP", "系统应提供 JSON 编辑器和端口号输入框。", "json-form"],
      ["R5-5", "校验并保存 MCP", "系统应验证配置有效性并保存信息。", "save-success"],
      ["R5-6", "测试连接", "系统应测试 MCP 连接状态并返回结果。", "test-result"],
      ["R5-7", "删除 MCP", "系统应弹出确认后删除 MCP 配置。", "confirm-delete"],
      ["R5-8", "启用 / 禁用 MCP", "系统应更新 MCP 服务器的可用状态。", "toggle-list"],
      ["R5-9", "连接失败与重连", "系统应展示错误信息并提供重连选项。", "error-retry"],
    ],
  },
  {
    code: "R6",
    title: "MCP 市场发布与发现",
    color: COLORS.blue,
    accentSoft: COLORS.blueSoft,
    screens: [
      ["R6-1", "MCP 市场列表", "系统应展示所有可用的公开 MCP 服务列表。", "market-grid"],
      ["R6-2", "搜索 MCP", "系统应返回匹配的 MCP 结果。", "search-results"],
      ["R6-3", "MCP 详情", "系统应展示服务描述、工具列表和使用说明。", "detail"],
      ["R6-4", "一键接入 MCP", "系统应自动配置 MCP 并添加到用户资产列表。", "import-success"],
      ["R6-5", "MCP 参数配置", "系统应在接入后弹出参数配置对话框。", "param-modal"],
      ["R6-6", "MCP 更新提示", "系统应提示用户更新版本。", "update-prompt"],
    ],
  },
  {
    code: "R7",
    title: "Web 对话界面",
    color: COLORS.teal,
    accentSoft: COLORS.tealSoft,
    screens: [
      ["R7-1", "对话页面", "系统应展示消息区域、输入框和智能体选择器。", "chat"],
      ["R7-2", "智能体选择器", "系统应展示可用智能体列表，支持搜索和分类筛选。", "selector"],
      ["R7-3", "发送消息", "系统应将消息传递给选定的智能体并启动响应。", "chat-send"],
      ["R7-4", "流式输出", "系统应实时展示响应内容，支持流式输出显示。", "streaming"],
      ["R7-5", "工具执行状态", "系统应展示工具执行状态和结果。", "tool-status"],
      ["R7-6", "重新生成消息", "系统应重新请求智能体生成响应。", "regen"],
      ["R7-7", "复制消息", "系统应复制消息内容到剪贴板。", "copy"],
      ["R7-8", "清空对话确认", "系统应弹出确认后清空当前对话历史。", "confirm-delete"],
      ["R7-9", "最大步数超限", "系统应提示用户并停止响应。", "limit-warning"],
      ["R7-10", "切换智能体保留历史", "系统应保留当前对话历史并切换智能体上下文。", "chat-switch"],
    ],
  },
  {
    code: "R8",
    title: "知识库管理",
    color: "#8B5CF6",
    accentSoft: "#F2ECFF",
    screens: [
      ["R8-1", "知识库列表", "系统应展示用户拥有的所有知识库列表。", "card-list"],
      ["R8-2", "创建知识库表单", "系统应提供名称、描述和索引配置。", "form"],
      ["R8-3", "上传并建立索引", "系统应解析文档内容并建立索引。", "upload-index"],
      ["R8-4", "管理文档列表", "系统应展示知识库中的所有文档列表。", "doc-list"],
      ["R8-5", "删除文档并更新索引", "系统应更新索引并移除文档。", "doc-delete"],
      ["R8-6", "测试检索", "系统应提供检索测试界面，输入查询返回结果。", "retrieve-test"],
      ["R8-7", "绑定知识库到智能体", "系统应允许选择知识库检索工具并配置目标知识库。", "bind-kb"],
      ["R8-8", "索引失败重试", "系统应展示错误信息并支持重试。", "error-retry"],
    ],
  },
  {
    code: "R9",
    title: "用户认证与权限",
    color: "#F97316",
    accentSoft: "#FFF1E8",
    screens: [
      ["R9-1", "登录 / 注册", "系统应提供登录 / 注册界面。", "auth"],
      ["R9-2", "登录成功加载资产", "系统应生成会话令牌并加载用户资产。", "session-load"],
      ["R9-3", "资产自动归属", "系统应自动设置资产所有者为当前用户。", "ownership"],
      ["R9-4", "权限选项设置", "系统应提供同组可编辑、同组只读、私有三种权限。", "permission-form"],
      ["R9-5", "无权限访问", "系统应拒绝访问并提示权限不足。", "forbidden"],
      ["R9-6", "用户管理页面", "系统应展示用户列表和权限配置选项。", "admin-table"],
      ["R9-7", "即时更新权限", "系统应即时更新权限配置。", "save-success"],
    ],
  },
  {
    code: "R10",
    title: "资产管理统一入口",
    color: COLORS.slate,
    accentSoft: "#EEF2F7",
    screens: [
      ["R10-1", "侧边栏三类资产", "系统应展示三大资产类别：智能体、Skill、MCP。", "nav"],
      ["R10-2", "导航切页", "系统应切换到对应的资产管理页面。", "nav-switch"],
      ["R10-3", "创建引导页", "系统应展示创建引导页面。", "wizard"],
      ["R10-4", "可视化创建卡片", "系统应提供可视化卡片引导用户选择创建类型。", "choice"],
      ["R10-5", "开始创建跳转", "系统应跳转到对应的创建表单页面。", "jump-form"],
      ["R10-6", "无创建权限", "系统应隐藏创建入口并提示权限不足。", "forbidden"],
    ],
  },
];

let nextId = 1;
function id(prefix) {
  return `${prefix}_${nextId++}`;
}

function text(x, y, content, opts = {}) {
  const node = {
    id: id("txt"),
    type: "text",
    x,
    y,
    layoutPosition: "absolute",
    content,
    fontFamily: opts.fontFamily || FONT.body,
    fontSize: opts.fontSize || 14,
    fill: opts.fill || COLORS.text,
  };
  if (opts.width) {
    node.width = opts.width;
    node.textGrowth = "fixed-width";
  }
  if (opts.fontWeight) node.fontWeight = opts.fontWeight;
  if (opts.lineHeight) node.lineHeight = opts.lineHeight;
  if (opts.letterSpacing !== undefined) node.letterSpacing = opts.letterSpacing;
  return node;
}

function frame(x, y, width, height, fill, opts = {}) {
  const node = {
    id: id("frm"),
    type: "frame",
    x,
    y,
    layoutPosition: "absolute",
    width,
    height,
    fill,
    layout: "none",
  };
  if (opts.cornerRadius !== undefined) node.cornerRadius = opts.cornerRadius;
  if (opts.stroke) node.stroke = opts.stroke;
  if (opts.clip !== undefined) node.clip = opts.clip;
  if (opts.children) node.children = opts.children;
  return node;
}

function lineCard(children, x, y, width, height, fill = COLORS.surfaceAlt, radius = 12, stroke = true) {
  children.push(
    frame(x, y, width, height, fill, {
      cornerRadius: radius,
      stroke: stroke
        ? { fill: COLORS.line, thickness: 1, align: "inside" }
        : undefined,
    }),
  );
}

function chip(children, x, y, label, fill, textFill, width = 88) {
  lineCard(children, x, y, width, 24, fill, 12, false);
  children.push(
    text(x + 12, y + 6, label, {
      fontFamily: FONT.mono,
      fontSize: 11,
      fontWeight: "700",
      fill: textFill,
      letterSpacing: 0.2,
    }),
  );
}

function button(children, x, y, label, fill, textFill, width = 112, secondary = false) {
  lineCard(children, x, y, width, 42, fill, 14, secondary);
  children.push(
    text(x + Math.max(18, (width - label.length * 14) / 2), y + 13, label, {
      fontSize: 14,
      fontWeight: "700",
      fill: textFill,
    }),
  );
}

function sectionTitle(children, x, y, title, sub) {
  children.push(text(x, y, title, { fontFamily: FONT.heading, fontSize: 20, fontWeight: "700" }));
  if (sub) {
    children.push(text(x, y + 28, sub, { width: 420, fontSize: 13, lineHeight: 1.45, fill: COLORS.textSub }));
  }
}

function pageShell(pageX, pageY, req, code, title, summary) {
  const children = [];

  children.push(
    frame(pageX, pageY, PAGE_W, PAGE_H, COLORS.page, {
      cornerRadius: 28,
      stroke: { fill: COLORS.line, thickness: 1, align: "inside" },
      clip: true,
    }),
  );
  children.push(frame(pageX, pageY, 92, PAGE_H, req.color));
  lineCard(children, pageX + 112, pageY + 20, 164, 24, req.accentSoft, 12, false);
  children.push(text(pageX + 126, pageY + 26, "Design System Review", { fontFamily: FONT.mono, fontSize: 11, fontWeight: "700", fill: req.color, letterSpacing: 0.3 }));
  children.push(text(pageX + 22, pageY + 26, "Agent", { fontFamily: FONT.heading, fontSize: 28, fontWeight: "700", fill: "#FFFFFF" }));
  children.push(text(pageX + 22, pageY + 64, "Platform", { fontFamily: FONT.heading, fontSize: 28, fontWeight: "700", fill: "#FFFFFF" }));
  children.push(text(pageX + 22, pageY + 118, req.code, { fontFamily: FONT.mono, fontSize: 12, fontWeight: "700", fill: "#DCE8FF", letterSpacing: 0.6 }));
  children.push(text(pageX + 22, pageY + 140, req.title, { width: 48, fontFamily: FONT.body, fontSize: 13, lineHeight: 1.4, fill: "#DCE8FF" }));

  const contentX = pageX + 122;
  const contentW = PAGE_W - 150;
  children.push(text(contentX, pageY + 24, code, { fontFamily: FONT.mono, fontSize: 12, fontWeight: "700", fill: req.color, letterSpacing: 0.4 }));
  children.push(text(contentX, pageY + 46, title, { width: 560, fontFamily: FONT.heading, fontSize: 30, fontWeight: "700" }));
  children.push(text(contentX, pageY + 92, summary, { width: 660, fontSize: 15, lineHeight: 1.45, fill: COLORS.textSub }));

  children.push(lineCardRoot(contentX, pageY + 142, contentW, PAGE_H - 176));

  return {
    children,
    contentX,
    contentY: pageY + 142,
    contentW,
    contentH: PAGE_H - 176,
  };
}

function lineCardRoot(x, y, width, height) {
  return frame(x, y, width, height, COLORS.surface, {
    cornerRadius: 24,
    stroke: { fill: COLORS.line, thickness: 1, align: "inside" },
  });
}

function sidebar(children, x, y, width, height, req) {
  lineCard(children, x, y, width, height, "#F9FBFF", 20, true);
  chip(children, x + 20, y + 20, "SYSTEM", req.accentSoft, req.color, 72);
  children.push(text(x + 20, y + 58, "统一导航", { fontFamily: FONT.heading, fontSize: 18, fontWeight: "700" }));
  children.push(text(x + 20, y + 84, "平台级入口、分类切换、创建入口与权限反馈。", { width: width - 40, fontSize: 12, lineHeight: 1.45, fill: COLORS.textSub }));
  const items = ["工作台", "Agents", "Skills", "MCP", "知识库", "市场", "权限"];
  items.forEach((item, index) => {
    const top = y + 132 + index * 46;
    lineCard(children, x + 18, top, width - 36, 34, index === 1 ? req.color : "#FFFFFF", 12, true);
    children.push(text(x + 32, top + 10, item, {
      fontSize: 13,
      fill: index === 1 ? "#FFFFFF" : COLORS.textSub,
      fontWeight: index === 1 ? "700" : "400",
    }));
  });
}

function headerStats(children, x, y, width, req) {
  lineCard(children, x, y, width, 84, COLORS.surfaceAlt, 20, true);
  children.push(text(x + 24, y + 18, "当前场景", { fontFamily: FONT.mono, fontSize: 12, fill: COLORS.textSub }));
  children.push(text(x + 24, y + 38, "可评审高保真页面", { fontFamily: FONT.heading, fontSize: 20, fontWeight: "700" }));
  lineCard(children, x + width - 324, y + 16, 84, 52, "#FFFFFF", 16, true);
  lineCard(children, x + width - 228, y + 16, 92, 52, req.accentSoft, 16, false);
  lineCard(children, x + width - 124, y + 16, 92, 52, "#F4F7FB", 16, false);
  children.push(text(x + width - 294, y + 26, "密度", { fontFamily: FONT.mono, fontSize: 11, fill: COLORS.textSub }));
  children.push(text(x + width - 294, y + 42, "Medium", { fontSize: 15, fontWeight: "700", fill: COLORS.text }));
  children.push(text(x + width - 202, y + 26, "状态", { fontFamily: FONT.mono, fontSize: 11, fill: COLORS.textSub }));
  children.push(text(x + width - 202, y + 42, "Ready", { fontSize: 15, fontWeight: "700", fill: req.color }));
  children.push(text(x + width - 98, y + 26, "模式", { fontFamily: FONT.mono, fontSize: 11, fill: COLORS.textSub }));
  children.push(text(x + width - 98, y + 42, "Review", { fontSize: 15, fontWeight: "700", fill: COLORS.text }));
}

function cardsGrid(children, x, y, width, count, req, titlePrefix) {
  const cardW = Math.floor((width - 24) / 3);
  for (let i = 0; i < count; i += 1) {
    const col = i % 3;
    const row = Math.floor(i / 3);
    const left = x + col * (cardW + 12);
    const top = y + row * 122;
    lineCard(children, left, top, cardW, 126, COLORS.surface, 18, true);
    chip(children, left + 18, top + 16, i % 2 === 0 ? "ACTIVE" : "DRAFT", i % 2 === 0 ? req.accentSoft : COLORS.surfaceAlt, i % 2 === 0 ? req.color : COLORS.textSub, 72);
    children.push(text(left + 18, top + 46, `${titlePrefix} ${i + 1}`, { fontFamily: FONT.heading, fontSize: 17, fontWeight: "700" }));
    children.push(text(left + 18, top + 72, "描述信息、状态标签、更新时间和操作入口。", { width: cardW - 36, fontSize: 13, lineHeight: 1.4, fill: COLORS.textSub }));
    lineCard(children, left + 18, top + 102, 56, 8, req.color, 4, false);
    lineCard(children, left + 82, top + 102, 56, 8, "#E5EAF3", 4, false);
    lineCard(children, left + 146, top + 102, 56, 8, "#E5EAF3", 4, false);
  }
}

function formPanel(children, x, y, width, height, req, withSidePanel = false) {
  const mainW = withSidePanel ? width - 304 : width;
  lineCard(children, x, y, mainW, height, COLORS.surface, 20, true);
  sectionTitle(children, x + 28, y + 24, "基础配置", "以统一字段样式、校验节奏和操作层级组织表单。");
  const fields = [
    "名称 / Name",
    "描述 / Description",
    "模型选择 / Model",
    "最大步骤数 / Max Steps",
    "权限 / Access",
  ];
  fields.forEach((label, index) => {
    const top = y + 92 + index * 76;
    children.push(text(x + 28, top, label, { fontFamily: FONT.body, fontSize: 14, fontWeight: "700" }));
    lineCard(children, x + 28, top + 24, mainW - 56, 38, index === 2 ? req.accentSoft : "#F9FBFE", 12, true);
  });
  button(children, x + 28, y + height - 74, "取消", "#EEF2F7", COLORS.textSub, 112, false);
  button(children, x + 152, y + height - 74, "保存", req.color, "#FFFFFF", 112, false);

  if (withSidePanel) {
    lineCard(children, x + width - 284, y, 284, height, "#FBFCFF", 20, true);
    chip(children, x + width - 256, y + 24, "SUMMARY", req.accentSoft, req.color, 92);
    children.push(text(x + width - 256, y + 58, "配置摘要", { fontFamily: FONT.heading, fontSize: 18, fontWeight: "700" }));
    ["模型：GPT-5.4", "协作 Agent：3", "工具组：4", "知识库：2"].forEach((row, index) => {
      children.push(text(x + width - 256, y + 64 + index * 28, row, { fontFamily: FONT.mono, fontSize: 12, fill: COLORS.textSub }));
    });
    lineCard(children, x + width - 256, y + 182, 228, 120, req.accentSoft, 16, false);
    children.push(text(x + width - 238, y + 202, "校验提示", { fontFamily: FONT.heading, fontSize: 15, fontWeight: "700", fill: req.color }));
    children.push(text(x + width - 238, y + 230, "缺失字段、权限提示、工具兼容性与发布前校验都集中展示。", {
      width: 190,
      fontSize: 13,
      lineHeight: 1.45,
      fill: COLORS.textSub,
    }));
  }
}

function detailLayout(children, x, y, width, height, req) {
  lineCard(children, x, y, 290, height, "#FBFCFF", 20, true);
  lineCard(children, x + 314, y, width - 314, height, COLORS.surface, 20, true);
  lineCard(children, x + 24, y + 24, 242, 138, req.accentSoft, 18, false);
  chip(children, x + 24, y + 178, "METADATA", req.accentSoft, req.color, 92);
  children.push(text(x + 24, y + 186, "基本资料", { fontFamily: FONT.heading, fontSize: 18, fontWeight: "700" }));
  ["作者 / Author", "类目 / Category", "版本 / Version", "统计 / Stats"].forEach((row, index) => {
    children.push(text(x + 24, y + 220 + index * 28, row, { fontFamily: FONT.body, fontSize: 14, fill: COLORS.textSub }));
  });
  children.push(text(x + 342, y + 26, "详情概览", { fontFamily: FONT.heading, fontSize: 24, fontWeight: "700" }));
  children.push(text(x + 342, y + 62, "说明、示例、工具、更新记录和用户操作都汇聚在同一页面。", {
    width: width - 370,
    fontSize: 14,
    lineHeight: 1.45,
    fill: COLORS.textSub,
  }));
  lineCard(children, x + 342, y + 110, width - 370, 100, "#F9FBFE", 16, true);
  lineCard(children, x + 342, y + 226, width - 370, 160, "#F9FBFE", 16, true);
  button(children, x + 342, y + 402, "主操作", req.color, "#FFFFFF", 140, false);
  button(children, x + 494, y + 402, "次操作", "#EEF2F7", COLORS.textSub, 140, false);
}

function marketGrid(children, x, y, width, height, req) {
  lineCard(children, x, y, width, 72, COLORS.surfaceAlt, 18, true);
  lineCard(children, x + 24, y + 18, 320, 36, "#FFFFFF", 12, true);
  button(children, x + 364, y + 18, "全部", req.accentSoft, req.color, 88, false);
  button(children, x + 462, y + 18, "热门", "#EEF2F7", COLORS.textSub, 88, false);
  button(children, x + 560, y + 18, "最新", "#EEF2F7", COLORS.textSub, 88, false);
  cardsGrid(children, x, y + 96, width, 6, req, "资产");
}

function modal(children, x, y, width, height, req, tone = "normal") {
  lineCard(children, x, y, width, height, "#E9EEF5", 20, false);
  const modalFill = tone === "danger" ? COLORS.redSoft : tone === "warn" ? COLORS.amberSoft : "#FFFFFF";
  lineCard(children, x + 180, y + 92, width - 360, height - 184, modalFill, 24, true);
  children.push(text(x + 220, y + 126, "确认操作", { fontFamily: FONT.heading, fontSize: 26, fontWeight: "700" }));
  children.push(text(x + 220, y + 168, "操作说明、影响范围、权限和后续动作都在弹窗中解释清楚。", {
    width: width - 440,
    fontSize: 15,
    lineHeight: 1.45,
    fill: COLORS.textSub,
  }));
  lineCard(children, x + 220, y + 242, width - 440, 92, "#F9FBFE", 16, true);
  button(children, x + width - 460, y + height - 154, "取消", "#EEF2F7", COLORS.textSub, 120, false);
  button(children, x + width - 326, y + height - 154, "确认", req.color, "#FFFFFF", 120, false);
}

function chatLayout(children, x, y, width, height, req, variant = "base") {
  lineCard(children, x, y, 250, height, "#F9FBFF", 20, true);
  lineCard(children, x + 274, y, width - 274, height, COLORS.surface, 20, true);
  chip(children, x + 18, y + 18, "AGENTS", req.accentSoft, req.color, 84);
  ["Revenue Ops", "Support QA", "Finance Agent", "Growth Writer"].forEach((label, index) => {
    const top = y + 84 + index * 46;
    lineCard(children, x + 18, top, 214, 34, index === 0 ? req.color : "#FFFFFF", 12, true);
    children.push(text(x + 34, top + 10, label, { fontSize: 13, fill: index === 0 ? "#FFFFFF" : COLORS.textSub, fontWeight: index === 0 ? "700" : "400" }));
  });
  lineCard(children, x + 298, y + 18, width - 310, 54, COLORS.surfaceAlt, 16, true);
  lineCard(children, x + 298, y + 94, width - 310, height - 198, "#FBFCFF", 16, true);
  lineCard(children, x + 336, y + 124, width - 420, 64, req.accentSoft, 18, false);
  lineCard(children, x + 298 + (width - 420) / 2, y + 210, width - 420, 82, "#EEF2F7", 18, false);
  lineCard(children, x + 336, y + 314, width - 420, 74, "#ECF8F3", 18, false);
  lineCard(children, x + 298, y + height - 88, width - 400, 50, "#FFFFFF", 16, true);
  button(children, x + width - 176, y + height - 88, "发送", req.color, "#FFFFFF", 100, false);
  children.push(text(x + 330, y + height - 72, "继续补充输入、工具调用或上下文。", { fontSize: 14, fill: COLORS.textSub }));

  if (variant === "streaming") {
    lineCard(children, x + 336, y + 414, width - 420, 94, "#EEF4FF", 18, false);
    children.push(text(x + 356, y + 438, "流式生成中 · token 正在持续写入", { fontFamily: FONT.mono, fontSize: 12, fill: req.color }));
  } else if (variant === "tools") {
    lineCard(children, x + 336, y + 414, width - 420, 108, "#EAF8F0", 18, false);
    children.push(text(x + 356, y + 438, "tool.run / knowledge.search / crm.pipeline.get", { fontFamily: FONT.mono, fontSize: 12, fill: COLORS.green }));
  } else if (variant === "limit") {
    lineCard(children, x + 336, y + 414, width - 420, 88, COLORS.amberSoft, 18, false);
    children.push(text(x + 356, y + 438, "超过最大步数，已停止继续推理并等待用户处理。", { width: width - 470, fontSize: 14, fill: COLORS.amber }));
  } else if (variant === "selector") {
    lineCard(children, x + 560, y + 110, 340, 320, "#FFFFFF", 20, true);
    lineCard(children, x + 584, y + 138, 292, 36, COLORS.surfaceAlt, 12, true);
    ["销售", "客服", "财务", "工程"].forEach((item, index) => {
      lineCard(children, x + 584, y + 190 + index * 44, 292, 34, index === 1 ? req.accentSoft : "#FFFFFF", 12, true);
      children.push(text(x + 602, y + 200 + index * 44, item, { fontSize: 13, fill: COLORS.textSub }));
    });
  }
}

function tableLayout(children, x, y, width, height, req, rows = 5) {
  lineCard(children, x, y, width, height, COLORS.surface, 20, true);
  lineCard(children, x + 20, y + 20, width - 40, 40, COLORS.surfaceAlt, 12, false);
  for (let i = 0; i < rows; i += 1) {
    const top = y + 72 + i * 64;
    lineCard(children, x + 20, top, width - 40, 52, i === 0 ? req.accentSoft : "#FFFFFF", 12, true);
    lineCard(children, x + 40, top + 16, 140, 8, i === 0 ? req.color : COLORS.lineStrong, 4, false);
    lineCard(children, x + 210, top + 16, 220, 8, "#E5EAF3", 4, false);
    lineCard(children, x + width - 184, top + 14, 120, 24, i === 0 ? req.accentSoft : COLORS.surfaceAlt, 12, false);
  }
}

function ratingLayout(children, x, y, width, height, req) {
  lineCard(children, x, y, width, height, COLORS.surface, 20, true);
  children.push(text(x + 28, y + 26, "评分反馈", { fontFamily: FONT.heading, fontSize: 24, fontWeight: "700" }));
  lineCard(children, x + 28, y + 74, width - 56, 110, req.accentSoft, 18, false);
  for (let i = 0; i < 5; i += 1) {
    lineCard(children, x + 40 + i * 68, y + 112, 48, 48, i < 4 ? req.color : "#DCE4F0", 24, false);
  }
  lineCard(children, x + 28, y + 206, width - 56, 118, "#F9FBFE", 16, true);
  lineCard(children, x + width - 170, y + height - 70, 120, 42, req.color, 14, false);
}

function publishSuccess(children, x, y, width, height, req) {
  lineCard(children, x, y, width, height, COLORS.surface, 20, true);
  lineCard(children, x + 28, y + 28, width - 56, 142, req.accentSoft, 20, false);
  children.push(text(x + 50, y + 56, "发布成功", { fontFamily: FONT.heading, fontSize: 28, fontWeight: "700", fill: req.color }));
  children.push(text(x + 50, y + 98, "状态已变更为已发布，可继续管理版本、查看市场反馈和设置公开范围。", {
    width: width - 100,
    fontSize: 15,
    lineHeight: 1.45,
    fill: COLORS.textSub,
  }));
  lineCard(children, x + 28, y + 196, width - 56, 120, "#FFFFFF", 16, true);
  lineCard(children, x + 28, y + 336, 156, 44, req.color, 14, false);
  lineCard(children, x + 198, y + 336, 156, 44, "#EEF2F7", 14, false);
}

function importExport(children, x, y, width, height, req, mode) {
  lineCard(children, x, y, width, height, COLORS.surface, 20, true);
  lineCard(children, x + 28, y + 28, width - 56, 60, COLORS.surfaceAlt, 16, true);
  lineCard(children, x + 28, y + 106, width - 56, 212, "#F8FAFD", 16, true);
  if (mode === "import") {
    lineCard(children, x + 48, y + 128, width - 96, 88, req.accentSoft, 16, false);
    lineCard(children, x + 48, y + 236, width - 96, 58, COLORS.amberSoft, 16, false);
    lineCard(children, x + width - 170, y + height - 70, 120, 42, req.color, 14, false);
  } else {
    lineCard(children, x + 48, y + 128, width - 96, 140, "#0F172A", 16, false);
    lineCard(children, x + width - 170, y + height - 70, 120, 42, req.color, 14, false);
  }
}

function splitConfigLayout(children, x, y, width, height, req) {
  lineCard(children, x, y, 250, height, "#FBFCFF", 20, true);
  lineCard(children, x + 270, y, width - 270, height, COLORS.surface, 20, true);
  ["协作 Agent", "工具组", "知识库", "发布设置"].forEach((label, index) => {
    const top = y + 22 + index * 54;
    lineCard(children, x + 18, top, 214, 38, index === 1 ? req.color : "#FFFFFF", 12, true);
    children.push(text(x + 34, top + 11, label, { fontSize: 13, fill: index === 1 ? "#FFFFFF" : COLORS.textSub, fontWeight: index === 1 ? "700" : "400" }));
  });
  lineCard(children, x + 296, y + 24, width - 322, 60, COLORS.surfaceAlt, 16, true);
  lineCard(children, x + 296, y + 104, width - 322, 170, "#FFFFFF", 16, true);
  lineCard(children, x + 296, y + 294, width - 322, 138, req.accentSoft, 16, false);
  lineCard(children, x + width - 184, y + height - 70, 140, 42, req.color, 14, false);
}

function toolGroupsLayout(children, x, y, width, height, req) {
  lineCard(children, x, y, 250, height, "#FBFCFF", 20, true);
  lineCard(children, x + 270, y, width - 270, height, COLORS.surface, 20, true);
  ["Knowledge", "Business", "Release", "System"].forEach((label, index) => {
    const top = y + 22 + index * 52;
    lineCard(children, x + 18, top, 214, 36, index === 0 ? req.color : "#FFFFFF", 12, true);
    children.push(text(x + 34, top + 10, label, { fontSize: 13, fill: index === 0 ? "#FFFFFF" : COLORS.textSub, fontWeight: index === 0 ? "700" : "400" }));
  });
  lineCard(children, x + 294, y + 22, width - 318, 56, COLORS.surfaceAlt, 16, true);
  lineCard(children, x + 294, y + 94, width - 318, 136, "#FFFFFF", 16, true);
  lineCard(children, x + 294, y + 246, width - 318, 138, "#F9FBFE", 16, true);
  lineCard(children, x + 314, y + 270, 120, 36, req.color, 12, false);
  lineCard(children, x + 448, y + 270, 120, 36, "#EEF2F7", 12, false);
}

function choiceLayout(children, x, y, width, height, req) {
  lineCard(children, x, y, width, height, COLORS.surface, 20, true);
  lineCard(children, x + 28, y + 28, width - 56, 64, COLORS.surfaceAlt, 16, true);
  const cardW = Math.floor((width - 80) / 3);
  ["创建 Agent", "创建 Skill", "接入 MCP"].forEach((label, index) => {
    const left = x + 28 + index * (cardW + 12);
    lineCard(children, left, y + 116, cardW, 220, index === 0 ? req.accentSoft : "#FFFFFF", 18, true);
    lineCard(children, left + 18, y + 136, 56, 18, req.color, 9, false);
    children.push(text(left + 18, y + 170, label, { fontFamily: FONT.heading, fontSize: 20, fontWeight: "700" }));
    children.push(text(left + 18, y + 204, "说明文案、能力边界、适用场景和开始入口。", {
      width: cardW - 36,
      fontSize: 14,
      lineHeight: 1.45,
      fill: COLORS.textSub,
    }));
    lineCard(children, left + 18, y + 280, 96, 36, index === 0 ? req.color : "#EEF2F7", 12, false);
  });
}

function renderKind(children, x, y, width, height, req, kind) {
  switch (kind) {
    case "wizard":
      choiceLayout(children, x, y + 98, width, height - 98, req);
      break;
    case "form":
    case "edit-form":
    case "jump-form":
      formPanel(children, x, y + 98, width, height - 98, req, kind !== "jump-form");
      break;
    case "split-config":
      splitConfigLayout(children, x, y + 98, width, height - 98, req);
      break;
    case "tool-groups":
      toolGroupsLayout(children, x, y + 98, width, height - 98, req);
      break;
    case "debug":
    case "test-result":
      lineCard(children, x, y + 98, width, height - 98, COLORS.surface, 20, true);
      lineCard(children, x + 28, y + 126, width - 56, 84, req.accentSoft, 18, false);
      lineCard(children, x + 28, y + 230, width - 56, 236, "#FBFCFF", 18, true);
      lineCard(children, x + width - 188, y + height - 78, 140, 42, req.color, 14, false);
      break;
    case "card-list":
    case "server-list":
      headerStats(children, x, y, width, req);
      cardsGrid(children, x, y + 114, width, 6, req, kind === "server-list" ? "Server" : "资产");
      break;
    case "duplicate":
      headerStats(children, x, y, width, req);
      cardsGrid(children, x, y + 114, width, 3, req, "Agent");
      lineCard(children, x + width - 330, y + 438, 290, 108, req.accentSoft, 18, false);
      break;
    case "confirm-delete":
      modal(children, x, y + 30, width, height - 30, req, "danger");
      break;
    case "export":
      importExport(children, x, y + 98, width, height - 98, req, "export");
      break;
    case "import-validate":
      importExport(children, x, y + 98, width, height - 98, req, "import");
      break;
    case "publish-ready":
      formPanel(children, x, y + 98, width, height - 98, req, true);
      lineCard(children, x + width - 176, y + 116, 128, 40, req.color, 14, false);
      children.push(text(x + width - 136, y + 128, "发布", { fontSize: 14, fontWeight: "700", fill: "#FFFFFF" }));
      break;
    case "publish-modal":
      modal(children, x, y + 30, width, height - 30, req, "normal");
      break;
    case "publish-success":
    case "import-success":
    case "save-success":
      publishSuccess(children, x, y + 98, width, height - 98, req);
      break;
    case "market-grid":
      headerStats(children, x, y, width, req);
      marketGrid(children, x, y + 104, width, height - 104, req);
      break;
    case "search-results":
      headerStats(children, x, y, width, req);
      lineCard(children, x, y + 104, width, 72, COLORS.surfaceAlt, 18, true);
      lineCard(children, x + 24, y + 122, width - 48, 36, "#FFFFFF", 12, true);
      tableLayout(children, x, y + 198, width, height - 198, req, 5);
      break;
    case "detail":
    case "stats-detail":
      headerStats(children, x, y, width, req);
      detailLayout(children, x, y + 104, width, height - 104, req);
      break;
    case "chat-launch":
    case "chat":
    case "chat-send":
    case "chat-switch":
      chatLayout(children, x, y + 98, width, height - 98, req, "base");
      break;
    case "favorite":
      headerStats(children, x, y, width, req);
      marketGrid(children, x, y + 104, width, height - 104, req);
      lineCard(children, x + width - 208, y + 216, 156, 96, req.accentSoft, 18, false);
      break;
    case "version-history":
      headerStats(children, x, y, width, req);
      lineCard(children, x, y + 104, 280, height - 104, "#FBFCFF", 20, true);
      tableLayout(children, x + 304, y + 104, width - 304, height - 104, req, 6);
      break;
    case "update-prompt":
      headerStats(children, x, y, width, req);
      lineCard(children, x, y + 104, width, 96, COLORS.amberSoft, 18, false);
      lineCard(children, x, y + 220, width, height - 220, COLORS.surface, 20, true);
      break;
    case "editor":
    case "json-form":
      lineCard(children, x, y + 98, 280, height - 98, "#FBFCFF", 20, true);
      lineCard(children, x + 304, y + 98, width - 304, height - 98, COLORS.surfaceDark, 20, false);
      lineCard(children, x + 324, y + 126, width - 344, 40, "#1E293B", 14, false);
      for (let i = 0; i < 10; i += 1) {
        children.push(text(x + 330, y + 192 + i * 34, `${i + 1}`, { fontFamily: FONT.mono, fontSize: 12, fill: "#64748B" }));
        lineCard(children, x + 362, y + 190 + i * 34, width - 414, 12, i % 2 === 0 ? "#60A5FA" : "#A78BFA", 6, false);
      }
      break;
    case "choice":
      choiceLayout(children, x, y + 98, width, height - 98, req);
      break;
    case "url-form":
      formPanel(children, x, y + 98, width, height - 98, req, false);
      lineCard(children, x + 28, y + 332, 220, 38, req.accentSoft, 12, false);
      lineCard(children, x + 260, y + 332, 220, 38, req.accentSoft, 12, false);
      break;
    case "toggle-list":
      tableLayout(children, x, y + 98, width, height - 98, req, 6);
      lineCard(children, x + width - 90, y + 130, 42, 22, req.color, 11, false);
      lineCard(children, x + width - 90, y + 194, 42, 22, "#CBD5E1", 11, false);
      break;
    case "error-retry":
    case "forbidden":
      lineCard(children, x, y + 98, width, 96, kind === "forbidden" ? COLORS.redSoft : COLORS.amberSoft, 18, false);
      lineCard(children, x, y + 216, width, height - 216, COLORS.surface, 20, true);
      lineCard(children, x + width - 188, y + height - 72, 140, 42, req.color, 14, false);
      break;
    case "selector":
      chatLayout(children, x, y + 98, width, height - 98, req, "selector");
      break;
    case "streaming":
      chatLayout(children, x, y + 98, width, height - 98, req, "streaming");
      break;
    case "tool-status":
      chatLayout(children, x, y + 98, width, height - 98, req, "tools");
      break;
    case "regen":
    case "copy":
      chatLayout(children, x, y + 98, width, height - 98, req, "base");
      lineCard(children, x + width - 248, y + 252, 196, 96, req.accentSoft, 18, false);
      break;
    case "limit-warning":
      chatLayout(children, x, y + 98, width, height - 98, req, "limit");
      break;
    case "upload-index":
      lineCard(children, x, y + 98, width, height - 98, COLORS.surface, 20, true);
      lineCard(children, x + 28, y + 126, width - 56, 120, req.accentSoft, 18, false);
      lineCard(children, x + 28, y + 268, width - 56, 182, "#F9FBFE", 18, true);
      lineCard(children, x + 28, y + 470, width - 56, 20, "#D7EAFE", 10, false);
      break;
    case "doc-list":
    case "admin-table":
      headerStats(children, x, y, width, req);
      tableLayout(children, x, y + 104, width, height - 104, req, 6);
      break;
    case "doc-delete":
      headerStats(children, x, y, width, req);
      tableLayout(children, x, y + 104, width, height - 104, req, 5);
      lineCard(children, x + width - 236, y + 226, 180, 92, COLORS.redSoft, 18, false);
      break;
    case "retrieve-test":
      lineCard(children, x, y + 98, width, 84, COLORS.surfaceAlt, 18, true);
      lineCard(children, x + 24, y + 118, width - 180, 42, "#FFFFFF", 12, true);
      lineCard(children, x + width - 140, y + 118, 116, 42, req.color, 12, false);
      lineCard(children, x, y + 202, width, height - 202, COLORS.surface, 20, true);
      lineCard(children, x + 24, y + 226, width - 48, 94, req.accentSoft, 16, false);
      break;
    case "bind-kb":
      splitConfigLayout(children, x, y + 98, width, height - 98, req);
      lineCard(children, x + 302, y + 308, 280, 48, req.accentSoft, 12, false);
      break;
    case "auth":
      lineCard(children, x + 220, y + 98, width - 440, height - 98, COLORS.surface, 26, true);
      lineCard(children, x + 260, y + 140, width - 520, 72, req.accentSoft, 18, false);
      formPanel(children, x + 260, y + 236, width - 520, height - 320, req, false);
      break;
    case "session-load":
      lineCard(children, x, y + 98, width, 110, req.accentSoft, 18, false);
      cardsGrid(children, x, y + 232, width, 6, req, "资产");
      break;
    case "ownership":
      formPanel(children, x, y + 98, width, height - 98, req, true);
      lineCard(children, x + width - 252, y + 292, 196, 110, req.accentSoft, 16, false);
      break;
    case "permission-form":
      formPanel(children, x, y + 98, width, height - 98, req, false);
      ["同组可编辑", "同组只读", "私有"].forEach((label, index) => {
        lineCard(children, x + 28, y + 432 + index * 54, width - 56, 40, index === 0 ? req.accentSoft : "#F9FBFE", 12, true);
        children.push(text(x + 46, y + 445 + index * 54, label, { fontSize: 14, fontWeight: "700", fill: index === 0 ? req.color : COLORS.textSub }));
      });
      break;
    case "nav":
    case "nav-switch":
      sidebar(children, x, y + 98, 250, height - 98, req);
      lineCard(children, x + 274, y + 98, width - 274, 84, COLORS.surfaceAlt, 20, true);
      cardsGrid(children, x + 274, y + 206, width - 274, 6, req, "资产");
      break;
    default:
      lineCard(children, x, y + 98, width, height - 98, COLORS.surface, 20, true);
      break;
  }
}

const pages = [];
let index = 0;

REQUIREMENTS.forEach((req) => {
  req.screens.forEach(([code, title, summary, kind]) => {
    const col = index % GRID_COLS;
    const row = Math.floor(index / GRID_COLS);
    const pageX = 80 + col * (PAGE_W + PAGE_GAP_X);
    const pageY = 80 + row * (PAGE_H + PAGE_GAP_Y);

    const shell = pageShell(0, 0, req, code, title, summary);
    renderKind(
      shell.children,
      shell.contentX + 24,
      shell.contentY + 24,
      shell.contentW - 48,
      shell.contentH - 48,
      req,
      kind,
    );

    pages.push({
      id: `${code}_page`,
      type: "group",
      name: `${code} ${title}`,
      x: pageX,
      y: pageY,
      children: shell.children,
    });

    index += 1;
  });
});

const totalRows = Math.ceil(index / GRID_COLS);
const rootWidth = 80 + GRID_COLS * PAGE_W + (GRID_COLS - 1) * PAGE_GAP_X + 80;
const rootHeight = 80 + totalRows * PAGE_H + (totalRows - 1) * PAGE_GAP_Y + 80;

const document = {
  version: "2.11",
  children: [
    {
      id: "canvasBg",
      type: "frame",
      x: 0,
      y: 0,
      width: rootWidth,
      height: rootHeight,
      layout: "none",
      fill: COLORS.canvas,
    },
    ...pages,
  ],
};

fs.writeFileSync(outputPath, JSON.stringify(document, null, 2));
console.log(`Wrote ${outputPath}`);
