import { expect, test, type BrowserContext, type Page } from "@playwright/test";

async function warmOfflineShell(page: Page) {
  await page.goto("/");
  await page.evaluate(async () => {
    if ("serviceWorker" in navigator) await navigator.serviceWorker.ready;
  });
  await page.reload();
}

async function signInAs(page: Page, accountName: string) {
  await page.getByRole("button", { name: accountName }).click();
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
}

async function openStocktake(page: Page) {
  await page.locator("nav").getByRole("button", { name: "Kiểm kê", exact: false }).click();
  await expect(page.getByRole("heading", { name: "Kiểm kê theo lô" })).toBeVisible();
}

async function setOffline(context: BrowserContext, page: Page, offline: boolean) {
  await context.setOffline(offline);
  await expect(
    page.getByText(offline ? "Ngoại tuyến" : "Đang kết nối", { exact: true }),
  ).toBeVisible();
}

async function createOfflineCount(page: Page, note: string, quantity = "47") {
  await page.getByLabel("Lô hàng").selectOption("LO01");
  await page.getByLabel("Số lượng thực tế").fill(quantity);
  await page.getByLabel("Ghi chú", { exact: true }).fill(note);
  await page.getByRole("button", { name: "Lưu phiếu offline" }).click();
  const record = page.locator(".count-row").first();
  await expect(record).toContainText("Chờ kết nối");
  const operationText = await record.locator(".operation-id").textContent();
  const clientOperationId = operationText?.match(
    /[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}/i,
  )?.[0];
  expect(clientOperationId).toBeTruthy();
  return clientOperationId!;
}

test("offline count survives reload and a repeated save keeps one clientOperationId", async ({
  context,
  page,
}) => {
  await warmOfflineShell(page);
  await signInAs(page, "Nhân viên hàng hóa KHO001");
  await openStocktake(page);
  await setOffline(context, page, true);

  const note = "Kiểm kê offline kệ sữa";
  const clientOperationId = await createOfflineCount(page, note);

  await page.getByLabel("Số lượng thực tế").fill("47");
  await page.getByLabel("Ghi chú", { exact: true }).fill(note);
  await page.getByRole("button", { name: "Lưu phiếu offline" }).click();
  await expect(page.locator(".count-row")).toHaveCount(1);
  await expect(page.locator(".operation-id")).toContainText(clientOperationId);

  await page.reload();
  await openStocktake(page);
  await expect(page.locator(".count-row")).toHaveCount(1);
  await expect(page.locator(".count-row").first()).toContainText("Chờ kết nối");
  await expect(page.locator(".operation-id")).toContainText(clientOperationId);

  await setOffline(context, page, false);
  await expect(page.locator(".count-row")).toHaveCount(1);
  await expect(page.locator(".count-row").first()).toContainText("Chờ duyệt");
  await expect(page.locator(".operation-id")).toContainText(clientOperationId);
});

test("retry is blocked after changing account and succeeds again for the original actor", async ({
  context,
  page,
}) => {
  await warmOfflineShell(page);
  await signInAs(page, "Nhân viên hàng hóa KHO001");
  await openStocktake(page);
  await setOffline(context, page, true);
  const clientOperationId = await createOfflineCount(page, "Phiếu thuộc KHO001");

  await page.getByRole("button", { name: /Đăng xuất/ }).click();
  await context.setOffline(false);
  await page.waitForFunction(() => navigator.onLine);
  await signInAs(page, "Quản lý cửa hàng QL001");
  await openStocktake(page);

  const blocked = page.locator(".count-row").filter({ hasText: clientOperationId });
  await expect(blocked).toContainText("Chờ kết nối");
  await expect(blocked).toContainText("Thao tác thuộc tài khoản khác");
  await page.getByRole("button", { name: "Đồng bộ 1 phiếu" }).click();
  await expect(blocked).toContainText("Chờ kết nối");

  await page.getByRole("button", { name: /Đăng xuất/ }).click();
  await signInAs(page, "Nhân viên hàng hóa KHO001");
  await openStocktake(page);
  const retried = page.locator(".count-row").filter({ hasText: clientOperationId });
  await expect(retried).toContainText("Chờ duyệt");
  await expect(page.locator(".count-row")).toHaveCount(1);
});

test("retry is blocked when the queued store differs from the current session", async ({
  context,
  page,
}) => {
  await warmOfflineShell(page);
  await signInAs(page, "Nhân viên hàng hóa KHO001");
  await openStocktake(page);
  await setOffline(context, page, true);
  const clientOperationId = await createOfflineCount(page, "Sai phạm vi cửa hàng");

  await page.evaluate(async (operationId) => {
    const database = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("simtim-stocktake-queue-v1", 1);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
    await new Promise<void>((resolve, reject) => {
      const transaction = database.transaction("operations", "readwrite");
      const store = transaction.objectStore("operations");
      const request = store.get(operationId);
      request.onsuccess = () => store.put({ ...request.result, storeId: "OTHER-STORE" });
      transaction.oncomplete = () => resolve();
      transaction.onerror = () => reject(transaction.error);
    });
    database.close();
  }, clientOperationId);

  await page.reload();
  await openStocktake(page);
  const blocked = page.locator(".count-row").filter({ hasText: clientOperationId });
  await expect(blocked).toContainText("Thao tác thuộc cửa hàng khác");

  await setOffline(context, page, false);
  await expect(blocked).toContainText("Chờ kết nối");
  await expect(page.locator(".count-row")).toHaveCount(1);
});

test("changed stock becomes conflict without overwriting inventory or duplicating a count", async ({
  page,
}) => {
  await warmOfflineShell(page);
  await signInAs(page, "Quản lý cửa hàng QL001");
  await openStocktake(page);
  await page.getByLabel("Số lượng thực tế").fill("44");
  await page.getByLabel("Ghi chú", { exact: true }).fill("Đối chiếu trước khi bán");
  await page.getByRole("button", { name: "Gửi phiếu kiểm kê" }).click();
  const operationText = await page.locator(".operation-id").textContent();
  const clientOperationId = operationText?.match(
    /[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}/i,
  )?.[0];
  expect(clientOperationId).toBeTruthy();

  const navigation = page.locator("nav");
  await navigation.getByRole("button", { name: "Bán hàng", exact: false }).click();
  await page.getByRole("button", { name: /Sữa tươi ít đường/ }).click();
  await page.getByLabel("Khách đưa").fill("10000");
  await page.getByRole("button", { name: "Xác nhận thanh toán" }).click();
  await expect(page.locator(".invoice-row")).toHaveCount(1);

  await navigation.getByRole("button", { name: "Kiểm kê", exact: false }).click();
  await page.getByRole("button", { name: "Duyệt điều chỉnh tồn" }).click();
  const conflict = page.locator(".count-row").filter({ hasText: clientOperationId! });
  await expect(conflict).toContainText("Tồn đã đổi");
  await expect(conflict).toContainText("không ghi đè tự động");
  await expect(page.locator(".count-row")).toHaveCount(1);
  await navigation.getByRole("button", { name: "Tồn kho & lô hàng", exact: false }).click();
  await expect(page.locator("tr").filter({ hasText: "LO01" })).toContainText("47");
});
