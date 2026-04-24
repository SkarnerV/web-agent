/**
 * @schema 2.10
 */

const FONT_HEADING = "Geist";
const FONT_BODY = "Funnel Sans";
const FONT_MONO = "IBM Plex Mono";

const COLORS = {
  canvas: "#F3F6FB",
  rowBorder: "#D6DEEA",
  screen: "#FFFFFF",
  ink: "#162033",
  sub: "#5B667A",
  soft: "#EAF0F8",
  line: "#D9E2EF",
  modal: "#F8FBFF",
  success: "#EAF7EF",
  warn: "#FFF7E8",
  danger: "#FDECEC",
  blue: "#0B63F6",
};

const REQUIREMENTS = [
  {
    code: "R1",
    title: "智能体管理",
    color: "#0B63F6",
    screens: [
      ["R1-1", "创建入口引导", "wizard"],
      ["R1-2", "基础信息表单", "form"],
      ["R1-3", "协作体与工具配置", "split-config"],
      ["R1-4", "工具分组与参数", "tool-groups"],
      ["R1-5", "保存并调试", "debug"],
      ["R1-6", "智能体列表卡片", "card-list"],
      ["R1-7", "点击编辑", "edit-form"],
      ["R1-8", "点击复制", "duplicate"],
      ["R1-9", "点击删除确认", "confirm-delete"],
      ["R1-10", "导出 JSON", "export"],
      ["R1-11", "导入并校验", "import-validate"],
    ],
  },
  {
    code: "R2",
    title: "智能体市场发布与发现",
    color: "#7C3AED",
    screens: [
      ["R2-1", "配置完成显示发布", "publish-ready"],
      ["R2-2", "发布确认弹窗", "publish-modal"],
      ["R2-3", "公开发布成功", "publish-success"],
      ["R2-4", "市场列表分类", "market-grid"],
      ["R2-5", "市场搜索结果", "search-results"],
      ["R2-6", "智能体详情页", "detail"],
      ["R2-7", "点击使用启动会话", "chat-launch"],
      ["R2-8", "点击收藏", "favorite"],
      ["R2-9", "版本历史与回滚", "version-history"],
      ["R2-10", "提示发布新版本", "update-prompt"],
    ],
  },
  {
    code: "R3",
    title: "Skill 管理",
    color: "#0F9D58",
    screens: [
      ["R3-1", "Skill 列表", "card-list"],
      ["R3-2", "创建 Skill 表单", "form"],
      ["R3-3", "YAML / Markdown 编辑器", "editor"],
      ["R3-4", "保存并校验格式", "save-success"],
      ["R3-5", "编辑 Skill", "edit-form"],
      ["R3-6", "删除确认", "confirm-delete"],
      ["R3-7", "导出 Skill 配置", "export"],
    ],
  },
  {
    code: "R4",
    title: "Skill 市场发布与发现",
    color: "#F59E0B",
    screens: [
      ["R4-1", "Skill 市场列表", "market-grid"],
      ["R4-2", "Skill 详情", "detail"],
      ["R4-3", "导入到资产列表", "import-success"],
      ["R4-4", "发布到市场", "publish-success"],
      ["R4-5", "次数与评分统计", "stats-detail"],
      ["R4-6", "用户评分反馈", "rating"],
    ],
  },
  {
    code: "R5",
    title: "MCP 管理",
    color: "#E11D48",
    screens: [
      ["R5-1", "MCP 列表", "server-list"],
      ["R5-2", "添加方式选择", "choice"],
      ["R5-3", "URL 接入表单", "url-form"],
      ["R5-4", "JSON 配置编辑器", "json-form"],
      ["R5-5", "校验并保存", "save-success"],
      ["R5-6", "连接测试结果", "test-result"],
      ["R5-7", "删除 MCP", "confirm-delete"],
      ["R5-8", "启用 / 禁用切换", "toggle-list"],
      ["R5-9", "连接失败与重连", "error-retry"],
    ],
  },
  {
    code: "R6",
    title: "MCP 市场发布与发现",
    color: "#2563EB",
    screens: [
      ["R6-1", "MCP 市场列表", "market-grid"],
      ["R6-2", "搜索 MCP", "search-results"],
      ["R6-3", "MCP 详情说明", "detail"],
      ["R6-4", "一键接入", "import-success"],
      ["R6-5", "参数配置弹窗", "param-modal"],
      ["R6-6", "版本更新提示", "update-prompt"],
    ],
  },
  {
    code: "R7",
    title: "Web 对话界面",
    color: "#14B8A6",
    screens: [
      ["R7-1", "对话页面", "chat"],
      ["R7-2", "智能体选择器", "selector"],
      ["R7-3", "发送消息", "chat-send"],
      ["R7-4", "流式输出", "streaming"],
      ["R7-5", "工具执行状态", "tool-status"],
      ["R7-6", "重新生成", "regen"],
      ["R7-7", "复制消息", "copy"],
      ["R7-8", "清空对话确认", "confirm-delete"],
      ["R7-9", "超出最大步数", "limit-warning"],
      ["R7-10", "切换智能体保留历史", "chat-switch"],
    ],
  },
  {
    code: "R8",
    title: "知识库管理",
    color: "#8B5CF6",
    screens: [
      ["R8-1", "知识库列表", "card-list"],
      ["R8-2", "创建知识库表单", "form"],
      ["R8-3", "上传并建索引", "upload-index"],
      ["R8-4", "管理文档列表", "doc-list"],
      ["R8-5", "删除文档并更新索引", "doc-delete"],
      ["R8-6", "测试检索界面", "retrieve-test"],
      ["R8-7", "智能体绑定知识库", "bind-kb"],
      ["R8-8", "索引失败重试", "error-retry"],
    ],
  },
  {
    code: "R9",
    title: "用户认证与权限",
    color: "#F97316",
    screens: [
      ["R9-1", "登录 / 注册", "auth"],
      ["R9-2", "登录成功加载资产", "session-load"],
      ["R9-3", "创建资产自动归属", "ownership"],
      ["R9-4", "权限选项设置", "permission-form"],
      ["R9-5", "无权限访问", "forbidden"],
      ["R9-6", "管理员用户管理", "admin-table"],
      ["R9-7", "即时更新权限", "save-success"],
    ],
  },
  {
    code: "R10",
    title: "资产管理统一入口",
    color: "#334155",
    screens: [
      ["R10-1", "侧边栏三类资产", "nav"],
      ["R10-2", "点击导航切页", "nav-switch"],
      ["R10-3", "创建引导页", "wizard"],
      ["R10-4", "可视化创建卡片", "choice"],
      ["R10-5", "开始创建跳表单", "jump-form"],
      ["R10-6", "无创建权限", "forbidden"],
    ],
  },
];

