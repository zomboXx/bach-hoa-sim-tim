import { defineConfig } from "@playwright/test";

const apiProxyTarget = process.env.API_PROXY_TARGET;
if (!apiProxyTarget) throw new Error("API_PROXY_TARGET is required for live backend tests.");

export default defineConfig({
  testDir: "./live-tests",
  workers: 1,
  use: {
    baseURL: "http://127.0.0.1:4176",
    channel: process.env.CI ? undefined : "chrome",
    viewport: { width: 1440, height: 1050 },
    serviceWorkers: "block",
  },
  webServer: {
    command: "npm run build && npm run preview -- --port 4176",
    env: {
      VITE_USE_API: "true",
      VITE_INVENTORY_TIMEOUT_MS: "1000",
      API_PROXY_TARGET: apiProxyTarget,
    },
    url: "http://127.0.0.1:4176",
    reuseExistingServer: false,
  },
});
