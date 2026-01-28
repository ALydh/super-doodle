import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';
const FACTION_ID = 'NEC';
const DETACHMENT_ID = '000000815';

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

test('detachment abilities appear when selecting a detachment', async ({ page, request }) => {
  await page.goto(`/factions/${FACTION_ID}/armies/new`);
  await expect(page.getByTestId('detachment-select')).toBeVisible();

  await page.getByTestId('detachment-select').selectOption(DETACHMENT_ID);

  await expect(page.getByTestId('detachment-abilities-section')).toBeVisible();
  const toggle = page.getByTestId('detachment-abilities-toggle');
  await expect(toggle).toBeVisible();

  await toggle.click();
  await expect(page.getByTestId('detachment-abilities-list')).toBeVisible();

  const apiRes = await request.get(`${API_BASE}/api/detachments/${DETACHMENT_ID}/abilities`);
  expect(apiRes.ok()).toBeTruthy();
  const abilities = await apiRes.json();

  const items = page.getByTestId('detachment-ability-item');
  await expect(items).toHaveCount(abilities.length);
});

test('detachment stratagems appear filtered by selected detachment', async ({ page, request }) => {
  await page.goto(`/factions/${FACTION_ID}/armies/new`);
  await expect(page.getByTestId('detachment-select')).toBeVisible();

  await page.getByTestId('detachment-select').selectOption(DETACHMENT_ID);

  await expect(page.getByTestId('detachment-stratagems-section')).toBeVisible();
  const toggle = page.getByTestId('detachment-stratagems-toggle');
  await expect(toggle).toBeVisible();

  await toggle.click();
  await expect(page.getByTestId('detachment-stratagems-list')).toBeVisible();

  const apiRes = await request.get(`${API_BASE}/api/factions/${FACTION_ID}/stratagems`);
  expect(apiRes.ok()).toBeTruthy();
  const allStratagems = await apiRes.json();
  const filteredStratagems = allStratagems.filter((s: { detachmentId: string }) => s.detachmentId === DETACHMENT_ID);

  const items = page.getByTestId('detachment-stratagem-item');
  await expect(items).toHaveCount(filteredStratagems.length);
});
