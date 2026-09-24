import { describe, expect, it } from "vitest";
import type { OperationDto, Vec3 } from "../api/types";
import {
  abbreviate,
  formatBytes,
  formatProgress,
  formatSize,
  formatTime,
  formatVolume,
  isOperationActive,
} from "./format";

const vec = (x: number, y: number, z: number): Vec3 => [x, y, z];describe("formatBytes", () => {
  it("keeps bytes below 1 KiB unscaled", () => {
    expect(formatBytes(0)).toBe("0 B");
    expect(formatBytes(1023)).toBe("1023 B");
  });

  it("scales through the unit ladder", () => {
    expect(formatBytes(1024)).toBe("1.0 KiB");
    expect(formatBytes(48211394)).toBe("46.0 MiB");
  });
});

describe("formatSize / formatVolume", () => {
  it("formats dimensions and multiplies them", () => {
    expect(formatSize(vec(32, 20, 32))).toBe("32×20×32");
    expect(formatVolume(vec(3, 4, 5))).toBe(60);
  });
});

describe("abbreviate", () => {
  it("cuts long ids to 8 characters and keeps short ones whole", () => {
    expect(abbreviate("0123456789abcdef")).toBe("01234567");
    expect(abbreviate("short")).toBe("short");
  });
});

function operation(overrides: Partial<OperationDto>): OperationDto {
  return {
    operationId: "op",
    kind: "save_snapshot",
    owner: "web-console",
    target: "t",
    state: "running",
    phase: "capturing",
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
    ...overrides,
  };
}

describe("formatProgress", () => {
  it("shows the phase count without a total", () => {
    expect(formatProgress(operation({ completed: 12, unit: "blocks" }))).toBe("12 blocks");
  });

  it("shows the fraction and unit with a total", () => {
    expect(
      formatProgress(operation({ completed: 90, total: 240, unit: "sections" })),
    ).toBe("90 / 240 sections");
  });
});

describe("formatTime", () => {
  it("renders an em dash for missing timestamps", () => {
    expect(formatTime(null)).toBe("—");
  });

  it("returns unparsable input unchanged", () => {
    expect(formatTime("not-a-date")).toBe("not-a-date");
  });

  it("formats valid ISO timestamps", () => {
    const out = formatTime("2026-09-24T00:00:00Z");
    expect(out).not.toBe("2026-09-24T00:00:00Z");
    expect(out.length).toBeGreaterThan(0);
  });
});

describe("isOperationActive", () => {
  it("treats queued and running as active only", () => {
    expect(isOperationActive(operation({ state: "queued" }))).toBe(true);
    expect(isOperationActive(operation({ state: "running" }))).toBe(true);
    expect(isOperationActive(operation({ state: "succeeded" }))).toBe(false);
    expect(isOperationActive(operation({ state: "failed" }))).toBe(false);
  });
});
