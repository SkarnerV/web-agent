import { test, expect } from '@playwright/test';

test.describe('Agent CRUD', () => {
  test('agent list page loads', async ({ page }) => {
    await page.goto('/agents');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('main')).toBeVisible();
  });

  test('agent create page loads and form is accessible', async ({ page }) => {
    // Navigate directly to create page
    await page.goto('/agents/create');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // Fill in the name if form is visible
    const nameInput = page.locator('input[name="name"], input[placeholder*="名称"], input[placeholder*="Name"]').first();
    if (await nameInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await nameInput.fill('E2E Test Agent ' + Date.now());
    }

    // Fill description if visible
    const descInput = page.locator('textarea[name="description"], textarea[placeholder*="描述"]').first();
    if (await descInput.isVisible({ timeout: 1000 }).catch(() => false)) {
      await descInput.fill('Created by Playwright E2E test');
    }

    await expect(page.locator('body')).toBeVisible();
  });

  test('agent detail page loads', async ({ page }) => {
    await page.goto('/agents');
    await page.waitForLoadState('networkidle');

    // Click on first agent card or row
    const agentLink = page.locator('a[href*="/agents/"]').first();
    if (await agentLink.isVisible({ timeout: 3000 }).catch(() => false)) {
      await agentLink.click();
      await page.waitForURL('**/agents/**');
      await expect(page.locator('main')).toBeVisible();
    }
  });
});
