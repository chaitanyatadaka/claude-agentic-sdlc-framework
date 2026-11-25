import { test, expect } from '@playwright/test';

test('add an item through the form and see it persisted in the inventory', async ({ page }) => {
  const name = `E2E Fruit ${Date.now()}`;

  await page.goto('/');
  await page.getByRole('button', { name: 'Pantry inventory' }).click();

  const addButton = page.getByRole('button', { name: 'Add item' });
  await addButton.click();

  const dialog = page.getByRole('dialog', { name: 'Add item' });
  await expect(dialog).toBeVisible();

  // Empty submit shows the name required message and does not close the dialog.
  await dialog.getByRole('button', { name: 'Save item' }).click();
  await expect(dialog.getByText('Name is required.')).toBeVisible();
  await expect(dialog).toBeVisible();

  await dialog.getByLabel('Name', { exact: true }).fill(name);
  await dialog.getByLabel('Quantity', { exact: true }).fill('2');
  await dialog.getByLabel('Category', { exact: true }).selectOption('Fruits');

  // Marker on window: it survives only if the save does not reload the page.
  await page.evaluate(() => {
    window.__noReloadMarker = true;
  });
  await dialog.getByRole('button', { name: 'Save item' }).click();

  await expect(dialog).toBeHidden();
  const row = page.getByRole('row').filter({ hasText: name });
  await expect(row).toBeVisible();
  await expect(row.getByRole('cell', { name: '2', exact: true })).toBeVisible();
  await expect(row.getByRole('cell', { name: 'Fruits', exact: true })).toBeVisible();
  expect(await page.evaluate(() => window.__noReloadMarker)).toBe(true);
  await expect(addButton).toBeFocused();

  // Reload: the row is still there because it was saved to the backend.
  await page.reload();
  await page.getByRole('button', { name: 'Pantry inventory' }).click();
  await expect(page.getByRole('row').filter({ hasText: name })).toBeVisible();

  // Invalid quantity 0 shows a message next to the quantity field and blocks the save.
  await page.getByRole('button', { name: 'Add item' }).click();
  const second = page.getByRole('dialog', { name: 'Add item' });
  await second.getByLabel('Name', { exact: true }).fill(`${name} zero`);
  await second.getByLabel('Quantity', { exact: true }).fill('0');
  await second.getByLabel('Category', { exact: true }).selectOption('Fruits');
  await second.getByRole('button', { name: 'Save item' }).click();

  // The has-locator is evaluated inside each .field, so it must not be chained from the dialog.
  const quantityField = second.locator('.field', { has: page.getByLabel('Quantity', { exact: true }) });
  await expect(quantityField.getByText('Quantity must be a whole number greater than 0.')).toBeVisible();
  await expect(second.getByLabel('Quantity', { exact: true })).toHaveAttribute('aria-invalid', 'true');

  // Escape closes the dialog without saving.
  await page.keyboard.press('Escape');
  await expect(second).toBeHidden();
  await expect(page.getByRole('row').filter({ hasText: `${name} zero` })).toHaveCount(0);
});
