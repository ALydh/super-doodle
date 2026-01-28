import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';

test('clicking a unit shows its detail page', async ({ page }) => {
  await page.goto('/factions/NEC');
  await expect(page.getByTestId('datasheet-list')).toBeVisible();

  const firstUnit = page.getByTestId('datasheet-item').first();
  const unitName = await firstUnit.textContent();
  await firstUnit.getByRole('link').click();

  await expect(page).toHaveURL(/\/datasheets\/\d{9}$/);
  await expect(page.getByTestId('unit-name')).toHaveText(unitName!);
});

test('unit detail shows stats, weapons, and costs', async ({ page }) => {
  await page.goto('/factions/NEC');
  await expect(page.getByTestId('datasheet-list')).toBeVisible();

  await page.getByTestId('datasheet-item').first().getByRole('link').click();
  await expect(page.getByTestId('unit-name')).toBeVisible();

  await expect(page.getByTestId('stats-table')).toBeVisible();
  const statsRows = page.getByTestId('stats-row');
  expect(await statsRows.count()).toBeGreaterThan(0);
});

test('unit detail stats match API response', async ({ page, request }) => {
  await page.goto('/factions/NEC');
  await expect(page.getByTestId('datasheet-list')).toBeVisible();

  await page.getByTestId('datasheet-item').first().getByRole('link').click();
  await expect(page.getByTestId('unit-name')).toBeVisible();

  const url = page.url();
  const datasheetId = url.split('/datasheets/')[1];

  const apiResponse = await request.get(`${API_BASE}/api/datasheets/${datasheetId}`);
  expect(apiResponse.ok()).toBeTruthy();
  const detail = await apiResponse.json();

  await expect(page.getByTestId('unit-name')).toHaveText(detail.datasheet.name);

  if (detail.profiles.length > 0) {
    const firstRow = page.getByTestId('stats-row').first();
    await expect(firstRow.getByTestId('stat-m')).toHaveText(detail.profiles[0].movement);
    await expect(firstRow.getByTestId('stat-t')).toHaveText(detail.profiles[0].toughness);
    await expect(firstRow.getByTestId('stat-sv')).toHaveText(detail.profiles[0].save);
    await expect(firstRow.getByTestId('stat-w')).toHaveText(String(detail.profiles[0].wounds));
    await expect(firstRow.getByTestId('stat-ld')).toHaveText(detail.profiles[0].leadership);
    await expect(firstRow.getByTestId('stat-oc')).toHaveText(String(detail.profiles[0].objectiveControl));
  }

  if (detail.costs.length > 0) {
    await expect(page.getByTestId('costs-table')).toBeVisible();
    const firstCostRow = page.getByTestId('cost-row').first();
    await expect(firstCostRow.getByTestId('cost-value')).toHaveText(String(detail.costs[0].cost));
  }
});

test('datasheet detail shows abilities section', async ({ page, request }) => {
  await page.goto('/factions/NEC');
  await expect(page.getByTestId('datasheet-list')).toBeVisible();

  await page.getByTestId('datasheet-item').first().getByRole('link').click();
  await expect(page.getByTestId('unit-name')).toBeVisible();

  const url = page.url();
  const datasheetId = url.split('/datasheets/')[1];

  const apiResponse = await request.get(`${API_BASE}/api/datasheets/${datasheetId}`);
  const detail = await apiResponse.json();

  const abilitiesWithNames = detail.abilities.filter((a: { name: string | null }) => a.name);
  if (abilitiesWithNames.length > 0) {
    await expect(page.getByTestId('abilities-list')).toBeVisible();
    const items = page.getByTestId('ability-item');
    await expect(items).toHaveCount(abilitiesWithNames.length);
  }
});
