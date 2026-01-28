import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';
const FACTION_ID = 'NEC';

async function getFactionData(request: import('@playwright/test').APIRequestContext) {
  const [dsRes, detRes] = await Promise.all([
    request.get(`${API_BASE}/api/factions/${FACTION_ID}/datasheets`),
    request.get(`${API_BASE}/api/factions/${FACTION_ID}/detachments`),
  ]);
  const datasheets = await dsRes.json();
  const detachments = await detRes.json();
  const character = datasheets.find((ds: { role: string }) => ds.role === 'Characters');
  const battleline = datasheets.find((ds: { role: string }) => ds.role === 'Battleline');

  const charDetail = await (await request.get(`${API_BASE}/api/datasheets/${character.id}`)).json();
  const blDetail = await (await request.get(`${API_BASE}/api/datasheets/${battleline.id}`)).json();

  return { datasheets, detachments, character, battleline, charDetail, blDetail };
}

test('points exceeded shows validation error', async ({ page, request }) => {
  const { character, battleline, detachments, charDetail, blDetail } = await getFactionData(request);
  const charLine = charDetail.costs[0]?.line ?? 1;
  const blLine = blDetail.costs[0]?.line ?? 1;

  const army = {
    factionId: FACTION_ID,
    battleSize: 'Incursion' as const,
    detachmentId: detachments[0].detachmentId,
    warlordId: character.id,
    units: Array.from({ length: 20 }, () => ({
      datasheetId: battleline.id,
      sizeOptionLine: blLine,
      enhancementId: null,
      attachedLeaderId: null,
    })).concat([{
      datasheetId: character.id,
      sizeOptionLine: charLine,
      enhancementId: null,
      attachedLeaderId: null,
    }]),
  };

  const apiRes = await request.post(`${API_BASE}/api/armies/validate`, { data: army });
  const validation = await apiRes.json();
  expect(validation.valid).toBe(false);
  expect(validation.errors.some((e: { errorType: string }) => e.errorType === 'PointsExceeded')).toBeTruthy();

  await page.goto(`/factions/${FACTION_ID}/armies/new`);
  await page.getByTestId('battle-size-select').selectOption('Incursion');

  for (let i = 0; i < 6; i++) {
    const blItem = page.locator('[data-testid="unit-picker-item"]').filter({ hasText: battleline.name });
    await blItem.first().getByTestId('add-unit-button').click();
  }

  const charItem = page.locator('[data-testid="unit-picker-item"]').filter({ hasText: character.name });
  await charItem.first().getByTestId('add-unit-button').click();

  await expect(page.getByTestId('validation-errors')).toBeVisible({ timeout: 5000 });
});

test('no character shows validation error', async ({ request }) => {
  const { battleline, detachments, blDetail } = await getFactionData(request);
  const blLine = blDetail.costs[0]?.line ?? 1;

  const army = {
    factionId: FACTION_ID,
    battleSize: 'StrikeForce' as const,
    detachmentId: detachments[0].detachmentId,
    warlordId: battleline.id,
    units: [{
      datasheetId: battleline.id,
      sizeOptionLine: blLine,
      enhancementId: null,
      attachedLeaderId: null,
    }],
  };

  const apiRes = await request.post(`${API_BASE}/api/armies/validate`, { data: army });
  const validation = await apiRes.json();
  expect(validation.valid).toBe(false);
  expect(validation.errors.some((e: { errorType: string }) => e.errorType === 'NoCharacter')).toBeTruthy();
});

test('valid army returns no errors', async ({ request }) => {
  const { character, battleline, detachments, charDetail, blDetail } = await getFactionData(request);
  const charLine = charDetail.costs[0]?.line ?? 1;
  const blLine = blDetail.costs[0]?.line ?? 1;

  const army = {
    factionId: FACTION_ID,
    battleSize: 'StrikeForce' as const,
    detachmentId: detachments[0].detachmentId,
    warlordId: character.id,
    units: [
      { datasheetId: character.id, sizeOptionLine: charLine, enhancementId: null, attachedLeaderId: null },
      { datasheetId: battleline.id, sizeOptionLine: blLine, enhancementId: null, attachedLeaderId: null },
    ],
  };

  const apiRes = await request.post(`${API_BASE}/api/armies/validate`, { data: army });
  const validation = await apiRes.json();
  expect(validation.valid).toBe(true);
  expect(validation.errors).toHaveLength(0);
});

test('points total updates live when adding and removing units', async ({ page, request }) => {
  const { battleline } = await getFactionData(request);

  await page.goto(`/factions/${FACTION_ID}/armies/new`);
  await expect(page.getByTestId('unit-picker')).toBeVisible();

  const pointsBefore = await page.getByTestId('points-total').textContent();
  expect(pointsBefore).toContain('Points: 0');

  const blItem = page.locator('[data-testid="unit-picker-item"]').filter({ hasText: battleline.name });
  await blItem.first().getByTestId('add-unit-button').click();

  const pointsAfter = await page.getByTestId('points-total').textContent();
  expect(pointsAfter).not.toContain('Points: 0');

  await page.getByTestId('remove-unit').first().click();

  const pointsFinal = await page.getByTestId('points-total').textContent();
  expect(pointsFinal).toContain('Points: 0');
});
