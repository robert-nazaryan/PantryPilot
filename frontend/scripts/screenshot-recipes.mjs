import { chromium } from "playwright";
import { mkdir } from "node:fs/promises";
import { resolve } from "node:path";

const outDir = resolve("screenshots");
await mkdir(outDir, { recursive: true });

const password = "Password12345";
const suffix = Date.now();

const themes = ["light", "dark"];
const viewports = [
  { name: "mobile-375", width: 375, height: 812 },
  { name: "desktop-1440", width: 1440, height: 900 },
];

const browser = await chromium.launch();

async function newCtx(vp, theme) {
  const ctx = await browser.newContext({ viewport: { width: vp.width, height: vp.height } });
  await ctx.addInitScript((t) => {
    try { localStorage.setItem("pantrypilot-theme", t); } catch {}
  }, theme);
  return ctx;
}

async function register(page, tag) {
  await page.goto("http://localhost:5173/register", { waitUntil: "networkidle" });
  const email = `${tag}-${suffix}@test.local`;
  await page.fill("input[type=email]", email);
  await page.fill("input[type=password]", password);
  await page.fill('input[autocomplete="nickname"]', "Chef");
  await page.click('button:has-text("Create account")');
  await page.waitForURL("http://localhost:5173/dashboard", { timeout: 15000 });
}

async function shot(page, name, opts = {}) {
  const path = resolve(outDir, `${name}.png`);
  await page.screenshot({ path, fullPage: opts.fullPage ?? true });
  console.log(`  saved ${name}`);
}

async function fillForm(page, r) {
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
  await fillForm(page, r);
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

const sampleRecipe = {
  title: "Weeknight tomato pasta",
  instructions:
    "Boil pasta in salted water.\nMeanwhile, simmer crushed tomatoes with garlic and olive oil.\nToss pasta with sauce and finish with basil.",
  cookTime: 25,
  tags: ["dinner", "quick", "vegetarian"],
};

const secondRecipe = {
  title: "Overnight oats",
  instructions: "Combine rolled oats, milk, and honey.\nChill overnight.\nTop with berries.",
  cookTime: 5,
  tags: ["breakfast"],
};

const sampleIngredients = [
  { name: "Pasta", quantity: 200, unit: "g" },
  { name: "Crushed tomatoes", quantity: 400, unit: "g" },
  { name: "Garlic clove", quantity: 2, unit: "" },
];

for (const theme of themes) {
  for (const vp of viewports) {
    console.log(`\n=== recipes ${theme} / ${vp.name} ===`);

    // Empty state
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await register(page, `rempty-${theme}-${vp.name}`);
      await page.goto("http://localhost:5173/recipes", { waitUntil: "networkidle" });
      await page.waitForSelector("text=No recipes yet");
      await shot(page, `recipes-empty-${theme}-${vp.name}`);
      await ctx.close();
    }

    // Add recipe page (fullPage AND viewport-only to confirm buttons are visible without scrolling)
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await register(page, `radd-${theme}-${vp.name}`);
      await page.goto("http://localhost:5173/recipes", { waitUntil: "networkidle" });
      await page.waitForSelector("text=No recipes yet");
      await page.locator("main button", { hasText: "Add recipe" }).first().click();
      await page.waitForURL("http://localhost:5173/recipes/new");
      await fillForm(page, sampleRecipe);
      // full page for reference
      await shot(page, `recipes-add-page-${theme}-${vp.name}`);
      // viewport-only: proves the sticky footer keeps Cancel/Create visible without scrolling
      await shot(page, `recipes-add-page-viewport-${theme}-${vp.name}`, { fullPage: false });
      await ctx.close();
    }

    // List + detail (with ingredients)
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await register(page, `rlist-${theme}-${vp.name}`);
      await page.goto("http://localhost:5173/recipes", { waitUntil: "networkidle" });
      await createRecipe(page, sampleRecipe);
      for (const ing of sampleIngredients) {
        await addIngredient(page, ing);
      }
      await shot(page, `recipe-detail-${theme}-${vp.name}`);

      await page.goto("http://localhost:5173/recipes", { waitUntil: "networkidle" });
      await createRecipe(page, secondRecipe);
      await page.goto("http://localhost:5173/recipes", { waitUntil: "networkidle" });
      await page.waitForSelector(`text=${sampleRecipe.title}`);
      await shot(page, `recipes-list-${theme}-${vp.name}`);
      await ctx.close();
    }
  }
}

await browser.close();
console.log("\nRecipe screenshots saved.");
