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

for (const theme of themes) {
  for (const vp of viewports) {
    console.log(`\n=== design-system ${theme} / ${vp.name} ===`);
    const isDesktop = vp.width >= 768;

    // Pantry add form with combobox open on unit
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await register(page, `dspantry-${theme}-${vp.name}`);
      await page.goto("http://localhost:5173/pantry", { waitUntil: "networkidle" });
      await page.locator("main button", { hasText: "Add item" }).first().click();
      const container = isDesktop
        ? (await page.waitForSelector('[role="dialog"]'), page.locator('[role="dialog"]'))
        : (await page.waitForURL("http://localhost:5173/pantry/new"), page.locator("main"));
      // Pre-fill some fields so the form isn't blank
      await container.locator('input[placeholder="e.g. Whole milk"]').fill("Whole milk");
      await container.locator('input[type="number"]').fill("2");
      // Open the unit combobox dropdown for the screenshot
      await container.locator('[data-testid="unit-combobox"]').click();
      await page.waitForSelector('[role="option"]:has-text("l")');
      await shot(page, `pantry-add-form-unit-open-${theme}-${vp.name}`, { fullPage: false });
      await ctx.close();
    }

    // Pantry add form with category combobox open
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await register(page, `dspantrycat-${theme}-${vp.name}`);
      await page.goto("http://localhost:5173/pantry", { waitUntil: "networkidle" });
      await page.locator("main button", { hasText: "Add item" }).first().click();
      const container = isDesktop
        ? (await page.waitForSelector('[role="dialog"]'), page.locator('[role="dialog"]'))
        : (await page.waitForURL("http://localhost:5173/pantry/new"), page.locator("main"));
      await container.locator('input[placeholder="e.g. Whole milk"]').fill("Whole milk");
      await container.locator('input[type="number"]').fill("2");
      const unit = container.locator('[data-testid="unit-combobox"]');
      await unit.fill("l");
      await unit.press("Tab");
      await container.locator('[data-testid="category-combobox"]').click();
      await page.waitForSelector('[role="option"]:has-text("dairy")');
      await shot(page, `pantry-add-form-category-open-${theme}-${vp.name}`, { fullPage: false });
      await ctx.close();
    }

    // Recipe add form: hours+minutes filled, two tags
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await register(page, `dsrecipe-${theme}-${vp.name}`);
      await page.goto("http://localhost:5173/recipes", { waitUntil: "networkidle" });
      await page.locator("main button", { hasText: "Add recipe" }).first().click();
      await page.waitForURL("http://localhost:5173/recipes/new");
      await page.locator('input[placeholder="e.g. Weeknight tomato pasta"]').fill("Slow braise");
      await page.locator("textarea#recipe-instructions").fill("Season.\nBrown.\nBraise low for a long time.");
      await page.locator('[data-testid="cook-time-hours"]').fill("2");
      await page.locator('[data-testid="cook-time-minutes"]').fill("15");
      const tagInput = page.locator('[data-testid="tag-input"]');
      await tagInput.fill("comfort");
      await tagInput.press("Enter");
      await tagInput.fill("dinner");
      await tagInput.press("Enter");
      await shot(page, `recipe-add-form-${theme}-${vp.name}`, { fullPage: false });
      // Also with tag suggestions open
      await tagInput.click();
      await page.waitForSelector('[role="option"]');
      await shot(page, `recipe-add-form-tags-open-${theme}-${vp.name}`, { fullPage: false });
      await ctx.close();
    }
  }
}

await browser.close();
console.log("\nDesign-system screenshots saved.");
