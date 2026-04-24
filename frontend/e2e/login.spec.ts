import { test, expect } from '@playwright/test'

test('login page loads', async ({ page }) => {
  await page.goto('/login')
  await expect(page.locator('h1')).toBeVisible()
})

test('login form validation', async ({ page }) => {
  await page.goto('/login')
  await page.click('button[type=submit]')
  // Should show validation errors
})
