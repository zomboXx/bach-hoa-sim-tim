import { expect, test } from "@playwright/test";
import {
  TrainingApiAdapter,
  TrainingRun,
  type TrainingSavedResult,
} from "../src/training/api-training-adapter";

test("response loss retries the same start key and action sequence without client grades", async () => {
  const starts: Array<{ key: string; body: unknown }> = [];
  const actions: unknown[] = [];
  const saved: TrainingSavedResult = {
    totalScore: 80,
    passed: true,
    grade: "SATISFACTORY",
    errorCount: 2,
    completedAt: "2026-10-19T00:00:00Z",
  };
  let startAttempts = 0;
  let actionAttempts = 0;
  const reply = (body: unknown) =>
    new Response(JSON.stringify(body), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  const api = new TrainingApiAdapter(async (path, init) => {
    if (path === "/api/v1/training/scenarios") {
      return reply([{ id: "scenario-1", code: "SCN-RETAIL-01", version: 1 }]);
    }
    if (path === "/api/v1/training/sessions") {
      starts.push({
        key: new Headers(init?.headers).get("Idempotency-Key") || "",
        body: JSON.parse(String(init?.body)),
      });
      if (++startAttempts === 1) throw new Error("response lost");
      return reply({ id: "session-1", nextActionSequence: 1, status: "IN_PROGRESS" });
    }
    if (path === "/api/v1/training/sessions/session-1/actions") {
      actions.push(JSON.parse(String(init?.body)));
      if (++actionAttempts === 1) throw new Error("response lost");
      return reply({ nextActionSequence: 2, status: "COMPLETED", result: saved });
    }
    throw new Error(`Unexpected path ${path}`);
  });
  const updates: Array<{ message: string; result?: TrainingSavedResult; failed?: boolean }> = [];
  const run = new TrainingRun(api, (message, result, failed) => {
    updates.push({ message, result, failed });
  });

  await run.start();
  expect(updates.at(-1)?.failed).toBe(true);
  run.append({
    actionType: "greet_customer",
    targetKey: "customer_linh",
    choiceKey: "greet_confirm_need",
  });
  await run.retry();
  expect(updates.at(-1)?.failed).toBe(true);
  await run.retry();

  expect(starts).toHaveLength(2);
  expect(starts[0].key).toBeTruthy();
  expect(starts[0]).toEqual(starts[1]);
  expect(starts[0].body).toEqual({ scenarioId: "scenario-1" });
  expect(actions).toEqual([
    {
      actionSequence: 1,
      actionType: "greet_customer",
      targetKey: "customer_linh",
      choiceKey: "greet_confirm_need",
    },
    {
      actionSequence: 1,
      actionType: "greet_customer",
      targetKey: "customer_linh",
      choiceKey: "greet_confirm_need",
    },
  ]);
  expect(updates.at(-1)?.result).toEqual(saved);
});

test("leaving after a lost start response abandons the recovered server session", async () => {
  const startKeys: string[] = [];
  const paths: string[] = [];
  const reply = (body: unknown) =>
    new Response(JSON.stringify(body), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  const api = new TrainingApiAdapter(async (path, init) => {
    paths.push(path);
    if (path === "/api/v1/training/scenarios") {
      return reply([{ id: "scenario-1", code: "SCN-RETAIL-01", version: 1 }]);
    }
    if (path === "/api/v1/training/sessions") {
      startKeys.push(new Headers(init?.headers).get("Idempotency-Key") || "");
      if (startKeys.length === 1) throw new Error("response lost");
      return reply({ id: "session-1", nextActionSequence: 1, status: "IN_PROGRESS" });
    }
    if (path === "/api/v1/training/sessions/session-1/abandon") {
      return reply({ id: "session-1", status: "ABANDONED" });
    }
    throw new Error(`Unexpected path ${path}`);
  });
  const run = new TrainingRun(api, () => undefined);

  await run.start();
  run.append({
    actionType: "greet_customer",
    targetKey: "customer_linh",
    choiceKey: "greet_confirm_need",
  });
  await run.abandon();
  await run.retry();

  expect(startKeys).toHaveLength(2);
  expect(startKeys[0]).toBeTruthy();
  expect(startKeys[0]).toBe(startKeys[1]);
  expect(paths.at(-1)).toBe("/api/v1/training/sessions/session-1/abandon");
  expect(paths.some((path) => path.endsWith("/actions"))).toBe(false);
});
