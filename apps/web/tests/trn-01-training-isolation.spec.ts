import { expect, test } from "@playwright/test";

test("training route leaves an authenticated employee's pending stocktake untouched", async ({
  page,
}) => {
  const operationalWrites: string[] = [];
  page.on("request", (request) => {
    if (
      /^(POST|PUT|PATCH|DELETE)$/.test(request.method()) &&
      /\/api\/v1\/(?:inventory|sales|receipts|reports|catalog)(?:\/|$)/.test(
        new URL(request.url()).pathname,
      )
    ) {
      operationalWrites.push(`${request.method()} ${request.url()}`);
    }
  });

  await page.goto("/");
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("KHO001");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("demo123");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await expect(page.locator("nav")).toBeVisible();

  const operationId = "training-isolation-pending";
  await page.evaluate(async (clientOperationId) => {
    await new Promise<void>((resolve, reject) => {
      const request = indexedDB.open("simtim-stocktake-queue-v1", 1);
      request.onupgradeneeded = () => {
        request.result.createObjectStore("operations", { keyPath: "clientOperationId" });
      };
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const db = request.result;
        const tx = db.transaction("operations", "readwrite");
        tx.objectStore("operations").put({
          clientOperationId,
          actorId: "KHO001",
          organizationId: "DEMO",
          storeId: "MAIN",
          countedAt: new Date().toISOString(),
          productId: "SP001",
          batchId: "LO01",
          expectedQuantity: 48,
          baseVersion: 0,
          actualQuantity: 47,
          note: "Đếm trước khi vào ca học",
          status: "PENDING",
        });
        tx.oncomplete = () => {
          db.close();
          resolve();
        };
        tx.onerror = () => reject(tx.error);
      };
    });
  }, operationId);

  await page.goto("/training");
  await expect(page.getByTestId("trn-scene-root")).toBeVisible();
  await page.getByRole("button", { name: /Bắt đầu ca thực hành/ }).click();
  await page.keyboard.press("ArrowDown");

  const saved = await page.evaluate(async (clientOperationId) => {
    return new Promise<{ queue: unknown; counts: number }>((resolve, reject) => {
      const request = indexedDB.open("simtim-stocktake-queue-v1", 1);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const db = request.result;
        const tx = db.transaction("operations");
        const item = tx.objectStore("operations").get(clientOperationId);
        item.onsuccess = () => {
          const queue = item.result;
          db.close();
          const stateRequest = indexedDB.open("simtim-v2", 1);
          stateRequest.onerror = () => reject(stateRequest.error);
          stateRequest.onsuccess = () => {
            const stateDb = stateRequest.result;
            const stateTx = stateDb.transaction("state");
            const state = stateTx.objectStore("state").get("demo");
            state.onsuccess = () => {
              resolve({ queue, counts: state.result?.counts.length ?? 0 });
              stateDb.close();
            };
            state.onerror = () => reject(state.error);
          };
        };
        item.onerror = () => reject(item.error);
      };
    });
  }, operationId);
  expect(saved.queue).toMatchObject({ clientOperationId: operationId, status: "PENDING" });
  expect(saved.counts).toBe(0);
  expect(operationalWrites).toEqual([]);
});
