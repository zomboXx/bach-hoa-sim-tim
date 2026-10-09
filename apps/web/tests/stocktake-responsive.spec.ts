import { expect, test, type Page } from "@playwright/test";

const declaredViewports = [
  { name: "mobile", width: 360, height: 800 },
  { name: "tablet", width: 768, height: 1024 },
  { name: "desktop", width: 1366, height: 768 },
] as const;

async function signInAsManager(page: Page) {
  await page.goto("/");
  await page.getByRole("button", { name: "Quản lý cửa hàng QL001" }).click();
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
}

async function openStocktake(page: Page) {
  await page.locator("nav").getByRole("button", { name: "Kiểm kê", exact: false }).click();
  await expect(page.getByRole("heading", { name: "Kiểm kê theo lô" })).toBeVisible();
}

test("declared mobile, tablet and desktop viewports have no horizontal overflow", async ({
  page,
}) => {
  await signInAsManager(page);
  await openStocktake(page);

  for (const viewport of declaredViewports) {
    await test.step(viewport.name, async () => {
      await page.setViewportSize(viewport);
      await expect(page.getByRole("heading", { name: "Kiểm kê theo lô" })).toBeVisible();
      await expect(page.getByText("Cửa hàng hiện tại")).toBeVisible();
      await expect(page.getByText("MAIN", { exact: true })).toBeVisible();
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
        ),
      ).toBe(true);
    });
  }
});

test("KG accepts at most three decimals and saves the selected batch", async ({ page }) => {
  await signInAsManager(page);
  await openStocktake(page);

  await page.getByLabel("Lô hàng").selectOption({ label: "Gạo thơm · LO08" });
  await expect(page.getByText("KG", { exact: true }).first()).toBeVisible();
  await page.getByLabel("Số lượng thực tế").fill("12,345");
  await page.getByLabel("Ghi chú", { exact: true }).fill("Cân lại tại kệ gạo");
  await page.getByRole("button", { name: "Gửi phiếu kiểm kê" }).click();

  const record = page.locator(".count-row").first();
  await expect(record).toContainText("Gạo thơm");
  await expect(record).toContainText("LO08");
  await expect(record).toContainText("12,345");
  await expect(record).toContainText("KG");
  await expect(record).toContainText("Chờ duyệt");
  await expect(page.locator(".stocktake-success")).toContainText("Đã gửi số đếm");

  await page.getByLabel("Số lượng thực tế").fill("12.3456");
  await page.getByRole("button", { name: "Gửi phiếu kiểm kê" }).click();
  await expect(page.getByRole("alert")).toContainText("Đơn vị KG nhận tối đa 3 chữ số thập phân.");
  await expect(page.locator(".count-row")).toHaveCount(1);
});

test("counted units reject decimals and stocktake stays batch-specific", async ({ page }) => {
  await signInAsManager(page);
  await openStocktake(page);

  await page.getByLabel("Lô hàng").selectOption("LO01");
  await page.getByLabel("Số lượng thực tế").fill("44.5");
  await page.getByRole("button", { name: "Gửi phiếu kiểm kê" }).click();
  await expect(page.getByRole("alert")).toContainText("Đơn vị này chỉ nhận số nguyên không âm.");
  await expect(page.locator(".count-row")).toHaveCount(0);
});

test("users without stocktake access do not receive the stocktake navigation", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Nhân viên bán hàng NV001" }).click();
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await expect(
    page.locator("nav").getByRole("button", { name: "Kiểm kê", exact: false }),
  ).toHaveCount(0);
});
