import { chromium } from "playwright";

const password = "Password12345";
const suffix = Date.now();

const browser = await chromium.launch();

function log(section, msg) {
  console.log(`[${section}] ${msg}`);
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

async function pantryFlow() {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await register(page, "e2eds-pantry");

  log("pantry", "create item via combobox suggestion");
  await page.goto("http://localhost:5173/pantry", { waitUntil: "networkidle" });
  await page.locator("main button", { hasText: "Add item" }).first().click();
  await page.waitForSelector('[role="dialog"]');
  const dialog = page.locator('[role="dialog"]');
  await dialog.locator('input[placeholder="e.g. Whole milk"]').fill("Whole milk");
  await dialog.locator('input[type="number"]').fill("2");
  const unitCombobox = dialog.locator('[data-testid="unit-combobox"]');
  await unitCombobox.click();
  // Suggestions should appear; pick "l" from seeded list by clicking
  await page.waitForSelector('[role="option"]:has-text("l")');
  await page.locator('[role="option"]', { hasText: /^l$/ }).first().click();
  const unitValue = await unitCombobox.inputValue();
  if (unitValue !== "l") throw new Error(`expected unit "l", got "${unitValue}"`);
  const categoryCombobox = dialog.locator('[data-testid="category-combobox"]');
  await categoryCombobox.click();
  await page.waitForSelector('[role="option"]:has-text("dairy")');
  await page.locator('[role="option"]', { hasText: /^dairy$/ }).first().click();
  await dialog.locator('button:has-text("Add item")').click();
  await page.waitForSelector('[role="dialog"]', { state: "detached" });
  await page.waitForSelector('h3:has-text("Whole milk")');
  const line = await page.locator('[data-testid^="pantry-item-"]:has-text("Whole milk")').textContent();
  if (!line.includes("2 l")) throw new Error(`expected "2 l" in card, got "${line}"`);
  if (!line.toLowerCase().includes("dairy")) throw new Error(`expected "dairy" in card, got "${line}"`);
  log("pantry", "PASS combobox suggestion path");

  log("pantry", "create item with novel typed unit");
  await page.locator("main button", { hasText: "Add item" }).first().click();
  await page.waitForSelector('[role="dialog"]');
  await dialog.locator('input[placeholder="e.g. Whole milk"]').fill("Salt");
  await dialog.locator('input[type="number"]').fill("1");
  await unitCombobox.click();
  await unitCombobox.fill("shakes"); // novel unit not in seed list
  await unitCombobox.press("Escape");
  await categoryCombobox.click();
  await categoryCombobox.fill("pantry"); // novel category
  await categoryCombobox.press("Escape");
  await dialog.locator('button:has-text("Add item")').click();
  await page.waitForSelector('[role="dialog"]', { state: "detached" });
  await page.waitForSelector('h3:has-text("Salt")');
  const line2 = await page.locator('[data-testid^="pantry-item-"]:has-text("Salt")').textContent();
  if (!line2.includes("1 shakes")) throw new Error(`expected "1 shakes" in card, got "${line2}"`);
  if (!line2.toLowerCase().includes("pantry")) throw new Error(`expected "pantry" in card, got "${line2}"`);
  log("pantry", "PASS novel-value path");

  await ctx.close();
}

async function recipeFlow() {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await register(page, "e2eds-recipe");

  log("recipe", "create with hours+minutes and two tags");
  await page.goto("http://localhost:5173/recipes", { waitUntil: "networkidle" });
  await page.locator("main button", { hasText: "Add recipe" }).first().click();
  await page.waitForURL("http://localhost:5173/recipes/new");
  await page.locator('input[placeholder="e.g. Weeknight tomato pasta"]').fill("Slow braise");
  await page.locator("textarea#recipe-instructions").fill("Season.\nBrown.\nBraise low for a long time.");
  await page.locator('[data-testid="cook-time-hours"]').fill("2");
  await page.locator('[data-testid="cook-time-minutes"]').fill("15");
  // Two tags: one via type+Enter, one via clicking suggestion
  const tagInput = page.locator('[data-testid="tag-input"]');
  await tagInput.fill("comfort");
  await tagInput.press("Enter");
  await tagInput.click();
  await page.waitForSelector('[role="option"]:has-text("dinner")');
  await page.locator('[role="option"]', { hasText: /^dinner$/ }).first().click();
  await page.locator('button:has-text("Create recipe")').click();
  await page.waitForURL(/\/recipes\/\d+$/);
  await page.waitForSelector('[data-testid="recipe-title"]:has-text("Slow braise")');

  const cookTimeText = await page.locator('main').textContent();
  // Detail shows raw minutes; 2h15 = 135 min
  if (!cookTimeText.includes("135 min")) {
    throw new Error(`expected "135 min" in detail page, got body without it`);
  }
  // Both chips visible
  const comfortCount = await page.locator('span:has-text("comfort")').count();
  const dinnerCount = await page.locator('span:has-text("dinner")').count();
  if (comfortCount === 0) throw new Error(`comfort tag chip not visible`);
  if (dinnerCount === 0) throw new Error(`dinner tag chip not visible`);
  log("recipe", "PASS hours+minutes and tags");

  await ctx.close();
}

let failed = false;
try { await pantryFlow(); } catch (err) { console.error("pantryFlow FAIL:", err.message); failed = true; }
try { await recipeFlow(); } catch (err) { console.error("recipeFlow FAIL:", err.message); failed = true; }

await browser.close();
if (failed) process.exit(1);
console.log("\nDesign-system E2E flows passed.");
