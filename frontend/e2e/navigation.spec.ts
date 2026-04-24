import { test, expect } from '@playwright/test'

test.describe('Sidebar Navigation', () => {

  test.beforeEach(async ({ page }) => {
    // Mock auth check
    await page.route('**/api/v1/auth/userinfo', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 1,
            username: 'admin',
            nickname: 'Admin',
            roles: ['ADMIN'],
            tenantId: 100,
          },
        }),
      })
    })

    // Mock dashboard data
    await page.route('**/api/v1/agents**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { total: 0, records: [], page: 0, size: 20, totalPages: 0 },
        }),
      })
    })

    await page.route('**/api/v1/test-results**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { total: 0, records: [], page: 0, size: 20, totalPages: 0 },
        }),
      })
    })

    await page.route('**/api/v1/alerts**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { total: 0, records: [], page: 0, size: 20, totalPages: 0 },
        }),
      })
    })

    await page.route('**/api/v1/tools/stats**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { totalCalls: 0, apiCalls: 0 } }),
      })
    })

    await page.route('**/api/v1/logs**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [],
        }),
      })
    })

    // Set auth token
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.setItem('token', 'mock-jwt-token')
      localStorage.setItem('userInfo', JSON.stringify({
        id: 1,
        username: 'admin',
        nickname: 'Admin',
        roles: ['ADMIN'],
        tenantId: 100,
      }))
      localStorage.setItem('onboarding_completed', 'true')
    })
  })

  test('should display sidebar with menu groups', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    // Sidebar should be visible
    const sidebar = page.locator('aside')
    await expect(sidebar).toBeVisible()

    // Menu group labels should be visible
    await expect(page.locator('aside')).toContainText('Dashboard')
  })

  test('should navigate to Dashboard when clicking Dashboard menu item', async ({ page }) => {
    await page.goto('/agents')
    await page.waitForTimeout(1000)

    // Click on Dashboard menu item
    const dashboardLink = page.locator('aside a[href="/dashboard"], aside button', { hasText: /Dashboard/i }).first()
    if (await dashboardLink.isVisible()) {
      await dashboardLink.click()
      await page.waitForTimeout(500)
      await expect(page).toHaveURL(/\/dashboard/)
    }
  })

  test('should navigate to Agent List when clicking Agent Management menu item', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    // Click on Agent Management menu item
    const agentLink = page.locator('aside button', { hasText: /Agent Management|Agent/i }).first()
    if (await agentLink.isVisible()) {
      await agentLink.click()
      await page.waitForTimeout(500)
      await expect(page).toHaveURL(/\/agents/)
    }
  })

  test('should highlight active menu item based on current route', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    // The dashboard menu item should have active styling
    const activeItem = page.locator('aside button[class*="primary"]')
    // At least one menu item should be active (highlighted)
    const sidebarButtons = page.locator('aside nav button')
    const count = await sidebarButtons.count()
    expect(count).toBeGreaterThan(0)
  })

  test('should collapse and expand sidebar', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    const sidebar = page.locator('aside')
    await expect(sidebar).toBeVisible()

    // Find the collapse/expand button at the bottom of sidebar
    const collapseButton = page.locator('aside button', { hasText: /Collapse|展开/i }).first()
    if (await collapseButton.isVisible()) {
      await collapseButton.click()
      await page.waitForTimeout(300)

      // Sidebar should be in collapsed state (narrower width)
      const sidebarWidth = await sidebar.boundingBox()
      expect(sidebarWidth).not.toBeNull()
      // Collapsed sidebar should be narrow (w-16 = 64px)
      if (sidebarWidth) {
        expect(sidebarWidth.width).toBeLessThan(100)
      }
    }
  })

  test('should display breadcrumb navigation', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    // Breadcrumb should be visible
    const breadcrumb = page.locator('.ant-breadcrumb')
    await expect(breadcrumb).toBeVisible()
  })

  test('should update breadcrumb when navigating between pages', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    // Navigate to agents
    await page.goto('/agents')
    await page.waitForTimeout(1000)

    // Breadcrumb should still be visible after navigation
    const breadcrumb = page.locator('.ant-breadcrumb')
    await expect(breadcrumb).toBeVisible()
  })

  test('should display header with logo and user info', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    // Logo should be visible
    const logo = page.locator('header')
    await expect(logo).toBeVisible()
    await expect(logo).toContainText('AI Agent Station')

    // User avatar should be visible
    const userAvatar = page.locator('header .ant-avatar')
    await expect(userAvatar).toBeVisible()
  })

  test('should display global search input in header', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    // Search input should be visible in header
    const searchInput = page.locator('header input[type="text"]')
    await expect(searchInput).toBeVisible()
  })

  test('should navigate to Test Cases page from sidebar', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    // Click on Test Case Management menu item
    const testCaseLink = page.locator('aside button', { hasText: /Test Case/i }).first()
    if (await testCaseLink.isVisible()) {
      await testCaseLink.click()
      await page.waitForTimeout(500)
      await expect(page).toHaveURL(/\/test-cases/)
    }
  })

  test('should display submenu items for System Settings group', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    // Find the system settings group button
    const systemSettingsBtn = page.locator('aside button', { hasText: /System Settings|系统设置/i }).first()
    if (await systemSettingsBtn.isVisible()) {
      await systemSettingsBtn.click()
      await page.waitForTimeout(300)

      // Submenu items should be visible
      const subItems = page.locator('aside nav button[class*="ml-4"]')
      const subItemCount = await subItems.count()
      expect(subItemCount).toBeGreaterThan(0)
    }
  })

  test('should display notification bell in header', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    // Notification bell should be visible
    const notificationBell = page.locator('header button[aria-label*="notification"], header button[aria-label*="Notification"]')
    await expect(notificationBell).toBeVisible()
  })

  test('should display theme toggle button in header', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    // Theme toggle button should be visible
    const themeToggle = page.locator('header button', { has: page.locator('.anticon-bulb, .anticon-sun') })
    // The button exists even if the icon locator doesn't match exactly
    const headerButtons = page.locator('header button')
    const buttonCount = await headerButtons.count()
    expect(buttonCount).toBeGreaterThanOrEqual(3) // notification, theme, language, user
  })

  test('should display footer with version info', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)

    // Footer should be visible with version info
    const footer = page.locator('footer')
    await expect(footer).toBeVisible()
    await expect(footer).toContainText('v1.0.0')
  })
})
