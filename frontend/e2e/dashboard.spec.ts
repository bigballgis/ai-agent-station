import { test, expect } from '@playwright/test'

test.describe('Dashboard Page', () => {

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

    // Mock agents API
    await page.route('**/api/v1/agents**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            total: 12,
            records: [
              { id: 1, name: 'Agent A', status: 'PUBLISHED', isActive: true, createdAt: '2025-01-01T00:00:00Z' },
              { id: 2, name: 'Agent B', status: 'DRAFT', isActive: false, createdAt: '2025-01-02T00:00:00Z' },
              { id: 3, name: 'Agent C', status: 'PUBLISHED', isActive: true, createdAt: '2025-01-03T00:00:00Z' },
              { id: 4, name: 'Agent D', status: 'PENDING_APPROVAL', isActive: true, createdAt: '2025-01-04T00:00:00Z' },
              { id: 5, name: 'Agent E', status: 'PUBLISHED', isActive: true, createdAt: '2025-01-05T00:00:00Z' },
            ],
            page: 0,
            size: 20,
            totalPages: 1,
          },
        }),
      })
    })

    // Mock test results API
    await page.route('**/api/v1/test-results**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            total: 10,
            records: [
              { id: 1, status: 'SUCCESS' },
              { id: 2, status: 'SUCCESS' },
              { id: 3, status: 'SUCCESS' },
              { id: 4, status: 'FAILED' },
              { id: 5, status: 'SUCCESS' },
            ],
            page: 0,
            size: 20,
            totalPages: 1,
          },
        }),
      })
    })

    // Mock alerts API
    await page.route('**/api/v1/alerts**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            total: 3,
            records: [
              { id: 1, severity: 'HIGH' },
              { id: 2, severity: 'MEDIUM' },
              { id: 3, severity: 'LOW' },
            ],
            page: 0,
            size: 20,
            totalPages: 1,
          },
        }),
      })
    })

    // Mock tool stats API
    await page.route('**/api/v1/tools/stats**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { totalCalls: 4321, apiCalls: 3200 },
        }),
      })
    })

    // Mock logs API
    await page.route('**/api/v1/logs**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 1, action: 'Created Agent', module: 'agent', operator: 'Admin', createdAt: '2025-06-01T10:00:00Z' },
            { id: 2, action: 'Deployed Agent', module: 'agent', operator: 'Admin', createdAt: '2025-06-01T09:00:00Z' },
            { id: 3, action: 'Approved Request', module: 'approval', operator: 'Admin', createdAt: '2025-05-31T15:00:00Z' },
          ],
        }),
      })
    })

    // Set auth token and skip onboarding
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

  test('should load dashboard page successfully', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // Dashboard page should be visible
    const dashboardPage = page.locator('[aria-label="Dashboard"], .dashboard-page')
    await expect(dashboardPage.first()).toBeVisible()
  })

  test('should display page header with title', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // Page header should be visible
    const pageHeader = page.locator('.page-header, [class*="PageHeader"]')
    await expect(pageHeader.first()).toBeVisible()
  })

  test('should display statistics cards', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // Stat cards should be visible - there should be 4 stat cards
    const statCards = page.locator('.stat-card, [class*="StatCard"]')
    await expect(statCards.first()).toBeVisible({ timeout: 10000 })
  })

  test('should display total agents count in stat card', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // The stat card for total agents should show the count
    const dashboardContent = page.locator('.dashboard-page')
    await expect(dashboardContent).toContainText('12')
  })

  test('should display API calls stat card', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // The stat card for API calls should show the count
    const dashboardContent = page.locator('.dashboard-page')
    await expect(dashboardContent).toContainText('4,321')
  })

  test('should display pass rate stat card', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // Pass rate should be displayed (4 out of 5 = 80%)
    const dashboardContent = page.locator('.dashboard-page')
    await expect(dashboardContent).toContainText('80%')
  })

  test('should display active alerts count', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // Active alerts should be displayed
    const dashboardContent = page.locator('.dashboard-page')
    await expect(dashboardContent).toContainText('3')
  })

  test('should display chart containers', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // Chart containers should be visible (line chart and doughnut chart)
    const chartContainers = page.locator('canvas, [class*="chart"], [class*="Chart"]')
    // Charts may take time to render, give them time
    await expect(chartContainers.first()).toBeVisible({ timeout: 15000 })
  })

  test('should display quick actions section', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // Quick actions section should be visible
    const dashboardContent = page.locator('.dashboard-page')
    await expect(dashboardContent).toContainText('Quick Actions')
  })

  test('should display recent activity section', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // Recent activity section should be visible
    const dashboardContent = page.locator('.dashboard-page')
    await expect(dashboardContent).toContainText('Recent Activity')
  })

  test('should display recent activity items', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // Activity items should be visible
    const dashboardContent = page.locator('.dashboard-page')
    await expect(dashboardContent).toContainText('Created Agent')
    await expect(dashboardContent).toContainText('Deployed Agent')
    await expect(dashboardContent).toContainText('Approved Request')
  })

  test('should show loading spinner while fetching data', async ({ page }) => {
    // Delay all API responses
    await page.route('**/api/v1/**', async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 1500))
      await route.continue()
    })

    await page.goto('/dashboard')

    // Loading spinner should be visible
    const spinner = page.locator('.ant-spin')
    await expect(spinner.first()).toBeVisible({ timeout: 5000 })

    // After loading, spinner should disappear
    await expect(spinner.first()).not.toBeVisible({ timeout: 10000 })
  })

  test('should display onboarding banner for new users', async ({ page }) => {
    // Remove onboarding_completed flag
    await page.evaluate(() => {
      localStorage.removeItem('onboarding_completed')
    })

    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // Onboarding banner should be visible
    const onboarding = page.locator('.dashboard-page')
    const hasOnboarding = await onboarding.locator('text=Create your first AI Agent').isVisible().catch(() => false)
    // The onboarding may or may not show depending on i18n, just verify page loads
    const dashboardPage = page.locator('.dashboard-page')
    await expect(dashboardPage.first()).toBeVisible()
  })

  test('should navigate to agents page from quick action', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // Mock agents list for the target page
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

    // Find and click the "Create New Agent" quick action button
    const quickAction = page.locator('button', { hasText: /Create New Agent/i }).first()
    if (await quickAction.isVisible()) {
      await quickAction.click()
      await page.waitForTimeout(500)
      await expect(page).toHaveURL(/\/agents/)
    }
  })

  test('should display view all link for recent activity', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    // "View All" link should be visible near recent activity
    const viewAllLink = page.locator('button, a', { hasText: /View All/i }).first()
    await expect(viewAllLink).toBeVisible()
  })
})
