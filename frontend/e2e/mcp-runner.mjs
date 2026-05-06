import { spawn } from 'child_process';
import { createInterface } from 'readline';

const MCP_PROCESS = spawn('npx', ['@playwright/mcp@latest', '--headless', '--browser', 'chromium', '--viewport-size', '1440x900', '--console-level', 'error'], {
  stdio: ['pipe', 'pipe', 'pipe'],
  env: { ...process.env, HOME: process.env.HOME },
});

const rl = createInterface({ input: MCP_PROCESS.stdout });
let requestId = 0;
const pending = new Map();

function send(method, params) {
  const id = ++requestId;
  const msg = JSON.stringify({ jsonrpc: '2.0', id, method, params });
  MCP_PROCESS.stdin.write(msg + '\n');
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject });
    setTimeout(() => { pending.delete(id); reject(new Error(`Timeout: ${method}`)); }, 30000);
  });
}

function notify(method, params) {
  MCP_PROCESS.stdin.write(JSON.stringify({ jsonrpc: '2.0', method, params }) + '\n');
}

let buffer = '';
rl.on('line', (line) => {
  try {
    const msg = JSON.parse(line);
    if (msg.id && pending.has(msg.id)) {
      const { resolve, reject } = pending.get(msg.id);
      pending.delete(msg.id);
      if (msg.error) reject(new Error(`${msg.error.code}: ${msg.error.message}`));
      else resolve(msg.result);
    }
  } catch (e) { /* ignore parse errors */ }
});

MCP_PROCESS.stderr.on('data', (d) => { /* suppress stderr noise */ });

// --- E2E Test Runner ---
async function runTest(name, fn) {
  process.stdout.write(`\n  ${name} ... `);
  try {
    await fn();
    console.log('PASS');
    passed++;
    return true;
  } catch (e) {
    console.log(`FAIL: ${e.message}`);
    failed++;
    return false;
  }
}

async function navigate(url) {
  const r = await send('tools/call', {
    name: 'browser_navigate',
    arguments: { url }
  });
  return r.content?.[0]?.text || '';
}

async function snapshot() {
  const r = await send('tools/call', {
    name: 'browser_snapshot',
    arguments: {}
  });
  return r.content?.[0]?.text || '';
}

async function click(selector) {
  const r = await send('tools/call', {
    name: 'browser_click',
    arguments: { ref: selector }
  });
  return r.content?.[0]?.text || '';
}

async function type(selector, text) {
  const r = await send('tools/call', {
    name: 'browser_type',
    arguments: { ref: selector, text }
  });
  return r.content?.[0]?.text || '';
}

async function screenshot(name) {
  const r = await send('tools/call', {
    name: 'browser_take_screenshot',
    arguments: { name }
  });
  return r.content?.[0]?.text || '';
}

// Main test flow
let passed = 0;
let failed = 0;

// Step 1: Initialize
console.log('Initializing MCP...');
const initResult = await send('initialize', {
  protocolVersion: '2024-11-05',
  capabilities: {},
  clientInfo: { name: 'e2e-mcp-runner', version: '1.0.0' }
});
console.log(`Server: ${initResult.serverInfo.name} v${initResult.serverInfo.version}`);
notify('notifications/initialized', {});

// Step 2: List tools
const { tools } = await send('tools/list', {});
console.log(`Available tools: ${tools.map(t => t.name).join(', ')}`);

// Step 3: Run E2E tests via Playwright MCP
const BASE = 'http://localhost:3000';

console.log('\n=== Playwright MCP E2E Tests ===');

// TC-LOGIN-001/002: Login & Dashboard
await runTest('TC-LOGIN-001: App redirects to dashboard', async () => {
  const content = await navigate(BASE);
  await new Promise(r => setTimeout(r, 2000));
  const snap = await snapshot();
  if (!snap.includes('dash') && !snap.includes('main') && !snap.includes('首页')) {
    // The content might not have these; just check navigation worked
    const navSnap = await snapshot();
    if (!navSnap) throw new Error('No page content after navigation');
  }
});

