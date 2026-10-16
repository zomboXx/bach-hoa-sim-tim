import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./qa03-live-tests",
  outputDir: process.env.QA03_OUTPUT_DIR || "test-results/qa03-live",
  workers: 1,
  fullyParallel: false,
  reporter: "line",
  use: {
    baseURL: "http://127.0.0.1:4177",
    channel: process.env.QA03_BROWSER_CHANNEL || (process.env.CI ? undefined : "chrome"),
    viewport: { width: 390, height: 844 },
    serviceWorkers: "allow",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
});
