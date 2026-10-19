import { expect, test, type Route } from "@playwright/test";

const session = {
  userId: "00000000-0000-4000-8000-000000000001",
  organizationId: "00000000-0000-4000-8000-000000000002",
  storeId: "00000000-0000-4000-8000-000000000003",
  fullName: "Người học API",
  trainingEnabled: true,
  roles: ["SALES"],
  permissions: [],
};
const json = (route: Route, body: unknown, status = 200) =>
  route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });

test("authenticated training saves catalog actions and abandons without operational writes", async ({
  page,
}) => {
  const actions: unknown[] = [];
  const operationalWrites: string[] = [];
  let abandoned = false;
  page.on("request", (request) => {
    if (
      ["POST", "PUT", "PATCH", "DELETE"].includes(request.method()) &&
      /\/api\/v1\/(inventory|sales|receipts|reports)\b/.test(request.url())
    ) {
      operationalWrites.push(request.url());
    }
  });
  await page.route("**/api/v1/auth/login", (route) =>
    json(route, {
      accessToken: "a".repeat(43),
      tokenType: "Bearer",
      expiresAt: new Date(Date.now() + 3600000).toISOString(),
      session,
    }),
  );
  await page.route("**/api/v1/training/scenarios", (route) =>
    json(route, [{ id: "scenario-1", code: "SCN-RETAIL-01", version: 1 }]),
  );
  await page.route("**/api/v1/training/results?**", (route) => json(route, { items: [] }));
  await page.route("**/api/v1/training/sessions", (route) => {
    expect(route.request().headers()["idempotency-key"]).toBeTruthy();
    expect(route.request().postDataJSON()).toEqual({ scenarioId: "scenario-1" });
    return json(route, { id: "session-1", nextActionSequence: 1, status: "IN_PROGRESS" }, 201);
  });
  await page.route("**/api/v1/training/sessions/session-1/actions", (route) => {
    actions.push(route.request().postDataJSON());
    return json(route, { nextActionSequence: 2, status: "IN_PROGRESS" });
  });
  await page.route("**/api/v1/training/sessions/session-1/abandon", (route) => {
    abandoned = true;
    return json(route, { id: "session-1", status: "ABANDONED" });
  });

  await page.goto("/");
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("learner");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("server-pass");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await page
    .getByRole("button", { name: /Đào tạo nghiệp vụ/ })
    .first()
    .click();
  await page.getByRole("button", { name: /Bắt đầu ca thực hành/ }).click();
  await page.getByTestId("npc-customer").click();
  await page.getByRole("button", { name: /Dạ, chị muốn 2 hộp sữa ít đường đúng không ạ/ }).click();
  await expect.poll(() => actions.length).toBe(1);
  expect(actions).toEqual([
    {
      actionSequence: 1,
      actionType: "greet_customer",
      targetKey: "customer_linh",
      choiceKey: "greet_confirm_need",
    },
  ]);
  await expect(page.getByTestId("training-save-status")).toContainText("máy chủ");
  await page.getByTestId("btn-dialogue-next").click();
  await page
    .getByRole("button", { name: /Về cửa hàng/ })
    .first()
    .click();
  await page.getByTestId("btn-confirm-exit").click();
  await expect.poll(() => abandoned).toBe(true);
  expect(operationalWrites).toEqual([]);
});
