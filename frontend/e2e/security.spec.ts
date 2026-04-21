import { test, expect } from '@playwright/test';

test.describe('Security and RBAC', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem('disableManual', 'true');
    });
  });

  test('regular user should not access user management page', async ({ page }) => {
    // 1. Login as a normal user
    await page.goto('/');
    await page.fill('#username', 'user-1');
    await page.fill('#password', 'user123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('http://localhost:5173/');

    // 2. Try to navigate to /users directly
    await page.goto('/users');

    // 3. Should be redirected or blocked
    // The current implementation might redirect to dashboard or show unauthorized
    // Based on the PrivateRoute/Role-based logic, it should redirect to home or show a message
    await expect(page).toHaveURL('http://localhost:5173/');
    await expect(page.getByText('Administrator Dashboard')).not.toBeVisible();
  });

  test('admin should see user management link', async ({ page }) => {
    // 1. Login as admin
    await page.goto('/');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    
    // 2. Dashboard should show Admin header
    await expect(page.getByText('Administrator Dashboard')).toBeVisible();

    // 3. User management link in navigation should be visible
    // (Assuming there is a navigation link)
    const usersLink = page.getByRole('link', { name: /Benutzer/i });
    await expect(usersLink).toBeVisible();
  });

  test('unauthenticated user should be redirected to login', async ({ page }) => {
    // 1. Clear storage/cookies implicitly by starting fresh or navigating to a protected route
    await page.goto('/editor');

    // 2. Should be on login page
    await expect(page).toHaveURL(/.*login/);
    await expect(page.locator('#username')).toBeVisible();
  });
});