const PADDING = 28;
const LABEL_W = 220;
const SCREEN_W = 236;
const SCREEN_H = 172;
const SCREEN_GAP = 18;
const ROW_GAP = 26;
const HEADER_H = 30;
const ROW_H = SCREEN_H + 42;

function textNode(x, y, width, content, size, fill, opts = {}) {
  const node = {
    type: "text",
    x,
    y,
    content,
    fontFamily: opts.fontFamily || FONT_BODY,
    fontSize: size,
    fill,
  };
  if (width) {
    node.width = width;
    node.textGrowth = "fixed-width";
    if (opts.lineHeight) node.lineHeight = opts.lineHeight;
  }
  if (opts.fontWeight) node.fontWeight = opts.fontWeight;
  if (opts.letterSpacing !== undefined) node.letterSpacing = opts.letterSpacing;
  return node;
}

function rectNode(x, y, width, height, fill, strokeFill = COLORS.line, radius = 12) {
  return {
    type: "frame",
    x,
    y,
    width,
    height,
    fill,
    stroke: { fill: strokeFill, thickness: 1, align: "inside" },
    cornerRadius: radius,
  };
}

function lineBlock(nodes, x, y, w, h, fill = COLORS.soft, radius = 8) {
  nodes.push({
    type: "frame",
    x,
    y,
    width: w,
    height: h,
    fill,
    cornerRadius: radius,
  });
}

