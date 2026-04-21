import { test, expect } from '@playwright/test';

test.describe('User Management (Admin)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem('disableManual', 'true');
    });
    // Navigate to the app and login as admin
    await page.goto('/');
    
    // Fill in the login form
    // The DatabaseSeeder must have run so that admin/admin123 exists
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');

    // Wait for navigation to dashboard
    await expect(page).toHaveURL('http://localhost:5173/');
    await expect(page.getByText('Administrator Dashboard')).toBeVisible();

    // Navigate to user management
    await page.goto('/users');
    await expect(page).toHaveURL(/.*users/);
  });

  test('should display the user list', async ({ page }) => {
    // Ensure the users section is visible
    await expect(page.locator('h1')).toContainText('Benutzerverwaltung');
    
    // Check if the admin user is in the list
    await expect(page.getByRole('cell', { name: 'admin', exact: true })).toBeVisible();
  });

  test('should create a new user', async ({ page }) => {
    // Generate a unique username
    const uniqueUsername = `testuser_${Date.now()}`;

    // Click 'Neuen Benutzer anlegen' to show form
    await page.getByRole('button', { name: /Neuen Benutzer anlegen/i }).click();

    // Fill in the new user form
    await page.fill('input[name="username"]', uniqueUsername);
    await page.fill('input[name="password"]', 'testpass123');
    
    // Select role USER
    await page.selectOption('select[name="role"]', 'USER');

    // Click the add user button
    await page.getByRole('button', { name: /Benutzer erstellen/i }).click();

    // Wait for the new user to appear in the list
    await expect(page.getByText(uniqueUsername, { exact: true })).toBeVisible();
  });

  test('should delete a user', async ({ page }) => {
    // Generate a unique username
    const uniqueUsername = `delete_me_${Date.now()}`;

    // Create a user first to delete
    await page.getByRole('button', { name: /Neuen Benutzer anlegen/i }).click();
    await page.fill('input[name="username"]', uniqueUsername);
    await page.fill('input[name="password"]', 'testpass123');
    await page.selectOption('select[name="role"]', 'USER');
    await page.getByRole('button', { name: /Benutzer erstellen/i }).click();

    // Wait for the user to appear
    const userRow = page.locator('tr').filter({ hasText: uniqueUsername });
    await expect(userRow).toBeVisible();

    // Click the delete button in that row (.btn-delete class)
    await userRow.locator('.btn-delete').click();

    // Click 'Löschen' in the custom Modal
    await page.locator('.btn-confirm').click();

    // Wait for the user to disappear
    await expect(userRow).not.toBeVisible();
  });
});
