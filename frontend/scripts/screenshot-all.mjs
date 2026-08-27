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
  await page.waitForURL("http://localhost:5173/dashboard", { timeout: 10000 });
  await page.waitForSelector("h1:has-text('Welcome back')");
}

async function addItem(page, it, isDesktop) {
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
    const d = new Date(); d.setDate(d.getDate() + it.days);
    await container.locator('input[type="date"]').fill(d.toISOString().slice(0, 10));
  }
  await container.locator("button", { hasText: "Add item" }).click();
  if (isDesktop) {
    await page.waitForSelector('[role="dialog"]', { state: "detached", timeout: 8000 });
  } else {
    await page.waitForURL("http://localhost:5173/pantry", { timeout: 8000 });
  }
  await page.waitForSelector(`h3:has-text("${it.name}")`);
}

const seed = [
  { name: "Whole milk", qty: "2", unit: "L", category: "Dairy", days: 3 },
  { name: "Rice", qty: "5", unit: "kg", category: "Grains", days: null },
  { name: "Yogurt", qty: "1", unit: "cup", category: "Dairy", days: 20 },
];

async function shot(page, name) {
  const path = resolve(outDir, `${name}.png`);
  await page.screenshot({ path, fullPage: true });
  console.log(`  saved ${name}`);
}

for (const theme of themes) {
  for (const vp of viewports) {
    console.log(`\n=== ${theme} / ${vp.name} ===`);
    const isDesktop = vp.width >= 768;

    // Login (unauthenticated)
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await page.goto("http://localhost:5173/login", { waitUntil: "networkidle" });
      await shot(page, `login-${theme}-${vp.name}`);
      await ctx.close();
    }
    // Register
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await page.goto("http://localhost:5173/register", { waitUntil: "networkidle" });
      await shot(page, `register-${theme}-${vp.name}`);
      await ctx.close();
    }
    // Dashboard (authenticated, fresh)
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await register(page, `dash-${theme}-${vp.name}`);
      await shot(page, `dashboard-${theme}-${vp.name}`);
      await ctx.close();
    }
    // Pantry empty
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await register(page, `pempty-${theme}-${vp.name}`);
      await page.goto("http://localhost:5173/pantry", { waitUntil: "networkidle" });
      await page.waitForSelector("text=Your pantry is empty");
      await shot(page, `pantry-empty-${theme}-${vp.name}`);
      await ctx.close();
    }
    // Pantry list with items + add form (modal desktop / page mobile)
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await register(page, `plist-${theme}-${vp.name}`);
      await page.goto("http://localhost:5173/pantry", { waitUntil: "networkidle" });
      for (const it of seed) await addItem(page, it, isDesktop);
      await shot(page, `pantry-list-${theme}-${vp.name}`);
      await page.locator("main button", { hasText: "Add item" }).first().click();
      if (isDesktop) {
        await page.waitForSelector('[role="dialog"]');
        await shot(page, `pantry-add-modal-${theme}-${vp.name}`);
      } else {
        await page.waitForURL("http://localhost:5173/pantry/new");
        await page.waitForSelector('input[placeholder="e.g. Whole milk"]');
        await shot(page, `pantry-add-page-${theme}-${vp.name}`);
      }
      await ctx.close();
    }
  }
}

await browser.close();
console.log("\nAll screenshots saved.");
