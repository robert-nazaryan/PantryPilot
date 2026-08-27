import { chromium } from "playwright";
import { resolve } from "node:path";

const outDir = resolve("screenshots");
const browser = await chromium.launch();

for (const theme of ["light", "dark"]) {
  for (const vp of [
    { name: "mobile-375", width: 375, height: 812 },
    { name: "desktop-1440", width: 1440, height: 900 },
  ]) {
    const ctx = await browser.newContext({ viewport: { width: vp.width, height: vp.height } });
    await ctx.addInitScript((t) => {
      try { localStorage.setItem("pantrypilot-theme", t); } catch {}
    }, theme);
    const page = await ctx.newPage();
    await page.goto("http://localhost:5173/login", { waitUntil: "networkidle" });
    await page.screenshot({
      path: resolve(outDir, `login-with-google-${theme}-${vp.name}.png`),
      fullPage: true,
    });
    console.log(`saved login-with-google-${theme}-${vp.name}`);
    await page.goto("http://localhost:5173/register", { waitUntil: "networkidle" });
    await page.screenshot({
      path: resolve(outDir, `register-with-google-${theme}-${vp.name}.png`),
      fullPage: true,
    });
    console.log(`saved register-with-google-${theme}-${vp.name}`);
    await ctx.close();
  }
}

await browser.close();
