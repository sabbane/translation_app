import { test, expect } from '@playwright/test';

test.describe('Full Cycle Translation Workflow', () => {
  const docTitle = `Full-Cycle-Test-${Date.now()}`;

  test('should hand off document from User to Reviewer and complete it', async ({ browser }) => {
    // 1. USER: Create and Submit
    const userContext = await browser.newContext();
    const userPage = await userContext.newPage();
    
    await userPage.goto('http://localhost:5173/');
    await userPage.fill('#username', 'user-1');
    await userPage.fill('#password', 'user123');
    await userPage.click('button[type="submit"]');

    // Handle alerts
    userPage.on('dialog', dialog => dialog.accept());

    await userPage.getByRole('link', { name: /Neues Dokument/i }).click();
    await userPage.fill('.editor-title-input', docTitle);
    await userPage.fill('textarea:nth-of-type(1)', 'Hello, this needs review.');
    
    // Wait for auto-translation just to be sure it doesn't interfere with save
    await userPage.waitForTimeout(1000);
    
    await userPage.getByRole('button', { name: /Speichern/i }).click();
    await userPage.getByRole('button', { name: /Zur Review einreichen/i }).click();
    
    await userPage.selectOption('.reviewer-select', { label: 'reviewer-1' });
    await userPage.getByRole('button', { name: /Zuweisen & Einreichen/i }).click();
    
    await expect(userPage).toHaveURL('http://localhost:5173/');
    await userContext.close();

    // 2. REVIEWER: Review and Complete
    const reviewerContext = await browser.newContext();
    const reviewerPage = await reviewerContext.newPage();
    
    await reviewerPage.goto('http://localhost:5173/');
    await reviewerPage.fill('#username', 'reviewer-1');
    await reviewerPage.fill('#password', 'reviewer123');
    await reviewerPage.click('button[type="submit"]');

    // Handle alerts
    reviewerPage.on('dialog', dialog => dialog.accept());

    await expect(reviewerPage.getByText('Reviewer Dashboard')).toBeVisible();
    
    const docCard = reviewerPage.locator('.doc-card').filter({ hasText: docTitle });
    await expect(docCard).toBeVisible();
    await expect(docCard.getByText('In Prüfung')).toBeVisible();
    
    // Open document (click the card)
    await docCard.click();
    
    // Click 'Bestätigen & Abschließen'
    await reviewerPage.getByRole('button', { name: /Bestätigen & Abschließen/i }).click();

    // Verify back on dashboard and status is "Fertig"
    await expect(reviewerPage).toHaveURL('http://localhost:5173/');
    const completedDocCard = reviewerPage.locator('.doc-card').filter({ hasText: docTitle });
    await expect(completedDocCard.getByText('Fertig')).toBeVisible();
    
    await reviewerContext.close();
  });
});
