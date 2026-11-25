import { test, expect } from '@playwright/test';

function rowFor(page, text) {
  return page.getByRole('row').filter({ hasText: text });
}

async function openInventory(page) {
  await page.getByRole('button', { name: 'Pantry inventory' }).click();
}

// Reads the Grocery items count from the dashboard. Waits until the shared list has loaded.
async function groceryCount(page) {
  await page.getByRole('button', { name: 'Dashboard', exact: true }).click();
  const value = page
    .locator('.stat-card', { has: page.getByText('Grocery items', { exact: true }) })
    .locator('.stat-value');
  await expect(value).toHaveText(/^\d+$/);
  return Number(await value.innerText());
}

test('edit an item, cancel and confirm a delete, and keep the dashboard in step', async ({ page }) => {
  const name = `E2E Edit ${Date.now()}`;
  const renamed = `${name} renamed`;

  // Create the item through the existing Add item form.
  await page.goto('/');
  await openInventory(page);
  await page.getByRole('button', { name: 'Add item' }).click();
  const addDialog = page.getByRole('dialog', { name: 'Add item' });
  await addDialog.getByLabel('Name', { exact: true }).fill(name);
  await addDialog.getByLabel('Quantity', { exact: true }).fill('2');
  await addDialog.getByLabel('Category', { exact: true }).selectOption('Fruits');
  await addDialog.getByRole('button', { name: 'Save item' }).click();
  await expect(addDialog).toBeHidden();
  await expect(rowFor(page, name)).toBeVisible();

  const countBefore = await groceryCount(page);
  await openInventory(page);

  // Edit opens pre-filled. Change the name and quantity, then save.
  await page.getByRole('button', { name: `Edit ${name}`, exact: true }).click();
  const editDialog = page.getByRole('dialog', { name: 'Edit item' });
  await expect(editDialog).toBeVisible();
  await expect(editDialog.getByLabel('Name', { exact: true })).toHaveValue(name);
  await expect(editDialog.getByLabel('Quantity', { exact: true })).toHaveValue('2');
  await expect(editDialog.getByLabel('Category', { exact: true })).toHaveValue('Fruits');
  await editDialog.getByLabel('Name', { exact: true }).fill(renamed);
  await editDialog.getByLabel('Quantity', { exact: true }).fill('5');
  await editDialog.getByRole('button', { name: 'Save item' }).click();
  await expect(editDialog).toBeHidden();
  await expect(page.locator('.toast')).toHaveText(`Updated ${renamed}.`);

  const row = rowFor(page, renamed);
  await expect(row).toBeVisible();
  await expect(row.getByRole('cell', { name: '5', exact: true })).toBeVisible();
  await expect(rowFor(page, name).filter({ hasNotText: renamed })).toHaveCount(0);

  // Reload: the new values persist. groceryCount waits until the list has loaded again.
  await page.reload();
  expect(await groceryCount(page)).toBe(countBefore);
  await openInventory(page);
  const reloaded = rowFor(page, renamed);
  await expect(reloaded).toBeVisible();
  await expect(reloaded.getByRole('cell', { name: '5', exact: true })).toBeVisible();

  // Delete, then Cancel: the item stays.
  const deleteDialog = page.getByRole('dialog', { name: `Delete ${renamed}?`, exact: true });
  await page.getByRole('button', { name: `Delete ${renamed}`, exact: true }).click();
  await expect(deleteDialog).toBeVisible();
  await expect(deleteDialog.getByRole('button', { name: 'Cancel', exact: true })).toBeFocused();
  await deleteDialog.getByRole('button', { name: 'Cancel', exact: true }).click();
  await expect(deleteDialog).toBeHidden();
  await expect(rowFor(page, renamed)).toBeVisible();

  // Escape also closes the confirmation without deleting.
  await page.getByRole('button', { name: `Delete ${renamed}`, exact: true }).click();
  await expect(deleteDialog).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(deleteDialog).toBeHidden();
  await expect(rowFor(page, renamed)).toBeVisible();

  // Delete and confirm: the row goes, the toast shows, and the dashboard count drops by one.
  await page.getByRole('button', { name: `Delete ${renamed}`, exact: true }).click();
  await expect(deleteDialog).toBeVisible();
  await deleteDialog.getByRole('button', { name: 'Delete', exact: true }).click();
  await expect(deleteDialog).toBeHidden();
  await expect(page.locator('.toast')).toHaveText(`Deleted ${renamed}.`);
  await expect(rowFor(page, renamed)).toHaveCount(0);
  expect(await groceryCount(page)).toBe(countBefore - 1);

  // Reload: the item is still gone.
  await page.reload();
  expect(await groceryCount(page)).toBe(countBefore - 1);
  await openInventory(page);
  await expect(rowFor(page, renamed)).toHaveCount(0);
});
