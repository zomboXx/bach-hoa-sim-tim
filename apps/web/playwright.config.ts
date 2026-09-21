import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./tests",
  workers: 1,
  use: {
    baseURL: "http://127.0.0.1:4174",
    channel: process.env.CI ? undefined : "chrome",
    viewport: { width: 1440, height: 1050 },
  },
  webServer: {
    command: "npm run preview",
    url: "http://127.0.0.1:4174",
    reuseExistingServer: true,
  },
});
