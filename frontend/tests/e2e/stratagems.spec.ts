import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';
const FACTION_ID = 'NEC';

test('stratagems API returns data for a faction', async ({ request }) => {
  const res = await request.get(`${API_BASE}/api/factions/${FACTION_ID}/stratagems`);
  expect(res.ok()).toBeTruthy();
  const stratagems = await res.json();
  expect(Array.isArray(stratagems)).toBe(true);
  expect(stratagems.length).toBeGreaterThan(0);
  expect(stratagems[0]).toHaveProperty('name');
  expect(stratagems[0]).toHaveProperty('description');
});

test('faction page displays stratagems list', async ({ page }) => {
  await page.goto(`/factions/${FACTION_ID}`);
  await expect(page.locator('.stratagems-list')).toBeVisible();

  const items = page.locator('.stratagem-item');
  expect(await items.count()).toBeGreaterThan(0);
});
