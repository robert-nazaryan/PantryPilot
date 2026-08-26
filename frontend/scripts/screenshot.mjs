import { chromium } from "playwright";
import { mkdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

const __dirname = dirname(fileURLToPath(import.meta.url));
const outDir = resolve(__dirname, "..", "screenshots");
await mkdir(outDir, { recursive: true });

const pages = [
  { name: "login", path: "/login" },
  { name: "register", path: "/register" },
];
const viewports = [
  { name: "mobile-375", width: 375, height: 812 },
  { name: "desktop-1440", width: 1440, height: 900 },
];

const browser = await chromium.launch();
for (const vp of viewports) {
  const ctx = await browser.newContext({ viewport: { width: vp.width, height: vp.height } });
  for (const p of pages) {
    const page = await ctx.newPage();
    const url = `http://localhost:5173${p.path}`;
    await page.goto(url, { waitUntil: "networkidle" });
    // wait for auth-init redirect settle
    await page.waitForTimeout(500);
    const file = resolve(outDir, `${p.name}-${vp.name}.png`);
    await page.screenshot({ path: file, fullPage: true });
    console.log(`saved ${file}`);
    await page.close();
  }
  await ctx.close();
}
await browser.close();
