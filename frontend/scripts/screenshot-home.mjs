import { chromium } from "playwright";

const email = `home-shot-${Date.now()}@test.local`;
const password = "Password12345";
const displayName = "Chef Robert";

const browser = await chromium.launch();
for (const vp of [
  { name: "mobile-375", width: 375, height: 812 },
  { name: "desktop-1440", width: 1440, height: 900 },
]) {
  const ctx = await browser.newContext({ viewport: { width: vp.width, height: vp.height } });
  const page = await ctx.newPage();
  await page.goto("http://localhost:5173/register", { waitUntil: "networkidle" });
  await page.fill("input[type=email]", `${vp.name}-${email}`);
  await page.fill("input[type=password]", password);
  await page.fill('input[autocomplete="nickname"]', displayName);
  await page.click('button:has-text("Create account")');
  await page.waitForURL("http://localhost:5173/", { timeout: 5000 });
  await page.waitForSelector("text=Welcome,");
  await page.screenshot({ path: `screenshots/home-${vp.name}.png`, fullPage: true });
  console.log(`saved screenshots/home-${vp.name}.png`);
  await ctx.close();
}
await browser.close();
