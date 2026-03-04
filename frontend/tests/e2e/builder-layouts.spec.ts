import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';
const FACTION_ID = 'NEC';

async function createTestArmy(request: import('@playwright/test').APIRequestContext) {
  const datasheetsRes = await request.get(`${API_BASE}/api/factions/${FACTION_ID}/datasheets`);
  const datasheets = await datasheetsRes.json();
  const character = datasheets.find((ds: { role: string }) => ds.role === 'Characters');
  const battleline = datasheets.find((ds: { role: string }) => ds.role === 'Battleline');
  const detRes = await request.get(`${API_BASE}/api/factions/${FACTION_ID}/detachments`);
  const detachments = await detRes.json();
  const charDetail = await (await request.get(`${API_BASE}/api/datasheets/${character.id}`)).json();
  const blDetail = await (await request.get(`${API_BASE}/api/datasheets/${battleline.id}`)).json();

  const res = await request.post(`${API_BASE}/api/armies`, {
    data: {
      name: `Builder Test ${Date.now()}`,
      army: {
        factionId: FACTION_ID,
        battleSize: 'StrikeForce',
        detachmentId: detachments[0].detachmentId,
        warlordId: character.id,
        units: [
          { datasheetId: character.id, sizeOptionLine: charDetail.costs[0]?.line ?? 1, enhancementId: null, attachedLeaderId: null },
          { datasheetId: battleline.id, sizeOptionLine: blDetail.costs[0]?.line ?? 1, enhancementId: null, attachedLeaderId: null },
        ],
      },
    },
    headers: { 'Content-Type': 'application/json' },
  });
  const army = await res.json();
  return army.id as string;
}

async function deleteArmy(request: import('@playwright/test').APIRequestContext, id: string) {
  await request.delete(`${API_BASE}/api/armies/${id}`);
}

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login');
  await page.getByLabel('Username').fill('alex');
  await page.getByLabel('Password').fill('Duk199');
  await page.getByRole('button', { name: 'Login' }).click();
  await page.waitForURL((url) => !url.pathname.includes('/login'));
}

test.describe('Army Builder', () => {
  let armyId: string;

  test.beforeAll(async ({ request }) => {
    armyId = await createTestArmy(request);
  });

  test.afterAll(async ({ request }) => {
    await deleteArmy(request, armyId);
  });

  test('modal picker opens and closes', async ({ page }) => {
    await login(page);
    await page.goto(`/armies/${armyId}/edit`);
    await page.waitForLoadState('networkidle');

    // Picker not visible initially
    await expect(page.getByPlaceholder('Search units...')).not.toBeVisible();

    // Open modal
    await page.getByText('+ Add Units').click();
    await expect(page.getByPlaceholder('Search units...')).toBeVisible();

    // Close modal
    await page.getByRole('button', { name: '✕' }).click();
    await expect(page.getByPlaceholder('Search units...')).not.toBeVisible();

    await page.screenshot({ path: 'test-results/builder-closed.png', fullPage: false });
  });

  test('modal is fully within viewport when open', async ({ page }) => {
    await login(page);
    await page.goto(`/armies/${armyId}/edit`);
    await page.waitForLoadState('networkidle');

    await page.getByText('+ Add Units').click();
    await expect(page.getByPlaceholder('Search units...')).toBeVisible();

    const modal = page.locator('[class*="modal"]:not([class*="Overlay"]):not([class*="Header"]):not([class*="Body"]):not([class*="Title"])').first();
    const box = await modal.boundingBox();
    const viewport = page.viewportSize()!;

    expect(box).not.toBeNull();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.y).toBeGreaterThanOrEqual(0);
    expect(box!.x + box!.width).toBeLessThanOrEqual(viewport.width);
    expect(box!.y + box!.height).toBeLessThanOrEqual(viewport.height);

    await page.screenshot({ path: 'test-results/builder-modal-open.png', fullPage: false });
  });

  test('settings panel is accessible', async ({ page }) => {
    await login(page);
    await page.goto(`/armies/${armyId}/edit`);
    await page.waitForLoadState('networkidle');

    await expect(page.getByText('Settings')).toBeVisible();
    await page.getByText('Settings').click();
    await expect(page.getByLabel('Battle Size')).toBeVisible();
    await expect(page.getByLabel('Detachment')).toBeVisible();
  });
});
