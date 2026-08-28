import { chromium } from "playwright";

const password = "Password12345";
const suffix = Date.now();

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
  await page.fill('input[autocomplete="nickname"]', "Shopper");
  await page.click('button:has-text("Create account")');
  await page.waitForURL("http://localhost:5173/dashboard", { timeout: 15000 });
}

async function fillRecipeForm(page, r) {
  await page
    .locator('input[placeholder="e.g. Weeknight tomato pasta"]')
    .fill(r.title);
  await page.locator("textarea#recipe-instructions").fill(r.instructions);
  if (r.cookTime) {
    const h = Math.floor(r.cookTime / 60);
    const m = r.cookTime % 60;
    if (h) await page.locator('[data-testid="cook-time-hours"]').fill(String(h));
    if (m) await page.locator('[data-testid="cook-time-minutes"]').fill(String(m));
  }
  for (const tag of r.tags ?? []) {
    const t = page.locator('[data-testid="tag-input"]');
    await t.fill(tag);
    await t.press("Enter");
  }
}

async function createRecipe(page, r) {
  await page.locator("main button", { hasText: "Add recipe" }).first().click();
  await page.waitForURL("http://localhost:5173/recipes/new");
  await fillRecipeForm(page, r);
  await page.locator("button", { hasText: "Create recipe" }).click();
  await page.waitForURL(/\/recipes\/\d+$/, { timeout: 10000 });
  await page.waitForSelector('[data-testid="recipe-title"]');
}

async function addIngredient(page, ing) {
  await page.locator('[data-testid="add-ingredient-button"]').click();
  await page.waitForSelector('input[placeholder="e.g. Flour"]');
  await page.locator('input[placeholder="e.g. Flour"]').fill(ing.name);
  await page.locator('input[type="number"]').first().fill(String(ing.quantity));
  if (ing.unit) {
    const unitCombobox = page.locator('[data-testid="ingredient-unit-combobox"]');
    await unitCombobox.fill(ing.unit);
    await unitCombobox.press("Tab");
  }
  await page.locator('[data-testid="submit-ingredient"]').click();
  await page.waitForSelector(`li:has-text("${ing.name}")`);
}

async function createShoppingListManually(page, name) {
  await page.locator('[data-testid="new-shopping-list-button"]').click();
  await page.waitForSelector('[data-testid="shopping-list-name-input"]');
  await page.locator('[data-testid="shopping-list-name-input"]').fill(name);
  await page.locator('[data-testid="submit-shopping-list"]').click();
  await page.waitForURL(/\/shopping-lists\/\d+$/, { timeout: 10000 });
  await page.waitForSelector('[data-testid="shopping-list-name"]');
}

async function addShoppingListItem(page, item) {
  await page
    .locator('[data-testid="add-shopping-list-item-name"]')
    .fill(item.name);
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
  const ctx = await browser.newContext({
    viewport: { width: vp.width, height: vp.height },
  });
  const page = await ctx.newPage();

  log(vp, "register");
  await register(page, `e2esl-${vp.name}`);

  log(vp, "create a recipe with ingredients");
  await page.goto("http://localhost:5173/recipes", { waitUntil: "networkidle" });
  await page.waitForSelector("text=No recipes yet");
  const recipe = {
    title: "Pasta with sauce",
    instructions: "Boil pasta.\nSimmer sauce.\nCombine and serve.",
    cookTime: 25,
    tags: ["dinner"],
  };
  await createRecipe(page, recipe);
  const ingredients = [
    { name: "Spaghetti", quantity: 400, unit: "g" },
    { name: "Tomato passata", quantity: 500, unit: "ml" },
    { name: "Garlic cloves", quantity: 3, unit: "pcs" },
  ];
  for (const ing of ingredients) {
    await addIngredient(page, ing);
  }

  log(vp, "generate shopping list from recipe");
  await page.locator('[data-testid="generate-shopping-list-button"]').click();
  await page.waitForURL(/\/shopping-lists\/\d+$/, { timeout: 10000 });
  await page.waitForSelector('[data-testid="shopping-list-name"]');

  log(vp, "verify all ingredients appear as items");
  for (const ing of ingredients) {
    const count = await page
      .locator(
        `[data-testid^="shopping-list-item-"]:has-text("${ing.name}")`,
      )
      .count();
    if (count === 0) throw new Error(`ingredient ${ing.name} missing from generated list`);
  }

  log(vp, "check off two items");
  const toCheck = ["Spaghetti", "Garlic cloves"];
  for (const name of toCheck) {
    const row = page.locator(
      `[data-testid^="shopping-list-item-"]:has-text("${name}")`,
    );
    await row.locator('input[type="checkbox"]').click();
  }
  await page.waitForFunction(
    (names) => {
      const rows = document.querySelectorAll(
        '[data-testid^="shopping-list-item-"]',
      );
      const checkedNames = new Set();
      for (const el of rows) {
        if (el.getAttribute("data-checked") === "true") {
          checkedNames.add(el.textContent);
        }
      }
      return names.every((n) =>
        Array.from(checkedNames).some((t) => t?.includes(n)),
      );
    },
    toCheck,
    { timeout: 5000 },
  );

  log(vp, "verify checked heading appears");
  await page.waitForSelector('h2:has-text("Checked (2)")');

  log(vp, "add a manual item via inline row");
  await addShoppingListItem(page, {
    name: "Parmesan",
    quantity: 100,
    unit: "g",
  });

  log(vp, "rename the list inline");
  await page.locator('[data-testid="shopping-list-name"]').click();
  const input = page.locator('[data-testid="shopping-list-name-edit"]');
  await input.fill("Tonight's dinner run");
  await input.press("Enter");
  await page.waitForSelector(
    '[data-testid="shopping-list-name"]:has-text("Tonight\'s dinner run")',
  );

  log(vp, "delete one item");
  const removeTargetSelector =
    '[data-testid^="shopping-list-item-"]:has-text("Tomato passata")';
  const row = page.locator(removeTargetSelector);
  await row.locator('button[aria-label^="Delete"]').click();
  await row.locator("button", { hasText: "Remove" }).click();
  await page.waitForSelector(removeTargetSelector, { state: "detached" });

  log(vp, "delete the whole list");
  await page.locator('[data-testid="delete-shopping-list-button"]').click();
  await page.locator('[data-testid="confirm-delete-shopping-list"]').click();
  await page.waitForURL("http://localhost:5173/shopping-lists", {
    timeout: 10000,
  });

  log(vp, "verify redirect + list gone");
  const remaining = await page
    .locator('[data-testid^="shopping-list-card-"]')
    .count();
  if (remaining !== 0) throw new Error(`expected 0 lists after delete, found ${remaining}`);
  await page.waitForSelector("text=No shopping lists yet");

  log(vp, "create a list manually via New list flow");
  await createShoppingListManually(page, "Weekly grocery run");
  await page.goto("http://localhost:5173/shopping-lists", {
    waitUntil: "networkidle",
  });
  await page.waitForSelector("text=Weekly grocery run");

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
console.log("\nAll shopping list e2e flows passed.");