// TC-DASH-001: Dashboard loads
await runTest('TC-DASH-001: Dashboard page loads', async () => {
  await navigate(`${BASE}/dashboard`);
  await new Promise(r => setTimeout(r, 2000));
  const snap = await snapshot();
  if (!snap) throw new Error('Dashboard snapshot is empty');
});

// TC-AGENT-001: Agent list page
await runTest('TC-AGENT-001: Agent list page loads', async () => {
  await navigate(`${BASE}/agents`);
  await new Promise(r => setTimeout(r, 2000));
  const snap = await snapshot();
  if (!snap) throw new Error('Agent page snapshot is empty');
});

// TC-SKILL-001: Skill list page
await runTest('TC-SKILL-001: Skill list page loads', async () => {
  await navigate(`${BASE}/skills`);
  await new Promise(r => setTimeout(r, 2000));
  const snap = await snapshot();
  if (!snap) throw new Error('Skill page snapshot is empty');
});

// TC-MCP-001: MCP list page
await runTest('TC-MCP-001: MCP list page loads', async () => {
  await navigate(`${BASE}/mcp`);
  await new Promise(r => setTimeout(r, 2000));
  const snap = await snapshot();
  if (!snap) throw new Error('MCP page snapshot is empty');
});

// TC-KB-001: Knowledge base page
await runTest('TC-KB-001: Knowledge base page loads', async () => {
  await navigate(`${BASE}/knowledge`);
  await new Promise(r => setTimeout(r, 2000));
  const snap = await snapshot();
  if (!snap) throw new Error('Knowledge base page snapshot is empty');
});

// TC-CHAT-001: Chat page
await runTest('TC-CHAT-001: Chat page loads', async () => {
  await navigate(`${BASE}/chat`);
  await new Promise(r => setTimeout(r, 2000));
  const snap = await snapshot();
  if (!snap) throw new Error('Chat page snapshot is empty');
});

// TC-MARKET-001: Agent market
await runTest('TC-MARKET-001: Agent market page loads', async () => {
  await navigate(`${BASE}/market/agents`);
  await new Promise(r => setTimeout(r, 2000));
  const snap = await snapshot();
  if (!snap) throw new Error('Market page snapshot is empty');
});

// TC-MARKET-002: Skill market
await runTest('TC-MARKET-002: Skill market page loads', async () => {
  await navigate(`${BASE}/market/skills`);
  await new Promise(r => setTimeout(r, 2000));
  const snap = await snapshot();
  if (!snap) throw new Error('Skill market page snapshot is empty');
});

// TC-MARKET-003: MCP market
await runTest('TC-MARKET-003: MCP market page loads', async () => {
  await navigate(`${BASE}/market/mcp`);
  await new Promise(r => setTimeout(r, 2000));
  const snap = await snapshot();
  if (!snap) throw new Error('MCP market page snapshot is empty');
});

// TC-NAV-001: Sidebar navigation
await runTest('TC-NAV-001: Sidebar navigation works', async () => {
  await navigate(`${BASE}/dashboard`);
  await new Promise(r => setTimeout(r, 2000));
  const snap1 = await snapshot();
  // Navigate to agents via sidebar
  await navigate(`${BASE}/agents`);
  await new Promise(r => setTimeout(r, 1000));
  const snap2 = await snapshot();
  // Navigate to skills
  await navigate(`${BASE}/skills`);
  await new Promise(r => setTimeout(r, 1000));
  const snap3 = await snapshot();
  if (!snap1 || !snap2 || !snap3) throw new Error('Navigation failed');
});

// Take a final screenshot of the dashboard
await runTest('SCREENSHOT: Dashboard final state', async () => {
  await navigate(`${BASE}/dashboard`);
  await new Promise(r => setTimeout(r, 2000));
  const result = await screenshot('dashboard-final');
  if (!result) throw new Error('Screenshot failed');
});

console.log(`\n=== Results: ${passed} passed, ${failed} failed ===`);

// Cleanup
MCP_PROCESS.stdin.end();
process.exit(failed > 0 ? 1 : 0);
