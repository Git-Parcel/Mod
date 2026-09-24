import { describe, expect, it, vi } from "vitest";
import type { OperationDto } from "../api/types";
import {
  announceSettledTracked,
  onOperationTracked,
  onTrackedSettled,
  trackedCount,
  trackSubmitted,
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

  it("announces settled tracked operations exactly once", async () => {
    const listener = vi.fn();
    onTrackedSettled(listener);

    trackSubmitted(trackedOperation("evt-running"));
    trackSubmitted(trackedOperation("evt-done"));
    await Promise.resolve();
    await Promise.resolve();

    const feed = [
      operation("evt-running", "running"),
      operation("evt-done", "succeeded"),
      operation("stranger", "failed"),
    ];

    announceSettledTracked(feed);
    expect(listener).toHaveBeenCalledTimes(1);
    expect(listener.mock.calls[0][0].operationId).toBe("evt-done");

    // Announced operations are never announced twice.
    announceSettledTracked(feed);
    expect(listener).toHaveBeenCalledTimes(1);
  });
});
