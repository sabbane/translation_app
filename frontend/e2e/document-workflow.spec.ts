import { test, expect } from '@playwright/test';

test.describe('Document Workflow', () => {
  test.beforeEach(async ({ page }) => {
    // Login as user-1
    await page.goto('/');
    await page.fill('#username', 'user-1');
    await page.fill('#password', 'user123');
    await page.click('button[type="submit"]');

    // Wait for navigation
    await expect(page).toHaveURL('http://localhost:5173/');
    await expect(page.getByText('Meine Dokumente')).toBeVisible();
  });

  test('should create, auto-translate and submit a document', async ({ page }) => {
    // Handle alerts
    page.on('dialog', dialog => dialog.accept());

    // 1. Create new document
    await page.getByRole('link', { name: /Neues Dokument/i }).click();
    await expect(page).toHaveURL(/.*editor/);

    // 2. Fill title and text
    const docTitle = `E2E Test Document ${Date.now()}`;
    await page.fill('.editor-title-input', docTitle);
    
    // Select languages (EN -> DE)
    await page.selectOption('select:nth-of-type(1)', 'EN');
    await page.selectOption('select:nth-of-type(2)', 'DE');

    // Fill original text
    await page.getByPlaceholder('Text zum Übersetzen eingeben...').fill('The quick brown fox jumps over the lazy dog.');

    // 3. Wait for auto-translation (debounce 800ms + network)
    await expect(async () => {
      const translated = await page.getByPlaceholder('Die Übersetzung erscheint hier...').inputValue();
      expect(translated.length).toBeGreaterThan(5);
    }).toPass({ timeout: 10000 });

    // 4. Save the document
    await page.getByRole('button', { name: /Speichern/i }).click();

    // 5. Submit for review
    await page.getByRole('button', { name: /Zur Review einreichen/i }).click();

    // Fill assignment modal
    // Wait for modal to be visible
    await expect(page.locator('.modal-overlay')).toBeVisible();
    
    // Select reviewer (e.g. reviewer-2 for EN->DE as per DatabaseSeeder logic)
    await page.selectOption('.reviewer-select', { label: 'reviewer-2' });
    
    // Click assign
    await page.getByRole('button', { name: /Zuweisen & Einreichen/i }).click();

    // 6. Verify back on dashboard and document is in list with status "In Prüfung"
    await expect(page).toHaveURL('http://localhost:5173/');
    const docRow = page.locator('tr').filter({ hasText: docTitle });
    await expect(docRow).toBeVisible();
    await expect(docRow.getByText('In Prüfung')).toBeVisible();
  });
});
