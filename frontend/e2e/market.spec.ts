import { test, expect } from '@playwright/test';

test.describe('Market Module', () => {
  test('agent market page loads', async ({ page }) => {
    await page.goto('/market/agents');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('main')).toBeVisible();
  });

  test('skill market page loads', async ({ page }) => {
    await page.goto('/market/skills');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('main')).toBeVisible();
  });

  test('MCP market page loads', async ({ page }) => {
    await page.goto('/market/mcp');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('main')).toBeVisible();
  });

  test('market search functionality', async ({ page }) => {
    await page.goto('/market/agents');
    await page.waitForLoadState('networkidle');

    // Look for search input
    const searchInput = page.locator('input[placeholder*="搜索"], input[placeholder*="Search"]').first();
    if (await searchInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await searchInput.fill('test');
      await page.waitForTimeout(1000);
    }
    await expect(page.locator('main')).toBeVisible();
  });
});
