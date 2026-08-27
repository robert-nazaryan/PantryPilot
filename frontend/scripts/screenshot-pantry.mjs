import { chromium } from "playwright";
import { mkdir } from "node:fs/promises";
import { resolve } from "node:path";

const outDir = resolve("screenshots");
await mkdir(outDir, { recursive: true });

const suffix = Date.now();

async function register(page, viewport) {
  await page.goto("http://localhost:5173/register", { waitUntil: "networkidle" });
  await page.fill("input[type=email]", `pantry-${viewport}-${suffix}@test.local`);
  await page.fill("input[type=password]", "Password12345");
  await page.fill('input[autocomplete="nickname"]', "Chef");
  await page.click('button:has-text("Create account")');
  await page.waitForURL("http://localhost:5173/pantry", { timeout: 10000 });
  await page.waitForSelector("h1:has-text('Your pantry')");
}

async function addItem(page, it, isDesktop) {
  // Trigger: outside the dialog, inside main
  await page.locator("main button", { hasText: "Add item" }).first().click();

  if (isDesktop) {
    await page.waitForSelector('[role="dialog"]');
  } else {
    await page.waitForURL("http://localhost:5173/pantry/new");
  }

  const container = isDesktop ? page.locator('[role="dialog"]') : page.locator("main");
  await container.locator('input[placeholder="e.g. Whole milk"]').fill(it.name);
  await container.locator('input[type="number"]').fill(it.qty);
  await container.locator('input[placeholder="e.g. L, kg, cans"]').fill(it.unit);
  await container.locator('input[placeholder="e.g. Dairy"]').fill(it.category);
  if (it.days !== null && it.days >= 0) {
    const d = new Date();
    d.setDate(d.getDate() + it.days);
    await container.locator('input[type="date"]').fill(d.toISOString().slice(0, 10));
  }
  // Submit: only the form's Add item button
  await container.locator("button", { hasText: "Add item" }).click();

  if (isDesktop) {
    await page.waitForSelector('[role="dialog"]', { state: "detached", timeout: 8000 });
  } else {
    await page.waitForURL("http://localhost:5173/pantry", { timeout: 8000 });
  }
  await page.waitForSelector(`h3:has-text("${it.name}")`);
}

const seedList = [
  { name: "Whole milk", qty: "2", unit: "L", category: "Dairy", days: 3 },
  { name: "Rice", qty: "5", unit: "kg", category: "Grains", days: null },
  { name: "Yogurt", qty: "1", unit: "cup", category: "Dairy", days: 20 },
];

const browser = await chromium.launch();

// Empty state — mobile
{
  const ctx = await browser.newContext({ viewport: { width: 375, height: 812 } });
  const page = await ctx.newPage();
  await register(page, "empty-m");
  await page.waitForSelector("text=Your pantry is empty");
  await page.screenshot({ path: resolve(outDir, "pantry-empty-mobile-375.png"), fullPage: true });
  console.log("saved pantry-empty-mobile-375.png");
  await ctx.close();
}

// Empty state — desktop
{
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await register(page, "empty-d");
  await page.waitForSelector("text=Your pantry is empty");
  await page.screenshot({ path: resolve(outDir, "pantry-empty-desktop-1440.png"), fullPage: true });
  console.log("saved pantry-empty-desktop-1440.png");
  await ctx.close();
}

// List + modal — desktop
{
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await register(page, "list-d");
  for (const it of seedList) await addItem(page, it, true);
  await page.screenshot({ path: resolve(outDir, "pantry-list-desktop-1440.png"), fullPage: true });
  console.log("saved pantry-list-desktop-1440.png");
  await page.locator("main button", { hasText: "Add item" }).first().click();
  await page.waitForSelector('[role="dialog"]');
  await page.screenshot({ path: resolve(outDir, "pantry-add-modal-desktop-1440.png"), fullPage: true });
  console.log("saved pantry-add-modal-desktop-1440.png");
  await ctx.close();
}

// List + full page add — mobile
{
  const ctx = await browser.newContext({ viewport: { width: 375, height: 812 } });
  const page = await ctx.newPage();
  await register(page, "list-m");
  for (const it of seedList) await addItem(page, it, false);
  await page.screenshot({ path: resolve(outDir, "pantry-list-mobile-375.png"), fullPage: true });
  console.log("saved pantry-list-mobile-375.png");
  await page.locator("main button", { hasText: "Add item" }).first().click();
  await page.waitForURL("http://localhost:5173/pantry/new");
  await page.waitForSelector('input[placeholder="e.g. Whole milk"]');
  await page.screenshot({ path: resolve(outDir, "pantry-add-page-mobile-375.png"), fullPage: true });
  console.log("saved pantry-add-page-mobile-375.png");
  await ctx.close();
}

await browser.close();
