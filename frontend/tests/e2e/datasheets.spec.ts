import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';

test('clicking a faction navigates to its datasheets', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByTestId('faction-list')).toBeVisible();

  await page.getByRole('link', { name: 'Necrons' }).click();

  await expect(page).toHaveURL(/\/factions\/NEC$/);
  await expect(page.getByTestId('datasheet-list')).toBeVisible();
});

test('datasheet count matches API response', async ({ page, request }) => {
  await page.goto('/factions/NEC');
  await expect(page.getByTestId('datasheet-list')).toBeVisible();

  const apiResponse = await request.get(`${API_BASE}/api/factions/NEC/datasheets`);
  expect(apiResponse.ok()).toBeTruthy();
  const apiDatasheets = await apiResponse.json();

  const uiItems = page.getByTestId('datasheet-item');
  await expect(uiItems).toHaveCount(apiDatasheets.length);
});

test('known Necron units are displayed', async ({ page }) => {
  await page.goto('/factions/NEC');
  await expect(page.getByTestId('datasheet-list')).toBeVisible();

  await expect(page.getByText('Necron Warriors')).toBeVisible();
});
