import { test, expect } from "@playwright/test";

const TRAINING_ROUTE = process.env.TRN_ROUTE || "/training";

const VIEWPORTS = [
  { name: "desktop", width: 1280, height: 720 },
  { name: "mobile", width: 390, height: 844 },
];

for (const vp of VIEWPORTS) {
  test.describe(`TRN-01 Scene Cửa Hàng [${vp.name} ${vp.width}x${vp.height}]`, () => {
    test(`Di chuyển, va chạm, tương tác ổn định và KHÔNG gọi API ghi vận hành (${vp.name})`, async ({
      page,
    }) => {
      const operationalWriteRequests: string[] = [];

      // Giám sát toàn bộ network requests để xác nhận 0 request ghi xuống API vận hành
      page.on("request", (req) => {
        const method = req.method().toUpperCase();
        const url = req.url().toLowerCase();
        const isWriteMethod = ["POST", "PUT", "PATCH", "DELETE"].includes(method);
        const isOperationalEndpoint =
          url.includes("/api/sales") ||
          url.includes("/api/invoices") ||
          url.includes("/api/inventory") ||
          url.includes("/api/receiving") ||
          url.includes("/api/stocktake") ||
          url.includes("/api/catalog");

        if (isWriteMethod && isOperationalEndpoint) {
          operationalWriteRequests.push(`${method} ${req.url()}`);
        }
      });

      await page.setViewportSize({ width: vp.width, height: vp.height });
      await page.goto(TRAINING_ROUTE);

      // 1. Xác nhận Scene hiển thị đúng, có telemetry và không tràn ngang viewport
      const sceneRoot = page.getByTestId("trn-scene-root");
      await expect(sceneRoot).toBeVisible();
      await expect(page.getByTestId("sandbox-indicator")).toBeVisible();
      await expect(page.getByTestId("viewport-label")).toContainText(`${vp.width}x${vp.height}`);
      expect(
        await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
      ).toBe(true);

      // Bắt đầu ca thực hành
      await page.getByRole("button", { name: /Bắt đầu ca thực hành/ }).click();
      const player = page.locator(".world-player");
      await expect(player).toHaveAttribute("data-x", "10");
      await expect(player).toHaveAttribute("data-y", "11");

      // 2. Kiểm tra di chuyển Bàn phím & Va chạm tường (Collision)
      await page.keyboard.press("ArrowDown");
      await expect(player).toHaveAttribute("data-y", "12");
      // Thử đi tiếp vào tường dưới: va chạm chặn lại, giữ nguyên y=12
      await page.keyboard.press("ArrowDown");
      await expect(player).toHaveAttribute("data-y", "12");

      // 3. Kiểm tra điều khiển Touch D-Pad (Cảm ứng)
      await page.getByTestId("dpad-right").click();
      await expect(player).toHaveAttribute("data-x", "11");
      await page.getByTestId("dpad-up").click();
      await expect(player).toHaveAttribute("data-y", "11");

      // 4. Kiểm tra Chuột: Click sàn (click-to-move qua BFS path finding)
      const svg = page.getByTestId("store-svg");
      const box = await svg.boundingBox();
      expect(box).toBeTruthy();
      await svg.click({ position: { x: box!.width * 0.55, y: box!.height * 0.65 } });
      await page.waitForTimeout(600);
      const afterClickY = Number(await player.getAttribute("data-y"));
      expect(afterClickY).toBeLessThanOrEqual(10);

      // 5. Kiểm tra tương tác với Khách Linh (Mục tiêu 1: Giao tiếp & tư vấn)
      const customerChoice = page.getByRole("button", {
        name: /Dạ, chị muốn 2 hộp sữa ít đường/,
      });
      if (!(await customerChoice.isVisible())) {
        await page.getByTestId("npc-customer").click();
      }
      await expect(customerChoice).toBeVisible({ timeout: 5000 });
      await customerChoice.click();
      await page.getByRole("button", { name: /Tiếp tục trong cửa hàng/ }).click();
      await expect(player).toHaveAttribute("data-stage", "milk");

      // 6. Kiểm tra tương tác Kệ sữa (Mục tiêu 2: Nhặt hàng vào giỏ)
      await page.getByRole("button", { name: "Đi đến Kệ sữa & đồ uống", exact: true }).click();
      await page.getByRole("button", { name: /Lấy 2 hộp sữa ít đường/ }).click();
      await page.getByRole("button", { name: /Tiếp tục trong cửa hàng/ }).click();
      await expect(page.locator(".bag-panel")).toContainText("× 2");

      // 7. Kiểm tra tương tác Quầy thanh toán (Mục tiêu 3: Quét mã và hoàn tất đơn)
      await page.getByRole("button", { name: "Đi đến Quầy thanh toán", exact: true }).click();
      await page.getByRole("button", { name: "Quét hộp sữa 1", exact: true }).click();
      await page.getByRole("button", { name: "Quét hộp sữa 2", exact: true }).click();
      await page.getByRole("button", { name: /Nhận tiền từ khách/ }).click();
      await page.getByRole("button", { name: /Em gửi chị 3.000đ tiền thừa/ }).click();
      await page.getByRole("button", { name: /Tiếp tục trong cửa hàng/ }).click();

      // 8. Khẳng định tuyệt đối: 0 request ghi xuống API vận hành
      expect(operationalWriteRequests).toEqual([]);
    });
  });
}
