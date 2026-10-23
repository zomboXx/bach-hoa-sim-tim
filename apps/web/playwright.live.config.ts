import { defineConfig } from "@playwright/test";

const apiProxyTarget = process.env.API_PROXY_TARGET;
if (!apiProxyTarget) throw new Error("API_PROXY_TARGET is required for live backend tests.");
const port = Number(process.env.SIMTIM_LIVE_TEST_PORT || 4176);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error("Invalid live test port.");
const baseURL = `http://127.0.0.1:${port}`;

export default defineConfig({
  testDir: "./live-tests",
  outputDir: process.env.PLAYWRIGHT_OUTPUT_DIR || "test-results/live",
  workers: 1,
  use: {
    baseURL,
    channel: process.env.CI ? undefined : "chrome",
    viewport: { width: 1440, height: 1050 },
    serviceWorkers: "block",
  },
  webServer: {
    command: `npm run build && npm run preview -- --port ${port} --strictPort`,
    env: {
      VITE_USE_API: "true",
      VITE_INVENTORY_TIMEOUT_MS: "1000",
      API_PROXY_TARGET: apiProxyTarget,
    },
    url: baseURL,
    reuseExistingServer: false,
  },
});
