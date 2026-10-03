import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  testMatch: "**/api-*.spec.ts",
  workers: 1,
  use: {
    baseURL: "http://127.0.0.1:4175",
    channel: process.env.CI ? undefined : "chrome",
    viewport: { width: 1440, height: 1050 },
    serviceWorkers: "block",
  },
  webServer: {
    command: "npm run build && npm run preview -- --port 4175",
    env: { VITE_USE_API: "true", VITE_INVENTORY_TIMEOUT_MS: "300" },
    url: "http://127.0.0.1:4175",
    reuseExistingServer: false,
  },
});
