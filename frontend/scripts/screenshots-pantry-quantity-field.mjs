import { chromium } from "playwright";
import { mkdirSync } from "node:fs";
import { join } from "node:path";

const password = "Password12345";
const OUT_DIR = "screenshots";
mkdirSync(OUT_DIR, { recursive: true });

const viewports = [
  { name: "mobile-375", width: 375, height: 812 },
  { name: "desktop-1440", width: 1440, height: 900 },
];
const themes = ["light", "dark"];

async function register(page, tag, suffix) {
  const email = `${tag}-${suffix}@test.local`;
  await page.goto("http://localhost:5173/register", { waitUntil: "networkidle" });
  await page.fill("input[type=email]", email);
  await page.fill("input[type=password]", password);
  await page.fill('input[autocomplete="nickname"]', "Chef");
  await page.click('button:has-text("Create account")');
  await page.waitForURL("http://localhost:5173/dashboard", { timeout: 15000 });
}

async function ensureTheme(page, theme) {
  const isDark = await page.evaluate(() => document.documentElement.classList.contains("dark"));
  if ((theme === "dark") !== isDark) {
    const toggle = page.locator('button[aria-label*="dark mode" i], button[aria-label*="theme" i]').first();
    if (await toggle.count()) {
      await toggle.click();
    } else {
      await page.evaluate((wantDark) => {
        document.documentElement.classList.toggle("dark", wantDark);
        localStorage.setItem("pantrypilot-theme", wantDark ? "dark" : "light");
      }, theme === "dark");
    }
  }
}

async function openAddForm(page, isDesktop) {
  await page.goto("http://localhost:5173/pantry", { waitUntil: "networkidle" });
  await page.locator("main button", { hasText: "Add item" }).first().click();
  if (isDesktop) {
    await page.waitForSelector('[role="dialog"]', { timeout: 5000 });
    return page.locator('[role="dialog"]');
  }
  await page.waitForURL("http://localhost:5173/pantry/new", { timeout: 5000 });
  return page.locator("main");
}

async function shoot(page, filename) {
  const path = join(OUT_DIR, filename);
  await page.screenshot({ path, fullPage: false });
  console.log(`  wrote ${path}`);
}

async function captureStates(viewport, theme) {
  const isDesktop = viewport.width >= 768;
  const browser = await chromium.launch();
  const ctx = await browser.newContext({
    viewport: { width: viewport.width, height: viewport.height },
    colorScheme: theme === "dark" ? "dark" : "light",
  });
  const page = await ctx.newPage();
  const suffix = `${viewport.name}-${theme}-${Date.now()}`;

  try {
    await register(page, "shot-pantry", suffix);
    await ensureTheme(page, theme);

    console.log(`[${viewport.name}/${theme}] empty state`);
    let container = await openAddForm(page, isDesktop);
    await container.locator('input[placeholder="e.g. Whole milk"]').fill("Whole milk");
    await container.locator('[data-testid="quantity-input"]').focus();
    await page.waitForTimeout(150);
    await shoot(
      page,
      `pantry-add-form-quantity-empty-${theme}-${viewport.name}.png`,
    );

    console.log(`[${viewport.name}/${theme}] valid parse "2 kg"`);
    await container.locator('[data-testid="quantity-input"]').fill("2 kg");
    await page.waitForSelector('[data-testid="quantity-preview"]');
    await page.waitForTimeout(150);
    await shoot(
      page,
      `pantry-add-form-quantity-parsed-${theme}-${viewport.name}.png`,
    );

    console.log(`[${viewport.name}/${theme}] unit-assumed "5"`);
    await container.locator('[data-testid="quantity-input"]').fill("5");
    await page.waitForSelector('[data-testid="quantity-preview"]');
    await page.waitForTimeout(150);
    await shoot(
      page,
      `pantry-add-form-quantity-assumed-${theme}-${viewport.name}.png`,
    );

    console.log(`[${viewport.name}/${theme}] unparseable "a dozen"`);
    await container.locator('[data-testid="quantity-input"]').fill("a dozen");
    await page.waitForSelector('[data-testid="quantity-preview"]');
    await page.waitForTimeout(150);
    await shoot(
      page,
      `pantry-add-form-quantity-unparseable-${theme}-${viewport.name}.png`,
    );

    await browser.close();
    return true;
  } catch (err) {
    console.error(`[${viewport.name}/${theme}] FAILED: ${err.message}`);
    await browser.close();
    return false;
  }
}

let allOk = true;
for (const vp of viewports) {
  for (const theme of themes) {
    const ok = await captureStates(vp, theme);
    if (!ok) allOk = false;
  }
}
if (!allOk) process.exit(1);
console.log("\nAll screenshots captured.");
