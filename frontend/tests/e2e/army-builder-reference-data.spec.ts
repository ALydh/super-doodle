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

test('wargear options appear for units that have them', async ({ page, request }) => {
  await page.goto(`/factions/${FACTION_ID}/armies/new`);
  await expect(page.getByTestId('unit-picker')).toBeVisible();

  // Add a unit that has wargear options (Overlord)
  const unitPickerItem = page.getByTestId('unit-picker-item').filter({ hasText: 'Add Overlord (Characters) —' });
  await expect(unitPickerItem).toBeVisible();
  await unitPickerItem.getByTestId('add-unit-button').click();

  // Wait for the unit to be added
  await expect(page.getByTestId('unit-row')).toBeVisible();

  // Check that wargear button appears and shows correct count
  const wargearButton = page.getByTestId('wargear-toggle');
  await expect(wargearButton).toBeVisible();
  
  // Get the datasheet details to check expected options count
  const datasheetRes = await request.get(`${API_BASE}/api/datasheets/000000523`);
  expect(datasheetRes.ok()).toBeTruthy();
  const datasheet = await datasheetRes.json();
  const expectedOptionsCount = datasheet.options.length;
  
  // Initially should show 0/expectedOptionsCount
  await expect(wargearButton).toHaveText(`0/${expectedOptionsCount}`);
});

test('wargear checkboxes can be toggled and notes can be entered', async ({ page }) => {
  await page.goto(`/factions/${FACTION_ID}/armies/new`);
  await expect(page.getByTestId('unit-picker')).toBeVisible();

  // Add an Overlord unit
  const unitPickerItem = page.getByTestId('unit-picker-item').filter({ hasText: 'Add Overlord (Characters) —' });
  await expect(unitPickerItem).toBeVisible();
  await unitPickerItem.getByTestId('add-unit-button').click();

  // Wait for the unit to be added
  await expect(page.getByTestId('unit-row')).toBeVisible();

  // Click wargear button to expand
  const wargearButton = page.getByTestId('wargear-toggle');
  await wargearButton.click();

  // Wait for wargear section to appear
  await expect(page.getByTestId('wargear-section')).toBeVisible();

  // Find the first wargear option checkbox (has choices)
  const firstCheckbox = page.getByTestId('wargear-option-1');
  await expect(firstCheckbox).toBeVisible();
  
  // Initially should be unchecked
  await expect(firstCheckbox).not.toBeChecked();

  // Check the first option (this should show a dropdown)
  await firstCheckbox.check();
  await expect(firstCheckbox).toBeChecked();

  // Verify the button text updates to show selection
  await expect(wargearButton).toContainText('1/');

  // Dropdown should appear for checked option (Overlord option 1 has choices)
  const choiceDropdown = page.getByTestId('wargear-choice-1');
  await expect(choiceDropdown).toBeVisible();

  // Select an option from dropdown
  await choiceDropdown.selectOption('1 voidscythe');
  await expect(choiceDropdown).toHaveValue('1 voidscythe');

  // Find second wargear option checkbox (simple option, no dropdown)
  const secondCheckbox = page.getByTestId('wargear-option-2');
  await expect(secondCheckbox).toBeVisible();
  
  // Check the second option (this should not show a dropdown)
  await secondCheckbox.check();
  await expect(secondCheckbox).toBeChecked();

  // Verify the button text updates to show both selections
  await expect(wargearButton).toContainText('2/');

  // Dropdown should not appear for option 2 (simple option)
  const secondChoiceDropdown = page.getByTestId('wargear-choice-2');
  await expect(secondChoiceDropdown).not.toBeVisible();

  // Uncheck the first option
  await firstCheckbox.uncheck();
  await expect(firstCheckbox).not.toBeChecked();
  
  // Dropdown should disappear for first option
  await expect(choiceDropdown).not.toBeVisible();

  // Second option should still be checked
  await expect(secondCheckbox).toBeChecked();
  
  // Button text should show 1 selection
  await expect(wargearButton).toContainText('1/');
});

test('wargear selections persist after save and reload', async ({ page, request }) => {
  await page.goto(`/factions/${FACTION_ID}/armies/new`);
  await expect(page.getByTestId('unit-picker')).toBeVisible();

  // Set army name
  await page.getByTestId('army-name-input').fill('Test Army with Wargear');

  // Add an Overlord unit
  const unitPickerItem = page.getByTestId('unit-picker-item').filter({ hasText: 'Add Overlord (Characters) —' });
  await expect(unitPickerItem).toBeVisible();
  await unitPickerItem.getByTestId('add-unit-button').click();

  // Wait for the unit to be added
  await expect(page.getByTestId('unit-row')).toBeVisible();

  // Click wargear button to expand
  const wargearButton = page.getByTestId('wargear-toggle');
  await wargearButton.click();

  // Check the first option and select a choice
  const firstCheckbox = page.getByTestId('wargear-option-1');
  await firstCheckbox.check();
  
  // Wait a bit to ensure state is updated
  await page.waitForTimeout(100);
  
  // Check if checkbox is actually checked
  const isChecked = await firstCheckbox.isChecked();
  console.log('*** FIRST CHECKBOX CHECKED:', isChecked);
  
  // Select a choice from the dropdown
  const choiceDropdown = page.getByTestId('wargear-choice-1');
  await expect(choiceDropdown).toBeVisible();
  await choiceDropdown.selectOption('1 voidscythe');
  await expect(choiceDropdown).toHaveValue('1 voidscythe');

  // Save the army
  await page.getByTestId('save-army').click();
  
  // Check wargear button text before save
  const wargearTextBeforeSave = await page.getByTestId('wargear-toggle').textContent();
  console.log('Wargear button text before save:', wargearTextBeforeSave);
  
  // Wait for navigation to army detail page
  await expect(page).toHaveURL(/\/armies\/[a-f0-9-]+/);
  
  // Get the army ID to check saved data
  const currentUrl = page.url();
  const armyId = currentUrl.match(/\/armies\/([a-f0-9-]+)/)?.[1];
  if (armyId) {
    const armyRes = await request.get(`${API_BASE}/api/armies/${armyId}`);
    const army = await armyRes.json();
    console.log('*** SAVED ARMY UNITS:', JSON.stringify(army.army.units, null, 2));
  }

  // Click edit button to go back to army builder
  await page.getByRole('button', { name: 'Edit' }).click();

  // Wait for army builder to load
  await expect(page.getByTestId('builder-title')).toHaveText('Edit Army');

  // Verify the wargear selection persisted
  await expect(page.getByTestId('wargear-toggle')).toContainText('1/');
  await expect(page.getByTestId('wargear-toggle')).toBeVisible();

  // Expand wargear section
  await page.getByTestId('wargear-toggle').click();

  // Verify the checkbox is still checked and choice persists
  await expect(page.getByTestId('wargear-option-1')).toBeChecked();
  await expect(page.getByTestId('wargear-choice-1')).toHaveValue('1 voidscythe');
});
