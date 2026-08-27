import { chromium } from "playwright";

const password = "Password12345";

async function runLifecycle(viewport) {
  const suffix = `${viewport.name}-${Date.now()}`;
  const email = `pantry-lifecycle-${suffix}@test.local`;
  const isDesktop = viewport.width >= 768;
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: viewport.width, height: viewport.height } });
  const page = await ctx.newPage();
  const logs = [];
  page.on("console", (m) => logs.push(`[${m.type()}] ${m.text()}`));
  page.on("pageerror", (e) => logs.push(`[pageerror] ${e.message}`));

  function log(msg) {
    console.log(`  [${viewport.name}] ${msg}`);
  }

  try {
    log("register + login");
    await page.goto("http://localhost:5173/register", { waitUntil: "networkidle" });
    await page.fill("input[type=email]", email);
    await page.fill("input[type=password]", password);
    await page.fill('input[autocomplete="nickname"]', "Chef");
    await page.click('button:has-text("Create account")');
    await page.waitForURL("http://localhost:5173/pantry", { timeout: 10000 });
    await page.waitForSelector("h1:has-text('Your pantry')");

    log("empty state visible");
    await page.waitForSelector("text=Your pantry is empty");

    log("open add form via list header button");
    await page.locator("main button", { hasText: "Add item" }).first().click();
    if (isDesktop) {
      await page.waitForSelector('[role="dialog"]', { timeout: 5000 });
      log("desktop: modal opened at same URL");
      if (!page.url().endsWith("/pantry")) throw new Error(`desktop should stay at /pantry, got ${page.url()}`);
    } else {
      await page.waitForURL("http://localhost:5173/pantry/new", { timeout: 5000 });
      log("mobile: navigated to /pantry/new page");
    }

    const container = isDesktop ? page.locator('[role="dialog"]') : page.locator("main");
    log("fill and submit create form");
    await container.locator('input[placeholder="e.g. Whole milk"]').fill("Test milk");
    await container.locator('input[type="number"]').fill("2");
    await container.locator('input[placeholder="e.g. L, kg, cans"]').fill("L");
    await container.locator('input[placeholder="e.g. Dairy"]').fill("Dairy");
    const in3 = new Date(); in3.setDate(in3.getDate() + 3);
    await container.locator('input[type="date"]').fill(in3.toISOString().slice(0, 10));
    await container.locator("button", { hasText: "Add item" }).click();

    if (isDesktop) {
      await page.waitForSelector('[role="dialog"]', { state: "detached", timeout: 8000 });
    } else {
      await page.waitForURL("http://localhost:5173/pantry", { timeout: 8000 });
    }
    log("item appears in list");
    await page.waitForSelector('h3:has-text("Test milk")');
    const qtyText1 = await page.locator('li:has-text("Test milk")').locator("p").first().textContent();
    log(`  quantity shown: "${qtyText1}"`);
    if (!qtyText1.includes("2 L")) throw new Error(`expected "2 L", got "${qtyText1}"`);

    log("expiry flag shows 'Expires in 3 days' (warning)");
    await page.waitForSelector('text=Expires in 3 days');

    log("quick-consume -0.5");
    await page.locator('li:has-text("Test milk") button[aria-label="Consume 0.5 L"]').click();
    await page.waitForFunction(() =>
      document.querySelector('li h3')?.textContent === 'Test milk' &&
      Array.from(document.querySelectorAll('li')).some((li) =>
        li.textContent?.includes("Test milk") && li.textContent?.includes("1.5 L")
      ),
      { timeout: 5000 }
    );
    log("  quantity dropped to 1.5 L");

    log("quick-consume -1");
    await page.locator('li:has-text("Test milk") button[aria-label="Consume 1 L"]').click();
    await page.waitForFunction(() =>
      Array.from(document.querySelectorAll('li')).some((li) =>
        li.textContent?.includes("Test milk") && li.textContent?.includes("0.5 L")
      ),
      { timeout: 5000 }
    );
    log("  quantity is 0.5 L");

    log("open edit via pencil icon");
    await page.locator('li:has-text("Test milk") button[aria-label="Edit Test milk"]').click();
    if (isDesktop) {
      await page.waitForSelector('[role="dialog"] :text("Edit pantry item")');
    } else {
      await page.waitForURL(/\/pantry\/\d+\/edit/, { timeout: 5000 });
      await page.waitForSelector('h1:has-text("Edit pantry item")');
    }

    const editContainer = isDesktop ? page.locator('[role="dialog"]') : page.locator("main");
    await editContainer.locator('input[placeholder="e.g. Whole milk"]').fill("Renamed milk");
    await editContainer.locator("button", { hasText: "Save changes" }).click();

    if (isDesktop) {
      await page.waitForSelector('[role="dialog"]', { state: "detached", timeout: 8000 });
    } else {
      await page.waitForURL("http://localhost:5173/pantry", { timeout: 8000 });
    }
    log("edit reflected in list");
    await page.waitForSelector('h3:has-text("Renamed milk")');

    log("delete with inline confirm");
    await page.locator('li:has-text("Renamed milk") button[aria-label="Delete Renamed milk"]').click();
    await page.waitForSelector('text=Delete this item?');
    await page.locator('li:has-text("Renamed milk") button:has-text("Delete")').click();
    await page.waitForFunction(() =>
      !document.querySelector('h3')?.textContent?.includes("Renamed milk"), { timeout: 5000 }
    );
    await page.waitForSelector("text=Your pantry is empty");
    log("empty state restored after delete");

    log("PASSED");
    await browser.close();
    return true;
  } catch (err) {
    console.error(`  [${viewport.name}] FAILED: ${err.message}`);
    logs.forEach((l) => console.error(`    ${l}`));
    await browser.close();
    return false;
  }
}

const viewports = [
  { name: "mobile-375", width: 375, height: 812 },
  { name: "desktop-1440", width: 1440, height: 900 },
];

let allPassed = true;
for (const vp of viewports) {
  console.log(`\n=== ${vp.name} ===`);
  const ok = await runLifecycle(vp);
  if (!ok) allPassed = false;
}

if (!allPassed) {
  console.error("\n=== ONE OR MORE VIEWPORTS FAILED ===");
  process.exitCode = 1;
} else {
  console.log("\n=== ALL VIEWPORTS PASSED ===");
}
