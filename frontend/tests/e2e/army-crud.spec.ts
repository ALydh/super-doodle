import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';
const FACTION_ID = 'NEC';

function uniqueName(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
}

async function createArmyViaApi(request: import('@playwright/test').APIRequestContext) {
  const datasheetsRes = await request.get(`${API_BASE}/api/factions/${FACTION_ID}/datasheets`);
  const datasheets = await datasheetsRes.json();

  const character = datasheets.find((ds: { role: string }) => ds.role === 'Characters');
  const battleline = datasheets.find((ds: { role: string }) => ds.role === 'Battleline');

  const detRes = await request.get(`${API_BASE}/api/factions/${FACTION_ID}/detachments`);
  const detachments = await detRes.json();

  const costsRes = await request.get(`${API_BASE}/api/datasheets/${character.id}`);
  const charDetail = await costsRes.json();
  const charLine = charDetail.costs[0]?.line ?? 1;

  const blCostsRes = await request.get(`${API_BASE}/api/datasheets/${battleline.id}`);
  const blDetail = await blCostsRes.json();
  const blLine = blDetail.costs[0]?.line ?? 1;

  const army = {
    factionId: FACTION_ID,
    battleSize: 'StrikeForce',
    detachmentId: detachments[0].detachmentId,
    warlordId: character.id,
    units: [
      { datasheetId: character.id, sizeOptionLine: charLine, enhancementId: null, attachedLeaderId: null },
      { datasheetId: battleline.id, sizeOptionLine: blLine, enhancementId: null, attachedLeaderId: null },
    ],
  };

  const res = await request.post(`${API_BASE}/api/armies`, {
    data: { name: uniqueName('Test Army'), army },
  });
  expect(res.ok()).toBeTruthy();
  return res.json();
}

test('create army via UI and cross-check with API', async ({ page, request }) => {
  await page.goto(`/factions/${FACTION_ID}/armies/new`);
  await expect(page.getByTestId('builder-title')).toHaveText('Create Army');

  await page.getByTestId('army-name-input').fill('My Necron Host');
  await page.getByTestId('battle-size-select').selectOption('StrikeForce');

  await page.getByTestId('unit-search').fill('');
  const addButtons = page.getByTestId('add-unit-button');
  await addButtons.first().click();

  const charItems = page.locator('[data-testid="unit-picker-item"]').filter({ hasText: 'Characters' });
  if (await charItems.count() > 0) {
    await charItems.first().getByTestId('add-unit-button').click();
  }

  const unitRows = page.getByTestId('unit-row');
  expect(await unitRows.count()).toBeGreaterThan(0);

  const warlordRadios = page.getByTestId('warlord-radio');
  if (await warlordRadios.count() > 0) {
    await warlordRadios.first().click({ force: true });
  }

  await page.getByTestId('save-army').click();
  await expect(page).toHaveURL(/\/armies\/[a-f0-9-]+$/);

  const armyId = page.url().split('/armies/')[1];
  const apiRes = await request.get(`${API_BASE}/api/armies/${armyId}`);
  expect(apiRes.ok()).toBeTruthy();
  const apiArmy = await apiRes.json();
  expect(apiArmy.name).toBe('My Necron Host');
  expect(apiArmy.army.battleSize).toBe('StrikeForce');
});

test('army appears in faction list after creation', async ({ page, request }) => {
  const persisted = await createArmyViaApi(request);

  await page.goto(`/factions/${FACTION_ID}`);
  await expect(page.getByTestId('army-list-section')).toBeVisible();
  await expect(page.getByText(persisted.name).first()).toBeVisible();

  const apiRes = await request.get(`${API_BASE}/api/factions/${FACTION_ID}/armies`);
  const armies = await apiRes.json();
  expect(armies.some((a: { id: string }) => a.id === persisted.id)).toBeTruthy();
});

test('edit army via UI and cross-check with API', async ({ page, request }) => {
  const persisted = await createArmyViaApi(request);

  await page.goto(`/armies/${persisted.id}/edit`);
  await expect(page.getByTestId('builder-title')).toHaveText('Edit Army');

  await page.getByTestId('army-name-input').fill('Updated Army Name');
  await page.getByTestId('save-army').click();
  await expect(page).toHaveURL(`/armies/${persisted.id}`);

  const apiRes = await request.get(`${API_BASE}/api/armies/${persisted.id}`);
  const updated = await apiRes.json();
  expect(updated.name).toBe('Updated Army Name');
});

test('delete army via UI and cross-check with API', async ({ page, request }) => {
  const persisted = await createArmyViaApi(request);

  await page.goto(`/armies/${persisted.id}`);
  await expect(page.getByTestId('army-name')).toHaveText(persisted.name);

  await page.getByTestId('delete-army').click();
  await expect(page).toHaveURL(`/factions/${FACTION_ID}`);

  const apiRes = await request.get(`${API_BASE}/api/armies/${persisted.id}`);
  expect(apiRes.status()).toBe(404);
});

test('view army shows correct composition', async ({ page, request }) => {
  const persisted = await createArmyViaApi(request);

  await page.goto(`/armies/${persisted.id}`);
  await expect(page.getByTestId('army-name')).toHaveText(persisted.name);
  await expect(page.getByTestId('army-battle-size')).toContainText('StrikeForce');

  const unitItems = page.getByTestId('army-view-unit');
  await expect(unitItems).toHaveCount(persisted.army.units.length);
});
