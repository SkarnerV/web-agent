import { test, expect } from '@playwright/test';

test.describe('Chat Module', () => {
  test('chat page loads', async ({ page }) => {
    await page.goto('/chat');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('body')).toBeVisible();
  });

  test('API: create chat session', async ({ request }) => {
    // First get an agent to use
    const agentsResp = await request.get('http://localhost:3000/api/v1/agents?page=1&page_size=1');
    const agentsBody = await agentsResp.json();
    expect(agentsBody.success).toBe(true);

    const agents = agentsBody.data?.data || agentsBody.data || [];
    if (agents.length > 0) {
      const agentId = agents[0].id;
      const resp = await request.post('http://localhost:3000/api/v1/chat/sessions', {
        data: { agentId },
      });
      expect(resp.ok()).toBeTruthy();
      const body = await resp.json();
      expect(body.success).toBe(true);
    }
  });

  test('API: list chat sessions', async ({ request }) => {
    const resp = await request.get('http://localhost:3000/api/v1/chat/sessions?page=1&page_size=10');
    expect(resp.ok()).toBeTruthy();
    const body = await resp.json();
    expect(body.success).toBe(true);
  });

  test('API: delete chat session (full flow: create → delete → verify gone)', async ({ request }) => {
    // Create an agent first
    const agentResp = await request.post('http://localhost:3000/api/v1/agents', {
      headers: { 'Content-Type': 'application/json' },
      data: { name: 'DeleteTestAgent' + Date.now(), maxSteps: 5 },
    });
    if (!agentResp.ok()) {
      const errBody = await agentResp.text();
      console.log('Agent create failed:', agentResp.status(), errBody);
    }
    expect(agentResp.ok()).toBeTruthy();
    const agentBody = await agentResp.json();
    expect(agentBody.success).toBe(true);
    const agentId = agentBody.data.id;

    // Create session
    const createResp = await request.post('http://localhost:3000/api/v1/chat/sessions', {
      headers: { 'Content-Type': 'application/json' },
      data: { agentId },
    });
    expect(createResp.ok()).toBeTruthy();
    const sessionBody = await createResp.json();
    const sessionId = sessionBody.data.id;

    // Delete session
    const deleteResp = await request.delete(`http://localhost:3000/api/v1/chat/sessions/${sessionId}`);
    expect(deleteResp.status()).toBe(204);

    // Verify gone
    const getResp = await request.get(`http://localhost:3000/api/v1/chat/sessions/${sessionId}`);
    expect(getResp.status()).toBe(404);
  });
});
