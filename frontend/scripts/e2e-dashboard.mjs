import { chromium } from "playwright";

const password = "Password12345";
const suffix = Date.now();
const displayName = "Chef";

const viewports = [
  { name: "desktop-1440", width: 1440, height: 900 },
  { name: "mobile-375", width: 375, height: 812 },
];

const browser = await chromium.launch();

function log(vp, msg) {
  console.log(`[${vp.name}] ${msg}`);
}

async function register(page, tag) {
  await page.goto("http://localhost:5173/register", {
    waitUntil: "networkidle",
  });
  const email = `${tag}-${suffix}@test.local`;
  await page.fill("input[type=email]", email);
  await page.fill("input[type=password]", password);
  await page.fill('input[autocomplete="nickname"]', displayName);
  await page.click('button:has-text("Create account")');
  await page.waitForURL("http://localhost:5173/dashboard", { timeout: 15000 });
}

function iso(daysFromToday) {
  const d = new Date();
  d.setDate(d.getDate() + daysFromToday);
  return d.toISOString().slice(0, 10);
}

async function addPantryItem(page, isDesktop, item) {
  await page.goto("http://localhost:5173/pantry", { waitUntil: "networkidle" });
  await page.locator("main button", { hasText: "Add item" }).first().click();
  if (isDesktop) {
    await page.waitForSelector('[role="dialog"]');
  } else {
    await page.waitForURL("http://localhost:5173/pantry/new");
  }
  const container = isDesktop
    ? page.locator('[role="dialog"]')
    : page.locator("main");
  await container.locator('input[placeholder="e.g. Whole milk"]').fill(item.name);
  await container.locator('input[type="number"]').fill(String(item.qty));
  const unit = container.locator('[data-testid="unit-combobox"]');
  await unit.fill(item.unit);
  await unit.press("Tab");
  if (item.category) {
    const cat = container.locator('[data-testid="category-combobox"]');
    await cat.fill(item.category);
    await cat.press("Tab");
  }
  if (item.expiryDate) {
    await container.locator('input[type="date"]').fill(item.expiryDate);
  }
  await container.locator("button", { hasText: "Add item" }).click();
  if (isDesktop) {
    await page.waitForSelector('[role="dialog"]', {
      state: "detached",
      timeout: 8000,
    });
  } else {
    await page.waitForURL("http://localhost:5173/pantry", { timeout: 8000 });
  }
  await page.waitForSelector(`h3:has-text("${item.name}")`);
}

async function createRecipe(page, title) {
  await page.goto("http://localhost:5173/recipes", { waitUntil: "networkidle" });
  await page.locator("main button", { hasText: "Add recipe" }).first().click();
  await page.waitForURL("http://localhost:5173/recipes/new");
  await page
    .locator('input[placeholder="e.g. Weeknight tomato pasta"]')
    .fill(title);
  await page.locator("textarea#recipe-instructions").fill("Combine and cook.");
  await page.locator("button", { hasText: "Create recipe" }).click();
  await page.waitForURL(/\/recipes\/\d+$/, { timeout: 10000 });
}

async function createShoppingList(page, name) {
  await page.goto("http://localhost:5173/shopping-lists", {
    waitUntil: "networkidle",
  });
  await page.locator('[data-testid="new-shopping-list-button"]').click();
  await page.locator('[data-testid="shopping-list-name-input"]').fill(name);
  await page.locator('[data-testid="submit-shopping-list"]').click();
  await page.waitForURL(/\/shopping-lists\/\d+$/, { timeout: 10000 });
  const url = page.url();
  const match = /\/shopping-lists\/(\d+)$/.exec(url);
  return Number(match?.[1] ?? 0);
}

async function addShoppingListItem(page, item) {
  await page.locator('[data-testid="add-shopping-list-item-name"]').fill(item.name);
  await page
    .locator('[data-testid="add-shopping-list-item-quantity"]')
    .fill(String(item.quantity));
  if (item.unit) {
    const unitInput = page.locator('[data-testid="add-shopping-list-item-unit"]');
    await unitInput.fill(item.unit);
    await unitInput.press("Tab");
  }
  await page.locator('[data-testid="submit-shopping-list-item"]').click();
  await page.waitForSelector(
    `[data-testid^="shopping-list-item-"]:has-text("${item.name}")`,
  );
}

