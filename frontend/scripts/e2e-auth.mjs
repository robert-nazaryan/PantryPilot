import { chromium } from "playwright";

const email = `e2e-${Date.now()}@test.local`;
const password = "Password12345";
const displayName = "E2E Tester";

const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 1280, height: 800 } });
const page = await ctx.newPage();

const logs = [];
page.on("console", (m) => logs.push(`[${m.type()}] ${m.text()}`));
page.on("pageerror", (e) => logs.push(`[pageerror] ${e.message}`));

function log(step, note = "") {
  console.log(`[${step}]${note ? " " + note : ""}`);
}

try {
  log("1. Open /register");
  await page.goto("http://localhost:5173/register", { waitUntil: "networkidle" });
  await page.waitForSelector("input[type=email]");

  log("2. Fill and submit register");
  await page.fill("input[type=email]", email);
  await page.fill("input[type=password]", password);
  await page.fill('input[autocomplete="nickname"]', displayName);
  await page.click('button:has-text("Create account")');

  log("3. Wait for home page");
  await page.waitForURL("http://localhost:5173/", { timeout: 5000 });
  await page.waitForSelector("text=Welcome,");
  const homeGreeting = await page.textContent("h1");
  log("3a. Home greeting:", `"${homeGreeting}"`);
  if (!homeGreeting.includes(displayName)) throw new Error(`expected displayName in greeting, got: ${homeGreeting}`);

  log("4. Verify refresh cookie is httpOnly (JS cannot read it)");
  const jsCookies = await page.evaluate(() => document.cookie);
  log("4a. document.cookie =", `"${jsCookies}"`);
  const allCookies = await ctx.cookies();
  const refreshCookie = allCookies.find((c) => c.name === "refresh_token");
  if (!refreshCookie) throw new Error("no refresh_token cookie present in context");
  if (!refreshCookie.httpOnly) throw new Error("refresh_token cookie is not HttpOnly!");
  if (refreshCookie.sameSite !== "Strict") throw new Error(`expected SameSite=Strict, got ${refreshCookie.sameSite}`);
  if (jsCookies.includes("refresh_token")) throw new Error("refresh_token is readable by JS!");
  log("4b. cookie HttpOnly=true, SameSite=Strict, invisible to JS");

  log("5. Reload page to test silent refresh restore");
  await page.reload({ waitUntil: "networkidle" });
  await page.waitForSelector("text=Welcome,", { timeout: 5000 });
  const currentUrl = page.url();
  if (!currentUrl.endsWith("/")) throw new Error(`expected still at /, got ${currentUrl}`);
  log("5a. session survived reload via silent refresh");

  log("6. Logout");
  await page.click('button:has-text("Sign out")');
  await page.waitForURL("http://localhost:5173/login", { timeout: 5000 });
  log("6a. redirected to /login");

  log("7. Reload after logout should stay on /login");
  await page.reload({ waitUntil: "networkidle" });
  const urlAfterReload = page.url();
  if (!urlAfterReload.includes("/login")) throw new Error(`expected /login after logout+reload, got ${urlAfterReload}`);
  log("7a. still on /login after reload");

  log("8. Login with previously-registered credentials");
  await page.fill("input[type=email]", email);
  await page.fill("input[type=password]", password);
  await page.click('button:has-text("Sign in")');
  await page.waitForURL("http://localhost:5173/", { timeout: 5000 });
  log("8a. login worked, back at /");

  log("9. Login with wrong password should show error");
  await page.click('button:has-text("Sign out")');
  await page.waitForURL("http://localhost:5173/login", { timeout: 5000 });
  await page.fill("input[type=email]", email);
  await page.fill("input[type=password]", "wrongpassword");
  await page.click('button:has-text("Sign in")');
  const alert = await page.waitForSelector('[role="alert"]', { timeout: 5000 });
  const alertText = await alert.textContent();
  log("9a. error shown:", `"${alertText}"`);
  if (!alertText.toLowerCase().includes("incorrect")) throw new Error("expected 'incorrect' error message");

  log("10. Register with existing email should show 409 handling");
  await page.goto("http://localhost:5173/register", { waitUntil: "networkidle" });
  await page.fill("input[type=email]", email);
  await page.fill("input[type=password]", password);
  await page.click('button:has-text("Create account")');
  const dupErr = await page.waitForSelector('text=/already registered/i', { timeout: 5000 });
  log("10a. duplicate email error shown:", `"${await dupErr.textContent()}"`);

  console.log("\n=== ALL 10 CHECKS PASSED ===");
} catch (err) {
  console.error("\n=== FAILED ===");
  console.error(err.message);
  console.error("\nBrowser console log:");
  logs.forEach((l) => console.error("  " + l));
  process.exitCode = 1;
} finally {
  await browser.close();
}
