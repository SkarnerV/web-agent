import { test, expect } from '@playwright/test';

test.describe('Knowledge Base', () => {
  test('knowledge base page loads', async ({ page }) => {
    await page.goto('/knowledge');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('main')).toBeVisible();
  });

  test('create knowledge base flow', async ({ page }) => {
    await page.goto('/knowledge');
    await page.waitForLoadState('networkidle');

    const addBtn = page.getByRole('button', { name: /创建|新建|添加|Create|Add/ }).first();
    if (await addBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await addBtn.click();
      await page.waitForTimeout(1000);

      const nameInput = page.locator('input[name="name"], input[placeholder*="名称"]').first();
      if (await nameInput.isVisible()) {
        await nameInput.fill('E2E Test KB ' + Date.now());
      }

      const submitBtn = page.getByRole('button', { name: /保存|创建|提交|Save|Create/ }).first();
      if (await submitBtn.isVisible()) {
        await submitBtn.click();
        await page.waitForTimeout(2000);
      }
    }
  });
});