function drawMiniScreen(x, y, code, title, kind, accent) {
  const nodes = [];
  nodes.push(rectNode(x, y, SCREEN_W, SCREEN_H, COLORS.screen, COLORS.rowBorder, 18));
  nodes.push({
    type: "frame",
    x,
    y,
    width: SCREEN_W,
    height: HEADER_H,
    fill: accent,
    cornerRadius: [18, 18, 0, 0],
  });
  nodes.push(textNode(x + 12, y + 8, 56, code, 11, "#FFFFFF", { fontFamily: FONT_MONO, letterSpacing: 0.3 }));
  nodes.push(textNode(x + 72, y + 8, SCREEN_W - 84, title, 12, "#FFFFFF", { fontWeight: "700" }));

  const ix = x + 12;
  const iy = y + HEADER_H + 10;
  const iw = SCREEN_W - 24;
  const ih = SCREEN_H - HEADER_H - 20;

  switch (kind) {
    case "wizard":
      lineBlock(nodes, ix, iy, iw, 24);
      lineBlock(nodes, ix, iy + 34, 64, 96, "#EEF4FF");
      lineBlock(nodes, ix + 74, iy + 34, iw - 74, 44);
      lineBlock(nodes, ix + 74, iy + 86, 78, 18, accent);
      lineBlock(nodes, ix + 160, iy + 86, 54, 18, COLORS.soft);
      break;
    case "form":
    case "edit-form":
    case "jump-form":
      lineBlock(nodes, ix, iy, iw, 18);
      lineBlock(nodes, ix, iy + 28, iw, 18);
      lineBlock(nodes, ix, iy + 56, iw, 18);
      lineBlock(nodes, ix, iy + 92, 84, 20, accent);
      break;
    case "split-config":
      lineBlock(nodes, ix, iy, 72, ih - 10);
      lineBlock(nodes, ix + 82, iy, iw - 82, 26);
      lineBlock(nodes, ix + 82, iy + 36, iw - 82, 18);
      lineBlock(nodes, ix + 82, iy + 64, iw - 82, 18);
      lineBlock(nodes, ix + 82, iy + 92, 90, 18, accent);
      break;
    case "tool-groups":
      lineBlock(nodes, ix, iy, 62, ih - 14);
      lineBlock(nodes, ix + 72, iy, iw - 72, 22);
      lineBlock(nodes, ix + 72, iy + 30, iw - 72, 18);
      lineBlock(nodes, ix + 72, iy + 56, iw - 72, 40);
      lineBlock(nodes, ix + 72, iy + 104, 56, 18, accent);
      lineBlock(nodes, ix + 136, iy + 104, 56, 18, COLORS.soft);
      break;
    case "debug":
    case "test-result":
      lineBlock(nodes, ix, iy, iw, 26);
      lineBlock(nodes, ix, iy + 36, iw, 52, "#EEF7EE");
      lineBlock(nodes, ix, iy + 96, 82, 18, accent);
      break;
    case "card-list":
    case "market-grid":
    case "server-list":
      lineBlock(nodes, ix, iy, iw, 20);
      lineBlock(nodes, ix, iy + 30, (iw - 8) / 2, 54);
      lineBlock(nodes, ix + (iw + 8) / 2, iy + 30, (iw - 8) / 2, 54);
      lineBlock(nodes, ix, iy + 92, (iw - 8) / 2, 32, "#EEF4FF");
      lineBlock(nodes, ix + (iw + 8) / 2, iy + 92, (iw - 8) / 2, 32, "#EEF4FF");
      break;
    case "duplicate":
      lineBlock(nodes, ix, iy + 8, iw - 26, 66);
      lineBlock(nodes, ix + 24, iy + 28, iw - 26, 66, "#EEF4FF");
      lineBlock(nodes, ix + 120, iy + 106, 78, 18, accent);
      break;
    case "confirm-delete":
    case "publish-modal":
    case "param-modal":
      lineBlock(nodes, ix, iy, iw, ih, "#EDF2F7");
      lineBlock(nodes, ix + 24, iy + 18, iw - 48, 74, COLORS.modal);
      lineBlock(nodes, ix + 36, iy + 34, iw - 72, 18);
      lineBlock(nodes, ix + 36, iy + 62, 58, 18, COLORS.danger);
      lineBlock(nodes, ix + 102, iy + 62, 58, 18, COLORS.soft);
      break;
    case "export":
      lineBlock(nodes, ix, iy, iw, ih - 36);
      lineBlock(nodes, ix + 18, iy + 18, iw - 36, 62, "#F8FAFC");
      lineBlock(nodes, ix + 72, iy + ih - 28, 86, 18, accent);
      break;
    case "import-validate":
      lineBlock(nodes, ix, iy, iw, 30);
      lineBlock(nodes, ix, iy + 38, iw, 50, "#FFF7E8");
      lineBlock(nodes, ix, iy + 96, 94, 18, "#EAB308");
      break;
    case "publish-ready":
      lineBlock(nodes, ix, iy, iw, ih - 24);
      lineBlock(nodes, ix + iw - 94, iy + 10, 82, 18, accent);
      break;
    case "publish-success":
    case "import-success":
    case "save-success":
      lineBlock(nodes, ix, iy + 10, iw, 90, COLORS.success);
      lineBlock(nodes, ix + 56, iy + 112, 92, 18, accent);
      break;
    case "search-results":
      lineBlock(nodes, ix, iy, iw, 18);
      lineBlock(nodes, ix, iy + 26, iw, 26, "#EEF4FF");
      lineBlock(nodes, ix, iy + 62, iw, 20);
      lineBlock(nodes, ix, iy + 90, iw, 20);
      break;
    case "detail":
    case "stats-detail":
      lineBlock(nodes, ix, iy, 88, ih);
      lineBlock(nodes, ix + 98, iy, iw - 98, 28);
      lineBlock(nodes, ix + 98, iy + 36, iw - 98, 18);
      lineBlock(nodes, ix + 98, iy + 62, iw - 98, 18);
      lineBlock(nodes, ix + 98, iy + 92, 72, 18, accent);
      break;
    case "chat-launch":
    case "chat":
    case "chat-send":
    case "chat-switch":
      lineBlock(nodes, ix, iy, 72, ih);
      lineBlock(nodes, ix + 82, iy, iw - 82, ih - 34);
      lineBlock(nodes, ix + 92, iy + 10, iw - 112, 18, "#EEF4FF");
      lineBlock(nodes, ix + 112, iy + 38, iw - 140, 18, accent);
      lineBlock(nodes, ix + 92, iy + ih - 28, iw - 124, 16);
      lineBlock(nodes, ix + iw - 28, iy + ih - 28, 16, 16, accent, 6);
      break;
    case "favorite":
      lineBlock(nodes, ix, iy, iw, ih - 10);
      lineBlock(nodes, ix + iw - 34, iy + 10, 18, 18, "#F59E0B");
      break;
    case "version-history":
      lineBlock(nodes, ix, iy, 72, ih);
      lineBlock(nodes, ix + 82, iy, iw - 82, 20);
      lineBlock(nodes, ix + 82, iy + 30, iw - 82, 18);
      lineBlock(nodes, ix + 82, iy + 56, iw - 82, 18);
      lineBlock(nodes, ix + 82, iy + 82, 60, 18, accent);
      break;
    case "update-prompt":
    case "limit-warning":
      lineBlock(nodes, ix, iy + 8, iw, 46, COLORS.warn);
      lineBlock(nodes, ix, iy + 64, iw, 54);
      lineBlock(nodes, ix + iw - 72, iy + 122, 60, 18, accent);
      break;
    case "editor":
    case "json-form":
      lineBlock(nodes, ix, iy, 64, ih);
      lineBlock(nodes, ix + 74, iy, iw - 74, ih, "#0F172A");
      lineBlock(nodes, ix + 86, iy + 14, iw - 98, 10, "#60A5FA");
      lineBlock(nodes, ix + 86, iy + 34, iw - 122, 10, "#93C5FD");
      lineBlock(nodes, ix + 86, iy + 54, iw - 90, 10, "#BFDBFE");
      break;
    case "choice":
    case "selector":
      lineBlock(nodes, ix, iy, iw, 20);
      lineBlock(nodes, ix, iy + 30, (iw - 10) / 2, 60, "#EEF4FF");
      lineBlock(nodes, ix + (iw + 10) / 2, iy + 30, (iw - 10) / 2, 60, "#EEF4FF");
      break;
    case "url-form":
      lineBlock(nodes, ix, iy, iw, 18);
      lineBlock(nodes, ix, iy + 26, iw, 18);
      lineBlock(nodes, ix, iy + 52, 68, 18, "#EEF4FF");
      lineBlock(nodes, ix + 78, iy + 52, 68, 18, "#EEF4FF");
      lineBlock(nodes, ix, iy + 88, 80, 18, accent);
      break;
    case "toggle-list":
      lineBlock(nodes, ix, iy, iw, 22);
      lineBlock(nodes, ix, iy + 32, iw, 22);
      lineBlock(nodes, ix, iy + 64, iw, 22);
      lineBlock(nodes, ix + iw - 36, iy + 4, 24, 12, accent, 12);
      lineBlock(nodes, ix + iw - 36, iy + 36, 24, 12, "#CBD5E1", 12);
      break;
    case "error-retry":
    case "forbidden":
      lineBlock(nodes, ix, iy + 8, iw, 56, COLORS.danger);
      lineBlock(nodes, ix, iy + 74, iw, 34);
      lineBlock(nodes, ix + iw - 66, iy + 116, 54, 18, accent);
      break;
    case "streaming":
      lineBlock(nodes, ix, iy, 72, ih);
      lineBlock(nodes, ix + 82, iy, iw - 82, ih - 30);
      lineBlock(nodes, ix + 96, iy + 16, iw - 120, 14, "#DBEAFE");
      lineBlock(nodes, ix + 96, iy + 38, iw - 136, 14, accent);
      lineBlock(nodes, ix + 96, iy + 60, iw - 156, 14, "#DBEAFE");
      break;
    case "tool-status":
      lineBlock(nodes, ix, iy, iw, 24);
      lineBlock(nodes, ix, iy + 34, iw, 64, "#EEF7EE");
      lineBlock(nodes, ix + 12, iy + 46, iw - 24, 10, "#22C55E");
      lineBlock(nodes, ix + 12, iy + 66, iw - 42, 10, "#86EFAC");
      break;
    case "regen":
    case "copy":
      lineBlock(nodes, ix, iy, iw, 74);
      lineBlock(nodes, ix + 12, iy + 88, 64, 18, accent);
      lineBlock(nodes, ix + 84, iy + 88, 64, 18, COLORS.soft);
      break;
    case "upload-index":
      lineBlock(nodes, ix, iy, iw, 40, "#EEF4FF");
      lineBlock(nodes, ix, iy + 50, iw, 14);
      lineBlock(nodes, ix, iy + 72, iw - 24, 10, "#BFDBFE");
      lineBlock(nodes, ix, iy + 96, 84, 18, accent);
      break;
    case "doc-list":
      lineBlock(nodes, ix, iy, iw, 18);
      lineBlock(nodes, ix, iy + 28, iw, 18);
      lineBlock(nodes, ix, iy + 56, iw, 18);
      lineBlock(nodes, ix, iy + 84, 76, 18, accent);
      break;
    case "doc-delete":
      lineBlock(nodes, ix, iy, iw, 20);
      lineBlock(nodes, ix, iy + 30, iw, 18);
      lineBlock(nodes, ix, iy + 58, iw, 18, COLORS.danger);
      lineBlock(nodes, ix, iy + 94, 88, 18, accent);
      break;
    case "retrieve-test":
      lineBlock(nodes, ix, iy, iw, 18);
      lineBlock(nodes, ix, iy + 28, iw, 18);
      lineBlock(nodes, ix, iy + 56, iw, 42, "#EEF7EE");
      break;
    case "bind-kb":
      lineBlock(nodes, ix, iy, 82, ih);
      lineBlock(nodes, ix + 92, iy, iw - 92, 18);
      lineBlock(nodes, ix + 92, iy + 28, iw - 92, 18);
      lineBlock(nodes, ix + 92, iy + 56, 90, 18, accent);
      break;
    case "auth":
      lineBlock(nodes, ix + 34, iy, iw - 68, 110);
      lineBlock(nodes, ix + 48, iy + 18, iw - 96, 18);
      lineBlock(nodes, ix + 48, iy + 44, iw - 96, 18);
      lineBlock(nodes, ix + 68, iy + 78, iw - 136, 18, accent);
      break;
    case "session-load":
      lineBlock(nodes, ix, iy + 6, iw, 44, "#EEF7EE");
      lineBlock(nodes, ix, iy + 60, iw, 54);
      break;
    case "ownership":
      lineBlock(nodes, ix, iy, iw, 26);
      lineBlock(nodes, ix, iy + 36, iw, 54, "#EEF4FF");
      lineBlock(nodes, ix, iy + 98, 92, 18, accent);
      break;
    case "permission-form":
      lineBlock(nodes, ix, iy, iw, 18);
      lineBlock(nodes, ix, iy + 28, iw, 18, "#EEF4FF");
      lineBlock(nodes, ix, iy + 56, iw, 18, "#EEF4FF");
      lineBlock(nodes, ix, iy + 84, iw, 18, "#EEF4FF");
      break;
    case "admin-table":
      lineBlock(nodes, ix, iy, iw, 18);
      lineBlock(nodes, ix, iy + 28, iw, 18);
      lineBlock(nodes, ix, iy + 56, iw, 18);
      lineBlock(nodes, ix, iy + 84, iw, 18);
      break;
    case "nav":
    case "nav-switch":
      lineBlock(nodes, ix, iy, 56, ih);
      lineBlock(nodes, ix + 66, iy, iw - 66, 22);
      lineBlock(nodes, ix + 66, iy + 32, iw - 66, 72, "#EEF4FF");
      break;
    default:
      lineBlock(nodes, ix, iy, iw, ih);
      lineBlock(nodes, ix + 12, iy + 12, iw - 24, 18);
      lineBlock(nodes, ix + 12, iy + 40, iw - 36, 18);
      lineBlock(nodes, ix + 12, iy + 68, 88, 18, accent);
      break;
  }

  return nodes;
}

