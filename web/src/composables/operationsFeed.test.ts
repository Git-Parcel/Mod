import { describe, expect, it } from "vitest";
import type { OperationDto } from "../api/types";
import { currentIntervalMs } from "./operationsFeed";

function operation(state: OperationDto["state"]): OperationDto {
  return {
    operationId: state,
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

describe("currentIntervalMs", () => {
  it("polls fast while operations are in flight", () => {
    expect(currentIntervalMs([operation("running")], 0)).toBe(3000);
    expect(currentIntervalMs([operation("queued")], 0)).toBe(3000);
  });

  it("polls fast while tracked operations exist", () => {
    expect(currentIntervalMs([operation("succeeded")], 1)).toBe(3000);
    expect(currentIntervalMs([], 2)).toBe(3000);
  });

  it("relaxes once everything is settled and untracked", () => {
    expect(currentIntervalMs([operation("succeeded")], 0)).toBe(30000);
    expect(currentIntervalMs([], 0)).toBe(30000);
  });
});
