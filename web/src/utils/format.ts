import { currentLocale } from "../i18n";
import type { OperationDto, Vec3 } from "../api/types";

export function formatCoord(vec: Vec3): string {
  return `(${vec[0]}, ${vec[1]}, ${vec[2]})`;
}

export function formatBounds(from: Vec3, to: Vec3): string {
  return `${from[0]} ${from[1]} ${from[2]} → ${to[0]} ${to[1]} ${to[2]}`;
}

export function formatSize(size: Vec3): string {
  return `${size[0]}×${size[1]}×${size[2]}`;
}

export function formatVolume(size: Vec3): number {
  return size[0] * size[1] * size[2];
}

/** Inclusive block-box size between two corners, in either corner order. */
export function boxSize(from: Vec3, to: Vec3): Vec3 {
  return [
    Math.abs(to[0] - from[0]) + 1,
    Math.abs(to[1] - from[1]) + 1,
    Math.abs(to[2] - from[2]) + 1,
  ];
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  const units = ["KiB", "MiB", "GiB", "TiB"];
  let value = bytes;
  let unit = "B";
  for (const next of units) {
    if (value < 1024) break;
    value /= 1024;
    unit = next;
  }
  return `${value.toFixed(1)} ${unit}`;
}

const timeFormats = new Map<string, Intl.DateTimeFormat>();

export function formatTime(iso: string | null): string {
  if (!iso) return "—";
  const locale = currentLocale();
  let formatter = timeFormats.get(locale);
  if (!formatter) {
    formatter = new Intl.DateTimeFormat(locale, {
      dateStyle: "short",
      timeStyle: "medium",
    });
    timeFormats.set(locale, formatter);
  }
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? iso : formatter.format(date);
}

export function abbreviate(id: string): string {
  return id.substring(0, Math.min(8, id.length));
}

export function isOperationActive(operation: OperationDto): boolean {
  return operation.state === "queued" || operation.state === "running";
}

/** Phase plus counters; units are never merged into a fake overall percentage. */
export function formatProgress(operation: OperationDto): string {
  const unit = operation.unit ?? "";
  if (operation.total === null) {
    return `${operation.completed} ${unit}`.trim();
  }
  return `${operation.completed} / ${operation.total} ${unit}`.trim();
}
