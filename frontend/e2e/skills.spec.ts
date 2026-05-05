import { test, expect } from '@playwright/test';

test.describe('Skill Management', () => {
  test('skill list page loads', async ({ page }) => {
    await page.goto('/skills');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('main')).toBeVisible();
  });

  test('skill create page loads', async ({ page }) => {
    await page.goto('/skills/create');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('API: skills list returns successfully', async ({ request }) => {
    const resp = await request.get('http://localhost:3000/api/v1/skills?page=1&page_size=5');
    expect(resp.ok()).toBeTruthy();
    const body = await resp.json();
    expect(body.success).toBe(true);
  });
});
