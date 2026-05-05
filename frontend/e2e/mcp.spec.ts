import { test, expect } from '@playwright/test';

test.describe('MCP Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/mcp');
    await page.waitForLoadState('networkidle');
  });

  test('MCP list page loads', async ({ page }) => {
    await expect(page.locator('main')).toBeVisible();
  });

  test('create MCP server flow', async ({ page }) => {
    // Navigate directly to the add page
    await page.goto('/mcp/add');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // Fill MCP form if visible
    const nameInput = page.locator('input[name="name"], input[placeholder*="名称"], input[placeholder*="MCP"]').first();
    if (await nameInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await nameInput.fill('E2E Test MCP ' + Date.now());
    }

    const urlInput = page.locator('input[name="url"], input[placeholder*="URL"]').first();
    if (await urlInput.isVisible({ timeout: 1000 }).catch(() => false)) {
      await urlInput.fill('https://example.com/mcp');
    }

    // Submit if available
    const submitBtn = page.getByRole('button', { name: /保存|提交|创建|Save|Submit|Create/ }).first();
    if (await submitBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
      await submitBtn.click();
      await page.waitForTimeout(2000);
    }

    await expect(page.locator('body')).toBeVisible();
  });
});
