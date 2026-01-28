import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';
const FACTION_ID = 'NEC';

test('detachment dropdown matches API data', async ({ page, request }) => {
  await page.goto(`/factions/${FACTION_ID}/armies/new`);
  await expect(page.getByTestId('detachment-select')).toBeVisible();

  const apiRes = await request.get(`${API_BASE}/api/factions/${FACTION_ID}/detachments`);
  expect(apiRes.ok()).toBeTruthy();
  const detachments = await apiRes.json();

  const options = page.getByTestId('detachment-select').locator('option');
  await expect(options).toHaveCount(detachments.length);

  for (const det of detachments) {
    await expect(page.getByTestId('detachment-select').locator(`option[value="${det.detachmentId}"]`)).toBeAttached();
  }
});

test('available units match faction datasheets', async ({ page, request }) => {
  await page.goto(`/factions/${FACTION_ID}/armies/new`);
  await expect(page.getByTestId('unit-picker')).toBeVisible();

  const apiRes = await request.get(`${API_BASE}/api/factions/${FACTION_ID}/datasheets`);
  expect(apiRes.ok()).toBeTruthy();
  const datasheets = await apiRes.json();

  const nonVirtual = datasheets.filter((ds: { virtual: boolean }) => !ds.virtual);
  const pickerItems = page.getByTestId('unit-picker-item');
  await expect(pickerItems).toHaveCount(nonVirtual.length);
});
