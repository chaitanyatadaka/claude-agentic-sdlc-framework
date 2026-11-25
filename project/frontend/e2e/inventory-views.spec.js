import { test, expect } from '@playwright/test';

// Checks for the inventory and dashboard views: optional purchase date (REQ-001, Q-001),
// "No expiry date" with the Valid indicator (REQ-012, REQ-013, Q-006), the item detail view (REQ-003),
// and the dashboard re-reading inventory on each view (REQ-019, Q-018).

function rowFor(page, text) {
  return page.getByRole('row').filter({ hasText: text });
}

async function openInventory(page) {
  await page.getByRole('button', { name: 'Pantry inventory' }).click();
}

function isoOffset(days) {
  const d = new Date();
  d.setDate(d.getDate() + days);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

async function createViaApi(request, data) {
  const response = await request.post('/api/items', { data });
  expect(response.status()).toBe(201);
  return response.json();
}

async function deleteViaApi(request, id) {
  await request.delete(`/api/items/${id}`);
}

async function dashboardGroceryCount(page) {
  await page.getByRole('button', { name: 'Dashboard', exact: true }).click();
  const value = page
    .locator('.stat-card', { has: page.getByText('Grocery items', { exact: true }) })
    .locator('.stat-value');
  await expect(value).toHaveText(/^\d+$/);
  return { value, count: Number(await value.innerText()) };
}

test('an item can be saved with no purchase date (REQ-001, Q-001)', async ({ page }) => {
  const name = `E2E NoPurchase ${Date.now()}`;
  await page.goto('/');
  await openInventory(page);
  await page.getByRole('button', { name: 'Add item' }).click();

  const dialog = page.getByRole('dialog', { name: 'Add item' });
  await dialog.getByLabel('Name', { exact: true }).fill(name);
  await dialog.getByLabel('Quantity', { exact: true }).fill('1');
  await dialog.getByLabel('Category', { exact: true }).selectOption('Grains');
  await dialog.getByLabel('Purchase date', { exact: true }).fill('');
  await dialog.getByRole('button', { name: 'Save item' }).click();

  await expect(dialog).toBeHidden();
  const row = rowFor(page, name);
  await expect(row).toBeVisible();
  await expect(row.getByText('No expiry date', { exact: true })).toBeVisible();

  const saved = (await (await page.request.get('/api/items')).json()).find((i) => i.name === name);
  expect(saved.purchaseDate).toBeNull();
  await deleteViaApi(page.request, saved.id);
});

test('an item with no expiry date shows "No expiry date" and the Valid indicator (REQ-012, REQ-013)', async ({ page }) => {
  const name = `E2E NoExpiry ${Date.now()}`;
  await page.goto('/');
  await openInventory(page);
  await page.getByRole('button', { name: 'Add item' }).click();

  const dialog = page.getByRole('dialog', { name: 'Add item' });
  await dialog.getByLabel('Name', { exact: true }).fill(name);
  await dialog.getByLabel('Quantity', { exact: true }).fill('3');
  await dialog.getByLabel('Category', { exact: true }).selectOption('Snacks');
  await dialog.getByRole('button', { name: 'Save item' }).click();
  await expect(dialog).toBeHidden();

  const row = rowFor(page, name);
  await expect(row.getByRole('cell', { name: 'No expiry date', exact: true })).toBeVisible();
  await expect(row.locator('.badge')).toHaveText('Valid');
  await expect(row.locator('.badge')).toHaveClass(/badge-valid/);

  const saved = (await (await page.request.get('/api/items')).json()).find((i) => i.name === name);
  await deleteViaApi(page.request, saved.id);
});

test('selecting an item name or View opens a detail view with every stored field (REQ-003)', async ({ page }) => {
  const name = `E2E Detail ${Date.now()}`;
  const expiry = isoOffset(10);
  const purchase = isoOffset(-1);
  const created = await createViaApi(page.request, {
    name,
    quantity: 4,
    category: 'Dairy',
    purchaseDate: purchase,
    expiryDate: expiry,
  });

  try {
    await page.goto('/');
    await openInventory(page);

    // Selecting the name opens the detail view.
    await page.getByRole('button', { name, exact: true }).click();
    const detail = page.locator('.detail-list');
    await expect(detail).toBeVisible();
    await expect(detail.locator('dd').nth(0)).toHaveText(name);
    await expect(detail.locator('dd').nth(1)).toHaveText('4');
    await expect(detail.locator('dd').nth(2)).toHaveText('Dairy');
    await expect(detail.locator('dd').nth(3)).toHaveText(purchase);
    await expect(detail.locator('dd').nth(4)).toHaveText(expiry);
    await expect(detail.locator('dd').nth(5).locator('.badge')).toHaveText('Valid');

    // Back returns to the list, and the View action opens the same detail view.
    await page.getByRole('button', { name: 'Back to inventory' }).click();
    await expect(rowFor(page, name)).toBeVisible();
    await page.getByRole('button', { name: `View ${name}`, exact: true }).click();
    await expect(page.locator('.detail-list')).toBeVisible();
    await expect(page.locator('.detail-list dd').nth(4)).toHaveText(expiry);
  } finally {
    await deleteViaApi(page.request, created.id);
  }
});

test('the detail view shows "No expiry date" for an item without one (REQ-003, REQ-013)', async ({ page }) => {
  const name = `E2E DetailNoExpiry ${Date.now()}`;
  const created = await createViaApi(page.request, {
    name,
    quantity: 2,
    category: 'Fruits',
    purchaseDate: isoOffset(-2),
  });

  try {
    await page.goto('/');
    await openInventory(page);
    await page.getByRole('button', { name, exact: true }).click();

    const detail = page.locator('.detail-list');
    await expect(detail.locator('dd').nth(4)).toHaveText('No expiry date');
    await expect(detail.locator('dd').nth(5).locator('.badge')).toHaveText('Valid');
  } finally {
    await deleteViaApi(page.request, created.id);
  }
});

test('the dashboard shows inventory changed outside the page on its next view (REQ-019, Q-018)', async ({ page }) => {
  const name = `E2E Dashboard ${Date.now()}`;
  await page.goto('/');
  const before = await dashboardGroceryCount(page);

  // The change is made outside the open page, through the API.
  const created = await createViaApi(page.request, {
    name,
    quantity: 1,
    category: 'Others',
    purchaseDate: isoOffset(0),
  });

  try {
    await openInventory(page);
    await page.getByRole('button', { name: 'Dashboard', exact: true }).click();
    // Wait for the new value: the old count stays on screen until the re-read finishes.
    const value = page
      .locator('.stat-card', { has: page.getByText('Grocery items', { exact: true }) })
      .locator('.stat-value');
    await expect(value).toHaveText(String(before.count + 1));
  } finally {
    await deleteViaApi(page.request, created.id);
  }
});
