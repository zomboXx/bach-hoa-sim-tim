import { expect, test } from "@playwright/test";

const password = process.env.SIMTIM_LIVE_PASSWORD;
if (!password) throw new Error("SIMTIM_LIVE_PASSWORD is required for live backend tests.");

async function signIn(page: import("@playwright/test").Page) {
  await page.goto("/");
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("stock");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password!);
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
}

async function openReceive(page: import("@playwright/test").Page) {
  await page
    .locator("nav")
    .getByRole("button", { name: /Nhận hàng/ })
    .click();
}

test("real backend recovers a committed receipt after the POST response is lost", async ({
  page,
}) => {
  let posts = 0;
  await page.route("**/api/v1/inventory/receipts", async (route) => {
    if (route.request().method() !== "POST") {
      await route.continue();
      return;
    }
    posts++;
    const committedResponse = await route.fetch();
    expect(committedResponse.status()).toBe(201);
    await route.abort("timedout");
  });

  await signIn(page);
  await openReceive(page);
  await page.getByLabel("Giá nhập (VND)").fill("24500");
  await page.getByLabel(/Số lô nhà cung cấp/).fill(`LIVE-${crypto.randomUUID().slice(0, 8)}`);
  const expiry = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
  await page.getByLabel(/Hạn sử dụng/).fill(expiry);
  await page.getByRole("button", { name: "Xác nhận phiếu nhận" }).click();

  await expect(page.getByText("Thao tác trước chưa có kết luận")).toBeVisible();
  expect(posts).toBe(1);

  await page.reload();
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("stock");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password!);
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await openReceive(page);

  await expect(page.getByRole("status")).toContainText("Máy chủ đã ghi phiếu");
  expect(posts).toBe(1);
});
