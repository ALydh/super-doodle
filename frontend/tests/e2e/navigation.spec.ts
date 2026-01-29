import { test, expect } from '@playwright/test';

test('full navigation flow: factions -> faction -> unit -> back -> back', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('.faction-list')).toBeVisible();

  await page.getByRole('link', { name: 'Necrons' }).click();
  await expect(page).toHaveURL(/\/factions\/NEC$/);
  await expect(page.locator('.datasheet-list')).toBeVisible();

  await page.locator('.datasheet-item').first().getByRole('link').click();
  await expect(page).toHaveURL(/\/datasheets\/\d{9}$/);
  await expect(page.locator('.unit-name')).toBeVisible();

  await page.locator('.back-link').click();
  await expect(page).toHaveURL(/\/factions\/NEC$/);
  await expect(page.locator('.datasheet-list')).toBeVisible();

  await page.locator('.back-link').click();
  await expect(page).toHaveURL(/\/$/);
  await expect(page.locator('.faction-list')).toBeVisible();
});

test('browser back button works through navigation', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('.faction-list')).toBeVisible();

  await page.getByRole('link', { name: 'Necrons' }).click();
  await expect(page.locator('.datasheet-list')).toBeVisible();

  await page.locator('.datasheet-item').first().getByRole('link').click();
  await expect(page.locator('.unit-name')).toBeVisible();

  await page.goBack();
  await expect(page.locator('.datasheet-list')).toBeVisible();

  await page.goBack();
  await expect(page.locator('.faction-list')).toBeVisible();
});
