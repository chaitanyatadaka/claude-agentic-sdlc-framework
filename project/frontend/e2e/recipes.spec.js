import { test, expect } from '@playwright/test';

// The API response is mocked so the screen can be checked without depending on pantry contents.
// Shape: name, ingredients, matchedIngredients, missingIngredients, makeable, usesExpiringSoon.
const RECIPES = [
  {
    name: 'Scrambled Eggs',
    ingredients: ['Eggs', 'Butter'],
    matchedIngredients: ['Eggs', 'Butter'],
    missingIngredients: [],
    makeable: true,
    usesExpiringSoon: true,
  },
  {
    name: 'Garlic Bread',
    ingredients: ['Bread', 'Butter', 'Garlic'],
    matchedIngredients: ['Butter'],
    missingIngredients: ['Bread', 'Garlic'],
    makeable: false,
    usesExpiringSoon: false,
  },
];

test('Recipes page renders cards and shows a fully makeable recipe with no missing ingredients', async ({ page }) => {
  await page.route('**/api/recipes', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(RECIPES) }),
  );

  await page.goto('/');
  await page.getByRole('button', { name: 'Recipes', exact: true }).click();

  await expect(page.getByRole('heading', { name: 'Recipes', level: 1 })).toBeVisible();

  const cards = page.locator('.recipe-card');
  await expect(cards).toHaveCount(2);

  const makeable = cards.filter({ has: page.getByRole('heading', { name: 'Scrambled Eggs' }) });
  await expect(makeable.getByText('Makeable', { exact: true })).toBeVisible();
  await expect(makeable.getByText('Uses an item expiring soon')).toBeVisible();
  await expect(makeable.getByRole('list', { name: 'Missing ingredients for Scrambled Eggs' })).toHaveCount(0);

  const notMakeable = cards.filter({ has: page.getByRole('heading', { name: 'Garlic Bread' }) });
  await expect(notMakeable.getByText('Not makeable', { exact: true })).toBeVisible();
  const missing = notMakeable.getByRole('list', { name: 'Missing ingredients for Garlic Bread' });
  await expect(missing.getByText('Bread', { exact: true })).toBeVisible();
  await expect(missing.getByText('Garlic', { exact: true })).toBeVisible();
});
