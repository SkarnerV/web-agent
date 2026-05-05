import { test, expect } from '@playwright/test';

test.describe('Model Configuration', () => {
  test('model page loads via chat page model selector', async ({ page }) => {
    await page.goto('/chat');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('body')).toBeVisible();
  });

  test('API: builtin models are accessible', async ({ request }) => {
    const resp = await request.get('http://localhost:3000/api/v1/models/builtin');
    expect(resp.ok()).toBeTruthy();
    const body = await resp.json();
    expect(body.success).toBe(true);
    expect(body.data.length).toBeGreaterThanOrEqual(1);
  });

  test('API: all models return builtin + custom', async ({ request }) => {
    const resp = await request.get('http://localhost:3000/api/v1/models/all');
    expect(resp.ok()).toBeTruthy();
    const body = await resp.json();
    expect(body.success).toBe(true);
    expect(body.data.length).toBeGreaterThanOrEqual(1);
  });
});
