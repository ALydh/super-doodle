import { test, expect } from '@playwright/test';

test('full navigation flow: factions -> faction -> unit -> back -> back', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByTestId('faction-list')).toBeVisible();

  await page.getByRole('link', { name: 'Necrons' }).click();
  await expect(page).toHaveURL(/\/factions\/NEC$/);
  await expect(page.getByTestId('datasheet-list')).toBeVisible();

  await page.getByTestId('datasheet-item').first().getByRole('link').click();
  await expect(page).toHaveURL(/\/datasheets\/\d{9}$/);
  await expect(page.getByTestId('unit-name')).toBeVisible();

  await page.getByTestId('back-to-datasheets').click();
  await expect(page).toHaveURL(/\/factions\/NEC$/);
  await expect(page.getByTestId('datasheet-list')).toBeVisible();

  await page.getByTestId('back-to-factions').click();
  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByTestId('faction-list')).toBeVisible();
});

test('browser back button works through navigation', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByTestId('faction-list')).toBeVisible();

  await page.getByRole('link', { name: 'Necrons' }).click();
  await expect(page.getByTestId('datasheet-list')).toBeVisible();

  await page.getByTestId('datasheet-item').first().getByRole('link').click();
  await expect(page.getByTestId('unit-name')).toBeVisible();

  await page.goBack();
  await expect(page.getByTestId('datasheet-list')).toBeVisible();

  await page.goBack();
  await expect(page.getByTestId('faction-list')).toBeVisible();
});
