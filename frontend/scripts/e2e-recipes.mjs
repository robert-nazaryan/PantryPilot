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

async function assertText(page, selector, expected, label) {
  const text = (await page.locator(selector).first().textContent())?.trim() ?? "";
  if (!text.includes(expected)) {
    throw new Error(`${label}: expected "${expected}" in "${text}"`);
  }
}

async function register(page, tag) {
  await page.goto("http://localhost:5173/register", { waitUntil: "networkidle" });
  const email = `${tag}-${suffix}@test.local`;
  await page.fill("input[type=email]", email);
  await page.fill("input[type=password]", password);
  await page.fill('input[autocomplete="nickname"]', "Chef");
  await page.click('button:has-text("Create account")');
  await page.waitForURL("http://localhost:5173/dashboard", { timeout: 15000 });
  return email;
}

async function fillRecipeForm(page, r) {
  await page.locator('input[placeholder="e.g. Weeknight tomato pasta"]').fill(r.title);
  await page.locator("textarea#recipe-instructions").fill(r.instructions);
  if (r.cookTime) await page.locator('input[placeholder="e.g. 30"]').fill(String(r.cookTime));
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
  if (ing.unit) await page.locator('input[placeholder="e.g. g, cups"]').fill(ing.unit);
  await page.locator('[data-testid="submit-ingredient"]').click();
  await page.waitForSelector(`li:has-text("${ing.name}")`);
}

async function editRecipeTitle(page, newTitle) {
  await page.locator('[data-testid="edit-recipe-button"]').click();
  await page.waitForURL(/\/recipes\/\d+\/edit$/);
  await page.locator('input[placeholder="e.g. Weeknight tomato pasta"]').fill(newTitle);
  await page.locator("button", { hasText: "Save changes" }).click();
  await page.waitForURL(/\/recipes\/\d+$/);
  await page.waitForSelector(`[data-testid="recipe-title"]:has-text("${newTitle}")`);
}

async function runFlow(vp) {
  const ctx = await browser.newContext({ viewport: { width: vp.width, height: vp.height } });
  const page = await ctx.newPage();
  log(vp, `register`);
  await register(page, `e2erec-${vp.name}`);

  log(vp, `nav to /recipes`);
  await page.goto("http://localhost:5173/recipes", { waitUntil: "networkidle" });
  await page.waitForSelector("text=No recipes yet");

  const recipe = {
    title: "Weeknight tomato pasta",
    instructions:
      "Boil pasta.\nSimmer tomatoes with garlic.\nToss together and serve.",
    cookTime: 25,
    tags: ["dinner", "quick"],
  };

  log(vp, `create recipe`);
  await createRecipe(page, recipe);
  await assertText(page, '[data-testid="recipe-title"]', recipe.title, "recipe title");

  log(vp, `add ingredients`);
  const ingredients = [
    { name: "Pasta", quantity: 200, unit: "g" },
    { name: "Crushed tomatoes", quantity: 400, unit: "g" },
    { name: "Garlic clove", quantity: 2, unit: "" },
  ];
  for (const ing of ingredients) {
    await addIngredient(page, ing);
  }
  for (const ing of ingredients) {
    const found = await page.locator(`li:has-text("${ing.name}")`).count();
    if (found === 0) throw new Error(`ingredient ${ing.name} not visible`);
  }

  log(vp, `edit ingredient`);
  const firstRow = page.locator('[data-testid^="ingredient-row-"]').first();
  await firstRow.locator('button[aria-label^="Edit"]').click();
  await firstRow.locator('input').first().fill("Spaghetti");
  await firstRow.locator("button", { hasText: "Save" }).click();
  await page.waitForSelector('li:has-text("Spaghetti")');

  log(vp, `delete ingredient`);
  const secondRow = page.locator('[data-testid^="ingredient-row-"]:has-text("Crushed tomatoes")');
  await secondRow.locator('button[aria-label^="Delete"]').click();
  await secondRow.locator("button", { hasText: "Remove" }).click();
  await page.waitForSelector('li:has-text("Crushed tomatoes")', { state: "detached" });

  log(vp, `edit recipe title`);
  const newTitle = "Weeknight tomato pasta (v2)";
  await editRecipeTitle(page, newTitle);

  log(vp, `nav back to list, verify updated title`);
  await page.goto("http://localhost:5173/recipes", { waitUntil: "networkidle" });
  await page.waitForSelector(`text=${newTitle}`);

  log(vp, `open recipe and delete`);
  await page.locator(`a:has-text("${newTitle}")`).click();
  await page.waitForURL(/\/recipes\/\d+$/);
  await page.locator('[data-testid="delete-recipe-button"]').click();
  await page.locator('[data-testid="confirm-delete-recipe"]').click();
  await page.waitForURL("http://localhost:5173/recipes", { timeout: 10000 });
  await page.waitForSelector("text=No recipes yet");
  log(vp, `PASS`);

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
console.log("\nAll recipe e2e flows passed.");
