import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';

test('should display list of factions when page loads', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByTestId('faction-list')).toBeVisible();

  await expect(page.getByText('Space Marines', { exact: true })).toBeVisible();
  await expect(page.getByText('Orks')).toBeVisible();
  await expect(page.getByText('Tyranids')).toBeVisible();
});

test('faction count matches API response', async ({ page, request }) => {
  await page.goto('/');
  await expect(page.getByTestId('faction-list')).toBeVisible();

  const apiResponse = await request.get(`${API_BASE}/api/factions`);
  expect(apiResponse.ok()).toBeTruthy();
  const apiFactions = await apiResponse.json();

  const uiItems = page.getByTestId('faction-item');
  await expect(uiItems).toHaveCount(apiFactions.length);
});
