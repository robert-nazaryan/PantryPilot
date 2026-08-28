import { chromium } from "playwright";

const suffix = Date.now();
const email = `oauth-verify-${suffix}@test.local`;
const password = "Password12345";
const backendDisplayName = "Robert Nazaryan";

const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
const page = await ctx.newPage();

console.log("[test] load login page to establish browser origin");
await page.goto("http://localhost:5173/login", { waitUntil: "networkidle" });

console.log("[test] register via browser so refresh cookie lands in browser context");
const registerResult = await page.evaluate(
  async ({ email, password }) => {
    const resp = await fetch("http://localhost:8080/api/auth/register", {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password, displayName: "IgnoredRegisterName" }),
    });
    if (!resp.ok) throw new Error(`register failed: ${resp.status}`);
    return resp.json();
  },
  { email, password },
);
const accessToken = registerResult.accessToken;
const expiresIn = registerResult.expiresIn;

console.log("[test] clear any displayName localStorage from register-side applyTokens");
await page.evaluate(() => {
  window.localStorage.removeItem("pantrypilot-display-name");
});

const callbackUrl =
  "http://localhost:5173/auth/callback" +
  `?accessToken=${encodeURIComponent(accessToken)}` +
  `&expiresIn=${expiresIn}` +
  `&displayName=${encodeURIComponent(backendDisplayName)}`;

console.log("[test] visiting synthetic /auth/callback with displayName query param");
await page.goto(callbackUrl, { waitUntil: "networkidle" });
await page.waitForURL("http://localhost:5173/dashboard", { timeout: 10000 });

const heading = (await page.locator("h1").first().textContent())?.trim() ?? "";
console.log(`[test] dashboard greeting: "${heading}"`);
if (!heading.includes(backendDisplayName)) {
  throw new Error(`expected "${backendDisplayName}" in greeting, got: ${heading}`);
}

const navbarName = (await page
  .locator("nav[aria-label='Primary']")
  .locator("..")
  .locator("span, a, button")
  .filter({ hasText: backendDisplayName })
  .first()
  .textContent()
  .catch(() => null))?.trim();
console.log(`[test] navbar reads: "${navbarName ?? "(not found via primary-nav neighbor lookup)"}"`);
const pageBody = await page.locator("body").textContent();
if (!pageBody?.includes(backendDisplayName)) {
  throw new Error(`expected "${backendDisplayName}" somewhere in page body`);
}

console.log("[test] reload page to confirm cached displayName survives (localStorage persistence)");
await page.reload({ waitUntil: "networkidle" });
await page.waitForSelector("h1");
const headingAfterReload = (await page.locator("h1").first().textContent())?.trim() ?? "";
if (!headingAfterReload.includes(backendDisplayName)) {
  throw new Error(`after reload: expected "${backendDisplayName}", got: ${headingAfterReload}`);
}
console.log("[test] PASS — OAuth-style displayName query param flows to greeting and survives reload");

await browser.close();
