import { test, expect } from '@playwright/test';

// GET /api/items is stubbed with page.route, so these checks do not depend on the backend or on real failures.
// Requirements: REQ-032 and NFR-007 (readable messages only, no stack trace, Retry where it helps).

const STACK_TRACE_BODY = {
  message: 'java.lang.NullPointerException at com.smartpantry.item.ItemQueryService.findAllWithStatus(ItemQueryService.java:42)',
  stackTrace: 'java.lang.NullPointerException\n\tat com.smartpantry.item.ItemQueryService.findAllWithStatus(ItemQueryService.java:42)',
  correlationId: 'err-test-0001',
};

test('a 500 response shows a readable message and never shows a stack trace', async ({ page }) => {
  await page.route('**/api/items', (route) =>
    route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify(STACK_TRACE_BODY),
    }),
  );

  await page.goto('/');

  const alert = page.getByRole('alert');
  await expect(alert.getByText('Something went wrong on the server. Try again.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Retry', exact: true })).toBeVisible();

  const screenText = await page.locator('main').innerText();
  expect(screenText).not.toContain('Exception');
  expect(screenText).not.toContain('com.smartpantry');
  expect(screenText).not.toContain('ItemQueryService');
  expect(await page.content()).not.toContain('NullPointerException');
});

test('an unreachable server shows a readable message, and Retry loads the dashboard once it is back', async ({ page }) => {
  // Every request fails until the test marks the server reachable. This stays correct under React StrictMode,
  // which can send the first request twice in development.
  let reachable = false;
  await page.route('**/api/items', (route) => {
    if (reachable) {
      return route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    }
    return route.abort('connectionrefused');
  });

  await page.goto('/');

  const alert = page.getByRole('alert');
  await expect(alert.getByText('Cannot reach the server. Try again.')).toBeVisible();
  const retry = page.getByRole('button', { name: 'Retry', exact: true });
  await expect(retry).toBeVisible();

  reachable = true;
  await retry.click();

  await expect(page.getByRole('alert')).toHaveCount(0);
  await expect(page.locator('.stat-card', { has: page.getByText('Grocery items', { exact: true }) })
    .locator('.stat-value')).toHaveText('0');
});

test('a 404 response on the inventory screen shows the readable message from the API', async ({ page }) => {
  await page.route('**/api/items', (route) =>
    route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'The requested item could not be found.', correlationId: 'err-test-0404' }),
    }),
  );

  await page.goto('/');
  await page.getByRole('button', { name: 'Pantry inventory' }).click();

  await expect(page.getByRole('alert').getByText('The requested item could not be found.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Retry', exact: true })).toBeVisible();
  const screenText = await page.locator('main').innerText();
  expect(screenText).not.toContain('Exception');
  expect(screenText).not.toContain('com.smartpantry');
});