const nodes = [];

nodes.push({
  type: "frame",
  x: 0,
  y: 0,
  width: pencil.width,
  height: pencil.height,
  fill: COLORS.canvas,
  cornerRadius: 24,
});

nodes.push(textNode(PADDING, 24, 480, "Agent Platform Requirements Storyboard", 28, COLORS.ink, { fontFamily: FONT_HEADING, fontWeight: "700" }));
nodes.push(textNode(PADDING, 62, 860, "按需求分行、按验收标准分屏。每个缩略图对应 requirements.md 中的一条验收标准，用于信息架构、交互流与页面清单梳理。", 14, COLORS.sub, { lineHeight: 1.5 }));

let y = 108;
for (const req of REQUIREMENTS) {
  const rowWidth = LABEL_W + 24 + req.screens.length * SCREEN_W + (req.screens.length - 1) * SCREEN_GAP + 40;
  nodes.push(rectNode(PADDING, y, rowWidth, ROW_H, "#FFFFFFCC", COLORS.rowBorder, 22));
  nodes.push({
    type: "frame",
    x: PADDING,
    y,
    width: 10,
    height: ROW_H,
    fill: req.color,
    cornerRadius: [22, 0, 0, 22],
  });
  nodes.push(textNode(PADDING + 24, y + 20, LABEL_W - 36, req.code, 12, req.color, { fontFamily: FONT_MONO, fontWeight: "700", letterSpacing: 0.6 }));
  nodes.push(textNode(PADDING + 24, y + 42, LABEL_W - 36, req.title, 22, COLORS.ink, { fontFamily: FONT_HEADING, fontWeight: "700" }));
  nodes.push(textNode(PADDING + 24, y + 78, LABEL_W - 36, `${req.screens.length} 张图`, 13, COLORS.sub, { fontFamily: FONT_BODY }));

  let sx = PADDING + LABEL_W + 24;
  for (const [code, title, kind] of req.screens) {
    drawMiniScreen(sx, y + 20, code, title, kind, req.color).forEach((node) => nodes.push(node));
    sx += SCREEN_W + SCREEN_GAP;
  }
  y += ROW_H + ROW_GAP;
}

return nodes;
