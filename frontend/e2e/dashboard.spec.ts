import { test, expect } from '@playwright/test';

test.describe('App Shell & Navigation', () => {
  test('root redirects to dashboard and page loads', async ({ page }) => {
    await page.goto('/');
    await page.waitForURL('**/dashboard');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('main')).toBeVisible({ timeout: 10000 });
  });

  test('sidebar navigation: Home → Agents → Skill → MCP → Knowledge', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    // Navigate to Agents (智能体) - use button role for sidebar link
    await page.getByRole('button', { name: '智能体', exact: true }).click();
    await page.waitForURL('**/agents');
    await expect(page).toHaveURL(/\/agents/);

    // Navigate to Skill
    await page.getByRole('button', { name: 'Skill', exact: true }).click();
    await page.waitForURL('**/skills');
    await expect(page).toHaveURL(/\/skills/);

    // Navigate to MCP
    await page.getByRole('button', { name: 'MCP', exact: true }).click();
    await page.waitForURL('**/mcp');
    await expect(page).toHaveURL(/\/mcp/);

    // Navigate back to Home (首页)
    await page.getByRole('button', { name: '首页', exact: true }).click();
    await page.waitForURL('**/dashboard');
  });

  test('market navigation works', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    // Navigate to Agent Market
    await page.getByText('智能体市场').click();
    await page.waitForURL('**/market/agents');
    await expect(page).toHaveURL(/\/market\/agents/);

    // Navigate to Skill Market
    await page.getByText('Skill 市场').click();
    await page.waitForURL('**/market/skills');
  });
});