async function runFlow(vp) {
  const isDesktop = vp.width >= 768;
  const ctx = await browser.newContext({
    viewport: { width: vp.width, height: vp.height },
  });
  const page = await ctx.newPage();

  log(vp, "register");
  await register(page, `e2edash-${vp.name}`);

  log(vp, "seed pantry items (expiring + non-expiring)");
  const expiringItemName = "Whole milk";
  const expiringExpiryDate = iso(2);
  await addPantryItem(page, isDesktop, {
    name: expiringItemName,
    qty: 2,
    unit: "L",
    category: "Dairy",
    expiryDate: expiringExpiryDate,
  });
  await addPantryItem(page, isDesktop, {
    name: "Rice",
    qty: 5,
    unit: "kg",
    category: "Grains",
    expiryDate: null,
  });

  log(vp, "seed a recipe");
  await createRecipe(page, "Weeknight pasta");

  log(vp, "seed an active shopping list with items");
  const listName = "Weekend groceries";
  const listId = await createShoppingList(page, listName);
  await addShoppingListItem(page, {
    name: "Sourdough",
    quantity: 1,
    unit: "loaf",
  });
  await addShoppingListItem(page, {
    name: "Tomatoes",
    quantity: 500,
    unit: "g",
  });

  log(vp, "go to /dashboard");
  await page.goto("http://localhost:5173/dashboard", {
    waitUntil: "networkidle",
  });

  log(vp, "verify personalized greeting shows displayName");
  const heading = await page.locator("h1").first().textContent();
  if (!heading || !heading.includes(displayName)) {
    throw new Error(`expected greeting to include "${displayName}", got "${heading}"`);
  }

  log(vp, "verify stat tiles match seeded counts");
  await page.waitForSelector('[data-testid="dashboard-stat-pantry-value"]');
  const pantryStat = await page
    .locator('[data-testid="dashboard-stat-pantry-value"]')
    .textContent();
  const recipesStat = await page
    .locator('[data-testid="dashboard-stat-recipes-value"]')
    .textContent();
  const listsStat = await page
    .locator('[data-testid="dashboard-stat-shopping-lists-value"]')
    .textContent();
  if (pantryStat?.trim() !== "2") throw new Error(`pantry stat: expected 2, got ${pantryStat}`);
  if (recipesStat?.trim() !== "1") throw new Error(`recipes stat: expected 1, got ${recipesStat}`);
  if (listsStat?.trim() !== "1") throw new Error(`lists stat: expected 1, got ${listsStat}`);

  log(vp, "cross-check pantry count against /pantry list page");
  await page.goto("http://localhost:5173/pantry", { waitUntil: "networkidle" });
  const pantryRows = await page.locator('[data-testid^="pantry-item-"]').count();
  if (pantryRows !== 2)
    throw new Error(`pantry page shows ${pantryRows} items, expected 2`);

  log(vp, "cross-check recipe count against /recipes list page");
  await page.goto("http://localhost:5173/recipes", { waitUntil: "networkidle" });
  const recipeRows = await page.locator('main h3').count();
  if (recipeRows !== 1)
    throw new Error(`recipes page shows ${recipeRows} recipe titles, expected 1`);

  log(vp, "cross-check shopping list count against /shopping-lists list page");
  await page.goto("http://localhost:5173/shopping-lists", {
    waitUntil: "networkidle",
  });
  const listRows = await page
    .locator('[data-testid^="shopping-list-card-"]')
    .count();
  if (listRows !== 1)
    throw new Error(`shopping-lists page shows ${listRows} lists, expected 1`);

  log(vp, "back to /dashboard and click expiring card");
  await page.goto("http://localhost:5173/dashboard", {
    waitUntil: "networkidle",
  });
  const expiringCard = page.locator(
    `[data-testid^="dashboard-expiry-card-"]:has-text("${expiringItemName}")`,
  );
  await expiringCard.waitFor();
  await expiringCard.click();

  log(vp, "verify edit form opens pre-filled");
  if (isDesktop) {
    await page.waitForSelector('[role="dialog"] :text("Edit pantry item")');
    const nameInput = page.locator(
      '[role="dialog"] input[placeholder="e.g. Whole milk"]',
    );
    const nameVal = await nameInput.inputValue();
    if (nameVal !== expiringItemName)
      throw new Error(`edit form name: expected "${expiringItemName}", got "${nameVal}"`);
    const dateVal = await page
      .locator('[role="dialog"] input[type="date"]')
      .inputValue();
    if (dateVal !== expiringExpiryDate)
      throw new Error(
        `edit form date: expected "${expiringExpiryDate}", got "${dateVal}"`,
      );
    log(vp, "close edit form");
    await page.locator('[role="dialog"] button[aria-label="Close"]').click();
    await page.waitForSelector('[role="dialog"]', { state: "detached" });
  } else {
    await page.waitForURL(/\/pantry\/\d+\/edit/, { timeout: 5000 });
    await page.waitForSelector('h1:has-text("Edit pantry item")');
    const nameInput = page.locator('input[placeholder="e.g. Whole milk"]');
    const nameVal = await nameInput.inputValue();
    if (nameVal !== expiringItemName)
      throw new Error(`edit form name: expected "${expiringItemName}", got "${nameVal}"`);
    log(vp, "back to dashboard");
    await page.goto("http://localhost:5173/dashboard", {
      waitUntil: "networkidle",
    });
  }

  log(vp, "click 'Add pantry item' quick action");
  await page.locator('[data-testid="dashboard-quick-add-pantry-item"]').click();
  if (isDesktop) {
    await page.waitForSelector('[role="dialog"] :text("Add pantry item")');
    const nameVal = await page
      .locator('[role="dialog"] input[placeholder="e.g. Whole milk"]')
      .inputValue();
    if (nameVal !== "")
      throw new Error(`expected empty create form, got name="${nameVal}"`);
    log(vp, "close create form");
    await page.locator('[role="dialog"] button[aria-label="Close"]').click();
    await page.waitForSelector('[role="dialog"]', { state: "detached" });
  } else {
    await page.waitForURL("http://localhost:5173/pantry/new", { timeout: 5000 });
    await page.waitForSelector('h1:has-text("Add pantry item")');
    await page.goto("http://localhost:5173/dashboard", {
      waitUntil: "networkidle",
    });
  }

  log(vp, "click into the active shopping list card");
  await page
    .locator(`[data-testid="dashboard-shopping-list-card-${listId}"]`)
    .click();
  await page.waitForURL(`http://localhost:5173/shopping-lists/${listId}`, {
    timeout: 5000,
  });
  await page.waitForSelector(
    `[data-testid="shopping-list-name"]:has-text("${listName}")`,
  );

  log(vp, "PASS");
  await ctx.close();
}

let failed = false;
for (const vp of viewports) {
  try {
    await runFlow(vp);
  } catch (err) {
    console.error(`[${vp.name}] FAIL:`, err.message);
    failed = true;
  }
}

await browser.close();
if (failed) process.exit(1);
console.log("\nAll dashboard e2e flows passed.");
