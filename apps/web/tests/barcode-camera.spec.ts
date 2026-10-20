import { expect, test } from "@playwright/test";
import { cameraFromImage } from "./helpers/camera";

const samples = [
  ["api-rice", "8930000000001"],
  ["api-apple", "ST-APPLE-01"],
  ["demo-milk", "8934673120010"],
  ["demo-cookies", "8934673120027"],
];
for (const [file, code] of samples) {
  test("camera decodes exact Code 128 string: " + code, async ({ page }) => {
    await cameraFromImage(page, "/demo-barcodes/" + file + ".png");
    await page.goto("/");
    await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
    await page
      .locator("nav")
      .getByRole("button", { name: /Bán hàng/ })
      .click();
    await page.getByRole("button", { name: /Quét mã bằng camera/ }).click();
    const dialog = page.getByRole("dialog", { name: "Quét mã sản phẩm" });
    await expect(dialog.locator("strong")).toHaveText(code);
    // A stationary barcode must not add repeatedly.
    await expect(dialog.getByRole("button", { name: "Quét tiếp" })).toBeVisible();
    if (file === "demo-milk" || file === "demo-cookies") {
      await expect(page.locator(".cart-row")).toHaveCount(1);
      await expect(page.locator(".cart-row .stepper span")).toHaveText("1");
    }
    await dialog.getByRole("button", { name: "Đóng camera" }).click();
    await expect(dialog).toHaveCount(0);
    expect(
      await page.evaluate(() =>
        (window as Window & { posTestStream?: MediaStream })
          .posTestStream!.getTracks()
          .every((track) => track.readyState === "ended"),
      ),
    ).toBe(true);
  });
}
test("camera denial leaves manual barcode entry available", async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(navigator.mediaDevices, "getUserMedia", {
      value: () => Promise.reject(new DOMException("Denied", "NotAllowedError")),
    });
  });
  await page.goto("/");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await page
    .locator("nav")
    .getByRole("button", { name: /Bán hàng/ })
    .click();
  await page.getByRole("button", { name: /Quét mã bằng camera/ }).click();
  await expect(page.getByRole("alert")).toContainText("chưa cho phép");
  await page.getByRole("button", { name: "Đóng và nhập mã bằng tay" }).click();
  await page.getByLabel("Tìm sản phẩm bán hàng").fill("8934673120010");
  await page.getByLabel("Tìm sản phẩm bán hàng").press("Enter");
  await expect(page.locator(".cart-row")).toContainText("Sữa tươi ít đường");
});
