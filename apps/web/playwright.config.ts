import { defineConfig } from "@playwright/test";
const port = Number(process.env.SIMTIM_DEMO_TEST_PORT || "4174");
export default defineConfig({
  testDir: "./tests",
  testIgnore: "**/api-*.spec.ts",
  workers: 1,
  use: {
    baseURL: `http://127.0.0.1:${port}`,
    channel: process.env.CI ? undefined : "chrome",
    viewport: { width: 1440, height: 1050 },
  },
  webServer: {
    command: `npm run preview -- --port ${port} --strictPort`,
    url: `http://127.0.0.1:${port}`,
    reuseExistingServer: false,
  },
});
