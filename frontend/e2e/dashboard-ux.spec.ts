import { test, expect } from '@playwright/test';

test.describe('Dashboard UX and Filters', () => {
  test.beforeEach(async ({ page }) => {
    // Login as user-1
    await page.goto('/');
    await page.fill('#username', 'user-1');
    await page.fill('#password', 'user123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('http://localhost:5173/');
  });

  test('should toggle between table and grid view', async ({ page }) => {
    // 1. Initial state is table
    await expect(page.locator('.data-table')).toBeVisible();
    await expect(page.locator('.document-grid')).not.toBeVisible();

    // 2. Switch to grid view
    await page.click('button[title="Kachelansicht"]');
    await expect(page.locator('.document-grid')).toBeVisible();
    await expect(page.locator('.data-table')).not.toBeVisible();

    // 3. Switch back to table view
    await page.click('button[title="Listenansicht"]');
    await expect(page.locator('.data-table')).toBeVisible();
    await expect(page.locator('.document-grid')).not.toBeVisible();
  });

  test('should filter documents by search query', async ({ page }) => {
    // Assuming user-1 has at least one document from DatabaseSeeder
    // We'll create one just to be sure
    const uniqueTitle = `Searchable Doc ${Date.now()}`;
    await page.goto('/editor');
    await page.fill('.editor-title-input', uniqueTitle);
    await page.getByPlaceholder('Text zum Übersetzen eingeben oder Datei importieren...').fill('Test search');
    await page.click('button:has-text("Speichern")');
    await page.goto('/');

    // 1. Search for the unique title
    await page.fill('.search-input', uniqueTitle);
    
    // 2. Only the matching row should be visible
    const rows = page.locator('table.data-table tbody tr');
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText(uniqueTitle);

    // 3. Clear search
    await page.fill('.search-input', '');
    await expect(rows.count()).resolves.toBeGreaterThan(5); // user-1 should see all their documents again
  });

  test('should filter documents by status', async ({ page }) => {
    // Select "In Prüfung" from status filter
    await page.selectOption('.filter-select:nth-of-type(1)', 'IN_PRUEFUNG');
    
    // Check that all visible rows have the "In Prüfung" badge
    const rows = page.locator('table.data-table tbody tr');
    const rowCount = await rows.count();
    
    for (let i = 0; i < rowCount; i++) {
      await expect(rows.nth(i).getByText('In Prüfung')).toBeVisible();
    }
  });
});
