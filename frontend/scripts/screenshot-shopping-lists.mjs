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
  const ctx = await browser.newContext({
    viewport: { width: vp.width, height: vp.height },
  });
  await ctx.addInitScript((t) => {
    try {
      localStorage.setItem("pantrypilot-theme", t);
    } catch {}
  }, theme);
  return ctx;
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

async function shot(page, name, opts = {}) {
  const path = resolve(outDir, `${name}.png`);
  await page.screenshot({ path, fullPage: opts.fullPage ?? true });
  console.log(`  saved ${name}`);
}

async function createShoppingList(page, name) {
  await page.locator('[data-testid="new-shopping-list-button"]').click();
  await page.locator('[data-testid="shopping-list-name-input"]').fill(name);
  await page.locator('[data-testid="submit-shopping-list"]').click();
  await page.waitForURL(/\/shopping-lists\/\d+$/, { timeout: 10000 });
  await page.waitForSelector('[data-testid="shopping-list-name"]');
}

async function addItem(page, item) {
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

async function checkItem(page, name) {
  const row = page.locator(
    `[data-testid^="shopping-list-item-"]:has-text("${name}")`,
  );
  await row.locator('input[type="checkbox"]').click();
  await page.waitForFunction(
    (n) => {
      const rows = document.querySelectorAll(
        '[data-testid^="shopping-list-item-"]',
      );
      for (const el of rows) {
        if (el.textContent?.includes(n) && el.getAttribute("data-checked") === "true") {
          return true;
        }
      }
      return false;
    },
    name,
    { timeout: 5000 },
  );
}

const sampleItems = [
  { name: "Bread", quantity: 1, unit: "loaf" },
  { name: "Milk", quantity: 2, unit: "l" },
  { name: "Eggs", quantity: 12, unit: "pcs" },
  { name: "Tomatoes", quantity: 500, unit: "g" },
];

for (const theme of themes) {
  for (const vp of viewports) {
    console.log(`\n=== shopping lists ${theme} / ${vp.name} ===`);

    // Empty list list
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await register(page, `slempty-${theme}-${vp.name}`);
      await page.goto("http://localhost:5173/shopping-lists", {
        waitUntil: "networkidle",
      });
      await page.waitForSelector("text=No shopping lists yet");
      await shot(page, `shopping-lists-empty-${theme}-${vp.name}`);
      await ctx.close();
    }

    // Create modal
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await register(page, `slmodal-${theme}-${vp.name}`);
      await page.goto("http://localhost:5173/shopping-lists", {
        waitUntil: "networkidle",
      });
      await page.waitForSelector("text=No shopping lists yet");
      await page.locator('[data-testid="new-shopping-list-button"]').click();
      await page.waitForSelector('[data-testid="shopping-list-name-input"]');
      await shot(page, `shopping-lists-create-modal-${theme}-${vp.name}`);
      // viewport-only to prove modal + buttons fit at this viewport
      await shot(
        page,
        `shopping-lists-create-modal-viewport-${theme}-${vp.name}`,
        { fullPage: false },
      );
      await ctx.close();
    }

    // List with content + detail with mixed checked/unchecked items
    {
      const ctx = await newCtx(vp, theme);
      const page = await ctx.newPage();
      await register(page, `sllist-${theme}-${vp.name}`);
      await page.goto("http://localhost:5173/shopping-lists", {
        waitUntil: "networkidle",
      });
      await createShoppingList(page, "Weekend groceries");

      for (const item of sampleItems) {
        await addItem(page, item);
      }

      await checkItem(page, "Bread");
      await checkItem(page, "Eggs");

      await shot(page, `shopping-list-detail-${theme}-${vp.name}`);

      // Create a second list so the list view has multiple entries
      await page.goto("http://localhost:5173/shopping-lists", {
        waitUntil: "networkidle",
      });
      await createShoppingList(page, "Party supplies");
      await page.goto("http://localhost:5173/shopping-lists", {
        waitUntil: "networkidle",
      });
      await page.waitForSelector("text=Weekend groceries");
      await shot(page, `shopping-lists-list-${theme}-${vp.name}`);
      await ctx.close();
    }
  }
}

await browser.close();
console.log("\nShopping list screenshots saved.");
