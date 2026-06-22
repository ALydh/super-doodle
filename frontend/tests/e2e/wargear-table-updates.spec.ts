import { test, expect, type Page, type APIRequestContext } from '@playwright/test';

const API_BASE = 'http://localhost:8080';
const FACTION_ID = 'NEC';
const OVERLORD_NAME = 'Overlord';

async function ensureUser(request: APIRequestContext, username: string, password: string): Promise<void> {
  const loginRes = await request.post(`${API_BASE}/api/auth/login`, {
    data: { username, password },
    headers: { 'Content-Type': 'application/json' },
  });
  if (loginRes.ok()) return;
  await request.post(`${API_BASE}/api/auth/register`, {
    data: { username, password },
    headers: { 'Content-Type': 'application/json' },
  });
}

async function login(page: Page, username: string, password: string) {
  await page.goto('/login');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Login' }).click();
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 });
}

async function getWeaponNames(page: Page): Promise<string[]> {
  const headings = page.getByRole('heading', { name: /^(Ranged|Melee) Weapons$/ });
  const headingCount = await headings.count();
  const names: string[] = [];
  for (let h = 0; h < headingCount; h++) {
    const tableContainer = headings.nth(h).locator('..');
    const rows = tableContainer.locator('table tbody tr');
    const rowCount = await rows.count();
    for (let i = 0; i < rowCount; i++) {
      const cell = await rows.nth(i).locator('td').first().textContent();
      names.push((cell ?? '').trim());
    }
  }
  return names.sort();
}

test('clicking a wargear option updates the weapons table', async ({ page, request }) => {
  const username = 'wargear-bug-tester';
  const password = 'TestPassword123!';
  await ensureUser(request, username, password);
  const filterRequests: { status: number; reqBody: string; respCount: number }[] = [];
  page.on('request', (req) => {
    if (req.url().includes('/filter-wargear')) {
      console.log('REQ filter-wargear body:', req.postData());
    }
  });
  page.on('response', async (res) => {
    if (res.url().includes('/filter-wargear')) {
      const body = await res.json().catch(() => null);
      filterRequests.push({
        status: res.status(),
        reqBody: res.request().postData() ?? '',
        respCount: Array.isArray(body) ? body.length : -1,
      });
    }
  });

  await login(page, username, password);
  await page.goto(`/factions/${FACTION_ID}/armies/new`);

  // Open the unit picker modal.
  await page.getByRole('button', { name: /Add Units/ }).click();

  // Add Overlord. Picker LIs contain the unit name and a `+` add button.
  const overlordItem = page.locator('li').filter({ hasText: OVERLORD_NAME }).first();
  await expect(overlordItem).toBeVisible();
  await overlordItem.getByRole('button', { name: '+' }).click();

  // Close the picker modal via the ✕ button.
  await page.getByRole('button', { name: '✕' }).click();

  // Expand the unit row (the row header is a button with aria-label "Overlord, expand details").
  const header = page.getByRole('button', { name: new RegExp(`^${OVERLORD_NAME}, expand details`) });
  await expect(header).toBeVisible();

  // Wait for the initial /filter-wargear response triggered by expansion.
  const initialFilter = page.waitForResponse(/filter-wargear/);
  await header.click();
  await initialFilter;

  // Wait for the Weapons table to appear.
  await expect(page.getByRole('heading', { name: /^(Ranged|Melee) Weapons$/ }).first()).toBeVisible();
  const initialWeapons = await getWeaponNames(page);
  console.log('Initial weapons:', initialWeapons);
  expect(initialWeapons.length).toBeGreaterThan(0);

  // Expand the WargearSelector (button text "Configure Wargear" when nothing selected).
  const configureBtn = page.getByRole('button', { name: /Configure Wargear|Change Wargear/ });
  await expect(configureBtn).toBeVisible();
  await configureBtn.click();

  // The expanded view shows option cards. Each card has an "indicator" div and the option's
  // description. We click the first option card to toggle it on.
  // After expansion, "Done" button is rendered. Look for clickable cards rendered as siblings.
  const doneBtn = page.getByRole('button', { name: 'Done' });
  await expect(doneBtn).toBeVisible();

  // The option cards are clickable divs that follow the Done button inside the same container.
  // We grab them by locating the parent of Done and selecting the direct-child divs.
  const cardsContainer = page.locator('div').filter({ has: doneBtn }).last();
  const optionCards = cardsContainer.locator('> div');
  const cardCount = await optionCards.count();
  console.log('Option card count:', cardCount);
  expect(cardCount).toBeGreaterThan(0);

  // Listen for the next filter-wargear response triggered by selecting an option.
  const selectFilter1 = page.waitForResponse(/filter-wargear/, { timeout: 10000 }).catch(() => null);

  // Click the first option card.
  await optionCards.first().click();
  await selectFilter1;
  await page.waitForTimeout(300);

  // The first option is a "replace with one of the following" — after toggling on, a dropdown
  // appears. Pick a non-default choice so the filter has something to substitute.
  const choiceSelect = page.locator('select').filter({ hasText: 'Select wargear' }).first();
  if (await choiceSelect.isVisible().catch(() => false)) {
    const options = await choiceSelect.locator('option').allTextContents();
    console.log('First option dropdown choices:', options);
    const nonEmpty = options.find((o) => o.trim() && !/^Select/i.test(o));
    if (nonEmpty) {
      const selectFilter2 = page.waitForResponse(/filter-wargear/, { timeout: 10000 }).catch(() => null);
      await choiceSelect.selectOption({ label: nonEmpty });
      await selectFilter2;
      await page.waitForTimeout(300);
    }
  }

  const afterFirst = await getWeaponNames(page);
  console.log('Weapons after toggling option 1 + dropdown:', afterFirst);

  // Now also try toggling the second option card (a simple replace).
  const selectFilter3 = page.waitForResponse(/filter-wargear/, { timeout: 10000 }).catch(() => null);
  await optionCards.nth(1).click();
  await selectFilter3;
  await page.waitForTimeout(500);

  const afterSecond = await getWeaponNames(page);
  console.log('Weapons after toggling option 2:', afterSecond);

  console.log('All filter-wargear requests seen:', JSON.stringify(filterRequests, null, 2));

  // The bug being reproduced: the weapons table does NOT update after toggling wargear.
  // Use the strongest comparison — does the table differ from the initial set after we picked
  // a concrete wargear substitution?
  expect(afterFirst, 'weapons table should change after picking a wargear substitution').not.toEqual(initialWeapons);
});

