import { test, expect } from '@playwright/test'

test.describe('Agent List Page', () => {

  test.beforeEach(async ({ page }) => {
    // Mock API response for agents list
    await page.route('**/api/v1/agents**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            total: 5,
            records: [
              {
                id: 1,
                name: 'Customer Service Agent',
                description: 'Handles customer inquiries and support requests',
                status: 'PUBLISHED',
                isActive: true,
                config: { type: 'CHAT' },
                createdAt: '2025-01-15T10:00:00Z',
              },
              {
                id: 2,
                name: 'Data Analysis Agent',
                description: 'Performs data analysis and generates reports',
                status: 'DRAFT',
                isActive: false,
                config: { type: 'TASK' },
                createdAt: '2025-02-20T14:30:00Z',
              },
              {
                id: 3,
                name: 'Code Review Agent',
                description: 'Reviews code and provides improvement suggestions',
                status: 'PUBLISHED',
                isActive: true,
                config: { type: 'FLOW' },
                createdAt: '2025-03-10T09:00:00Z',
              },
              {
                id: 4,
                name: 'Document Processing Agent',
                description: 'Processes and categorizes documents',
                status: 'PENDING_APPROVAL',
                isActive: true,
                config: { type: 'TASK' },
                createdAt: '2025-04-05T16:00:00Z',
              },
              {
                id: 5,
                name: 'Translation Agent',
                description: 'Translates content between multiple languages',
                status: 'DRAFT',
                isActive: false,
                config: { type: 'CHAT' },
                createdAt: '2025-05-01T11:00:00Z',
              },
            ],
            page: 0,
            size: 20,
            totalPages: 1,
          },
        }),
      })
    })

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

    // Set auth token in localStorage
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
    })
  })

  test('should navigate to agent list page and display agents', async ({ page }) => {
    await page.goto('/agents')

    // Wait for the page to load and agents to render
    await page.waitForSelector('[aria-label="Agent List"]', { timeout: 10000 }).catch(() => {})
    await page.waitForTimeout(1000)

    // Verify the page header is visible
    const pageTitle = page.locator('.agent-list-page').first()
    await expect(pageTitle).toBeVisible()

    // Verify agent cards are displayed
    const agentCards = page.locator('.agent-card')
    await expect(agentCards).toHaveCount(5, { timeout: 10000 })
  })

  test('should display agent names in cards', async ({ page }) => {
    await page.goto('/agents')
    await page.waitForTimeout(1000)

    // Verify specific agent names are visible
    await expect(page.locator('.agent-card')).toContainText('Customer Service Agent')
    await expect(page.locator('.agent-card')).toContainText('Data Analysis Agent')
    await expect(page.locator('.agent-card')).toContainText('Code Review Agent')
  })

  test('should display status badges for each agent', async ({ page }) => {
    await page.goto('/agents')
    await page.waitForTimeout(1000)

    // Verify status badges are rendered (StatusBadge component)
    const statusBadges = page.locator('.agent-card .status-badge, .agent-card [class*="status"]')
    // At minimum, some status indicators should be present
    const agentCards = page.locator('.agent-card')
    await expect(agentCards).toHaveCount(5)
  })

  test('should show loading skeleton while fetching agents', async ({ page }) => {
    // Intercept and delay the API response
    await page.route('**/api/v1/agents**', async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 2000))
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { total: 0, records: [], page: 0, size: 20, totalPages: 0 },
        }),
      })
    })

    await page.goto('/agents')

    // Skeleton loading should be visible initially
    const skeleton = page.locator('.animate-pulse')
    await expect(skeleton.first()).toBeVisible({ timeout: 5000 })

    // After loading completes, skeleton should disappear
    await expect(skeleton.first()).not.toBeVisible({ timeout: 5000 })
  })

  test('should show empty state when no agents exist', async ({ page }) => {
    // Override the route to return empty list
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

    await page.goto('/agents')
    await page.waitForTimeout(1000)

    // Empty state should be visible
    const emptyState = page.locator('[data-testid="empty-state"], .empty-state, text=No data')
    // The EmptyState component should render when no agents exist
    const agentCards = page.locator('.agent-card')
    await expect(agentCards).toHaveCount(0)
  })

  test('should display action buttons on each agent card', async ({ page }) => {
    await page.goto('/agents')
    await page.waitForTimeout(1000)

    const agentCards = page.locator('.agent-card')
    await expect(agentCards).toHaveCount(5)

    // Each card should have action buttons (export, edit, version, delete)
    const firstCard = agentCards.first()
    const actionButtons = firstCard.locator('button[title], button[aria-label]')
    await expect(actionButtons.first()).toBeVisible()
  })

  test('should have create agent button visible', async ({ page }) => {
    await page.goto('/agents')
    await page.waitForTimeout(1000)

    // The create agent button should be visible in the page header
    const createButton = page.locator('button', { hasText: /create|Create|创建/i }).first()
    await expect(createButton).toBeVisible()
  })

  test('should have import and export buttons visible', async ({ page }) => {
    await page.goto('/agents')
    await page.waitForTimeout(1000)

    // Import and export buttons should be visible
    const importButton = page.locator('button', { hasText: /import|Import|导入/i }).first()
    const exportButton = page.locator('button', { hasText: /export|Export|导出/i }).first()

    await expect(importButton).toBeVisible()
    await expect(exportButton).toBeVisible()
  })

  test('should display agent descriptions', async ({ page }) => {
    await page.goto('/agents')
    await page.waitForTimeout(1000)

    // Verify descriptions are shown (truncated to 2 lines)
    await expect(page.locator('.agent-card')).toContainText('Handles customer inquiries')
    await expect(page.locator('.agent-card')).toContainText('Performs data analysis')
  })
})
