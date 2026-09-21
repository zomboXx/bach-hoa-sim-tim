import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./tests",
  testMatch: "**/sprint1.spec.ts",
  workers: 1,
  use: {
    baseURL: process.env.E2E_URL || "http://127.0.0.1:4174",
    channel: process.env.CI ? undefined : "chrome",
    viewport: { width: 1440, height: 1050 },
  },
  webServer: {
    command: "npm run preview",
    url: process.env.E2E_URL || "http://127.0.0.1:4174",
    reuseExistingServer: true,
  },
});