test('clicking an option card alone (no dropdown) updates the weapons table', async ({ page, request }) => {
  // This reproduces the user-reported flow literally: click the wargear option card and
  // expect the weapons table to update. No dropdown interaction, no dwell, just a click.
  const username = 'wargear-bug-tester';
  const password = 'TestPassword123!';
  await ensureUser(request, username, password);

  await login(page, username, password);
  await page.goto(`/factions/${FACTION_ID}/armies/new`);

  await page.getByRole('button', { name: /Add Units/ }).click();
  const overlordItem = page.locator('li').filter({ hasText: OVERLORD_NAME }).first();
  await overlordItem.getByRole('button', { name: '+' }).click();
  await page.getByRole('button', { name: '✕' }).click();

  const header = page.getByRole('button', { name: new RegExp(`^${OVERLORD_NAME}, expand details`) });
  const initialFilter = page.waitForResponse(/filter-wargear/);
  await header.click();
  await initialFilter;
  await expect(page.getByRole('heading', { name: /^(Ranged|Melee) Weapons$/ }).first()).toBeVisible();

  const initialWeapons = await getWeaponNames(page);
  console.log('Initial weapons:', initialWeapons);

  await page.getByRole('button', { name: /Configure Wargear|Change Wargear/ }).click();
  const doneBtn = page.getByRole('button', { name: 'Done' });
  await expect(doneBtn).toBeVisible();
  const cardsContainer = page.locator('div').filter({ has: doneBtn }).last();
  const optionCards = cardsContainer.locator('> div');

  // Click option 1 only — no dropdown choice. User expects the table to react.
  const filterAfterClick = page.waitForResponse(/filter-wargear/, { timeout: 10000 });
  await optionCards.first().click();
  const resp = await filterAfterClick;
  const respBody = await resp.json();
  console.log('Server returned weapons after card click:', respBody.map((w: { wargear: { name: string } }) => w.wargear.name));

  await page.waitForTimeout(500);
  const updatedWeapons = await getWeaponNames(page);
  console.log('Weapons in table after card click:', updatedWeapons);

  expect(updatedWeapons, 'weapons table should differ after clicking a wargear option card').not.toEqual(initialWeapons);
});
