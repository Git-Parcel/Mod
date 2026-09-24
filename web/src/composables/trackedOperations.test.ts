import { describe, expect, it } from "vitest";
import type { OperationDto } from "../api/types";
import {
  collectSettledTracked,
  onOperationTracked,
  trackSubmitted,
  trackedCount,
} from "./trackedOperations";

function operation(id: string, state: OperationDto["state"]): OperationDto {
  return {
    operationId: id,
    kind: "save_snapshot",
    owner: "web-console",
    target: "t",
    state,
    phase: "p",
    completed: 0,
    total: null,
    unit: null,
    submittedAt: "2026-09-24T00:00:00Z",
    startedAt: null,
    updatedAt: "2026-09-24T00:00:00Z",
    completedAt: null,
    result: null,
    error: null,
    errorCode: null,
  };
}

function trackedOperation(id: string): Promise<OperationDto> {
  return Promise.resolve(operation(id, "running"));
}

describe("trackedOperations", () => {
  it("registers operations once the submit response arrives", async () => {
    const before = trackedCount();
    trackSubmitted(trackedOperation("register-1"));
    await Promise.resolve();
    await Promise.resolve();
    expect(trackedCount()).toBe(before + 1);
  });

  it("fires the tracked hook after registration", async () => {
    let fired = 0;
    onOperationTracked(() => {
      fired += 1;
    });
    trackSubmitted(trackedOperation("register-2"));
    await Promise.resolve();
    await Promise.resolve();
    expect(fired).toBe(1);
  });

  it("collects only settled tracked operations and forgets them", async () => {
    trackSubmitted(trackedOperation("running-1"));
    trackSubmitted(trackedOperation("done-1"));
    await Promise.resolve();
    await Promise.resolve();

    const all = [
      operation("running-1", "running"),
      operation("done-1", "succeeded"),
      operation("stranger", "failed"),
    ];
    const settled = collectSettledTracked(all);
    expect(settled.map((op) => op.operationId)).toEqual(["done-1"]);

    // Already-reported operations are never collected twice.
    expect(collectSettledTracked(all)).toEqual([]);
  });
});
