import { chromium } from "playwright";
import { mkdir } from "node:fs/promises";
import { resolve } from "node:path";

const outDir = resolve("screenshots");
await mkdir(outDir, { recursive: true });

const password = "Password12345";
const suffix = Date.now();

const browser = await chromium.launch();
let failed = false;

function assert(cond, msg) {
  if (!cond) {
    console.error(`  FAIL: ${msg}`);
    failed = true;
  } else {
    console.log(`  OK:   ${msg}`);
  }
}

async function register(page) {
  const email = `theme-${suffix}-${Math.random().toString(36).slice(2, 8)}@test.local`;
  await page.goto("http://localhost:5173/register", { waitUntil: "networkidle" });
  await page.fill("input[type=email]", email);
  await page.fill("input[type=password]", password);
  await page.fill('input[autocomplete="nickname"]', "Chef");
  await page.click('button:has-text("Create account")');
  await page.waitForURL("http://localhost:5173/dashboard", { timeout: 10000 });
}

// ---- Test 1: theme toggle flips <html> class ----
console.log("\n== T1: toggle flips <html> class ==");
{
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await register(page);

  const initialClass = await page.evaluate(() => document.documentElement.className);
  assert(!initialClass.includes("dark"), `initial theme is light (html class: "${initialClass}")`);

  await page.locator("button[aria-label='Switch to dark mode']").first().click();
  await page.waitForTimeout(150);
  const afterToggle = await page.evaluate(() => document.documentElement.className);
  assert(afterToggle.includes("dark"), `after toggle html has 'dark' class (html class: "${afterToggle}")`);

  await page.locator("button[aria-label='Switch to light mode']").first().click();
  await page.waitForTimeout(150);
  const afterUntoggle = await page.evaluate(() => document.documentElement.className);
  assert(!afterUntoggle.includes("dark"), `after second toggle 'dark' removed (html class: "${afterUntoggle}")`);
  await ctx.close();
}

// ---- Test 2: reload preserves theme via localStorage ----
console.log("\n== T2: reload preserves choice ==");
{
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await register(page);

  await page.locator("button[aria-label='Switch to dark mode']").first().click();
  const stored = await page.evaluate(() => localStorage.getItem("pantrypilot-theme"));
  assert(stored === "dark", `localStorage says '${stored}' after toggle`);

  await page.reload({ waitUntil: "networkidle" });
  const afterReloadClass = await page.evaluate(() => document.documentElement.className);
  assert(afterReloadClass.includes("dark"), `dark class present immediately after reload (no flash): "${afterReloadClass}"`);

  const storedAfter = await page.evaluate(() => localStorage.getItem("pantrypilot-theme"));
  assert(storedAfter === "dark", `localStorage still 'dark' after reload`);
  await ctx.close();
}

// ---- Test 3: default is light when no localStorage entry ----
console.log("\n== T3: default is light ==");
{
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await page.goto("http://localhost:5173/login", { waitUntil: "networkidle" });
  const cls = await page.evaluate(() => document.documentElement.className);
  const stored = await page.evaluate(() => localStorage.getItem("pantrypilot-theme"));
  assert(!cls.includes("dark"), `html has no 'dark' class by default (was: "${cls}")`);
  assert(stored === null, `no localStorage entry pre-toggle (was: ${stored})`);
  await ctx.close();
}

// ---- Test 4: active-route indicator ----
console.log("\n== T4: active-route indicator ==");
{
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await register(page);

  // On /dashboard, Dashboard link should have primary tint bg
  const dashClass = await page
    .locator("nav[aria-label='Primary'] a", { hasText: "Dashboard" })
    .getAttribute("class");
  assert(dashClass?.includes("bg-primary/10") || dashClass?.includes("text-primary"),
    `Dashboard link is active-styled on /dashboard (class: "${dashClass}")`);

  const pantryClassBefore = await page
    .locator("nav[aria-label='Primary'] a", { hasText: "Pantry" })
    .getAttribute("class");
  assert(!pantryClassBefore?.includes("bg-primary/10"),
    `Pantry link is NOT active-styled on /dashboard (class: "${pantryClassBefore}")`);

  await page.locator("nav[aria-label='Primary'] a", { hasText: "Pantry" }).click();
  await page.waitForURL("http://localhost:5173/pantry");
  const pantryClassAfter = await page
    .locator("nav[aria-label='Primary'] a", { hasText: "Pantry" })
    .getAttribute("class");
  assert(pantryClassAfter?.includes("bg-primary/10") || pantryClassAfter?.includes("text-primary"),
    `Pantry link is active-styled on /pantry (class: "${pantryClassAfter}")`);
  await ctx.close();
}

// ---- Test 5: mobile hamburger menu opens and closes ----
console.log("\n== T5: mobile hamburger menu ==");
{
  const ctx = await browser.newContext({ viewport: { width: 375, height: 812 } });
  const page = await ctx.newPage();
  await register(page);

  const mobileMenuBefore = await page.locator("#mobile-menu").count();
  assert(mobileMenuBefore === 0, "mobile menu closed initially");

  await page.locator("button[aria-label='Open menu']").click();
  await page.waitForSelector("#mobile-menu");
  const mobileMenuAfter = await page.locator("#mobile-menu").count();
  assert(mobileMenuAfter === 1, "mobile menu open after clicking hamburger");

  await page.screenshot({
    path: resolve(outDir, "navbar-mobile-menu-open-light-mobile-375.png"),
    fullPage: false,
  });
  console.log("  saved screenshot navbar-mobile-menu-open-light-mobile-375");

  // Click a link inside the menu → menu should close AND navigate
  await page.locator("#mobile-menu a", { hasText: "Pantry" }).click();
  await page.waitForURL("http://localhost:5173/pantry");
  const mobileMenuAfterNav = await page.locator("#mobile-menu").count();
  assert(mobileMenuAfterNav === 0, "mobile menu closed after clicking a nav link");

  await ctx.close();
}

// ---- Test 6: mobile menu also works in dark mode ----
console.log("\n== T6: mobile hamburger in dark mode ==");
{
  const ctx = await browser.newContext({ viewport: { width: 375, height: 812 } });
  await ctx.addInitScript(() => {
    try { localStorage.setItem("pantrypilot-theme", "dark"); } catch {}
  });
  const page = await ctx.newPage();
  await register(page);
  const cls = await page.evaluate(() => document.documentElement.className);
  assert(cls.includes("dark"), `html has 'dark' class from localStorage init`);
  await page.locator("button[aria-label='Open menu']").click();
  await page.waitForSelector("#mobile-menu");
  await page.screenshot({
    path: resolve(outDir, "navbar-mobile-menu-open-dark-mobile-375.png"),
    fullPage: false,
  });
  console.log("  saved screenshot navbar-mobile-menu-open-dark-mobile-375");
  await ctx.close();
}

await browser.close();

if (failed) {
  console.error("\n=== ONE OR MORE ASSERTIONS FAILED ===");
  process.exitCode = 1;
} else {
  console.log("\n=== ALL PASSED ===");
}
