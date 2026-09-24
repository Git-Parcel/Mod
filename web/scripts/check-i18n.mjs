// Audits the locale files: zh/en key parity, keys referenced in source that are
// missing, and defined-but-never-referenced keys (informational).
//
//   npm run check-i18n
import { readdirSync, readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = fileURLToPath(new URL(".", import.meta.url));
const srcDir = resolve(here, "..", "src");
const localesDir = join(srcDir, "locales");

function flatten(object, prefix = "") {
  const keys = new Set();
  for (const [key, value] of Object.entries(object)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (value !== null && typeof value === "object") {
      for (const child of flatten(value, path)) keys.add(child);
    } else {
      keys.add(path);
    }
  }
  return keys;
}

const zh = flatten(
  JSON.parse(readFileSync(join(localesDir, "zh-CN.json"), "utf-8")),
);
const en = flatten(
  JSON.parse(readFileSync(join(localesDir, "en-US.json"), "utf-8")),
);

let failed = false;
for (const key of zh) {
  if (!en.has(key)) {
    console.error(`missing in en-US: ${key}`);
    failed = true;
  }
}
for (const key of en) {
  if (!zh.has(key)) {
    console.error(`missing in zh-CN: ${key}`);
    failed = true;
  }
}

// Collect keys referenced from source: t('...'), t("...") and dynamic prefixes.
const referenced = new Set();
const dynamicPrefixes = new Set();
const sourceFiles = [];
function walk(dir) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) walk(path);
    else if (/\.(vue|ts)$/.test(entry.name)) sourceFiles.push(path);
  }
}
walk(srcDir);

const staticKeyPattern = /\bt\(\s*'([^']+)'/g;
const dynamicPrefixPattern = /\bt\(\s*'([^']*)\.\$\{/g;
const translatePrefixPattern = /translateId\(\s*'([^']+)'/g;
const hasPrefixPattern = /hasTranslation\(\s*'([^']+)'/g;

for (const file of sourceFiles) {
  const text = readFileSync(file, "utf-8");
  for (const match of text.matchAll(staticKeyPattern)) referenced.add(match[1]);
  for (const match of text.matchAll(dynamicPrefixPattern)) {
    dynamicPrefixes.add(match[1]);
  }
  for (const match of text.matchAll(translatePrefixPattern)) {
    dynamicPrefixes.add(match[1]);
  }
  for (const match of text.matchAll(hasPrefixPattern)) {
    dynamicPrefixes.add(match[1]);
  }
}

// Dynamic families: state.<x>, opKind.<x>, apiErrors.<code>, dims.<id>,
// sync.<x>, snapshots.source.<x>, mirror.<x>, rotation.<x>, repos.type.<x>.
const dynamicFamilies = {
  state: ["queued", "running", "succeeded", "failed", "canceled"],
  opKind: [
    "save_snapshot",
    "restore_snapshot",
    "retry_restore",
    "rollback_restore",
    "publish_snapshot",
    "import_snapshot",
    "query_snapshot_tree",
    "create",
    "clone",
    "fetch",
    "pull",
    "push",
  ],
  apiErrors: [
    "network",
    "unauthorized",
    "not_found",
    "bad_request",
    "invalid_body",
    "invalid_name",
    "invalid_value",
    "overlap",
    "volume_limit",
    "busy",
    "server_busy",
    "stale_cursor",
    "internal_error",
  ],
  dims: ["minecraft_overworld", "minecraft_the_nether", "minecraft_the_end"],
  sync: ["synced", "outOfSync", "never"],
  "snapshots.source": ["saved", "imported"],
  mirror: ["none", "left_right", "front_back"],
  rotation: ["none", "clockwise_90", "clockwise_180", "counterclockwise_90"],
  "repos.type": ["local", "cloned"],
};

for (const [prefix, values] of Object.entries(dynamicFamilies)) {
  for (const value of values) {
    referenced.add(`${prefix}.${value}`);
  }
}

for (const key of referenced) {
  if (key.includes("${")) continue; // template expression, skipped
  if (!zh.has(key)) {
    console.error(`referenced but missing in zh-CN: ${key}`);
    failed = true;
  }
  if (!en.has(key)) {
    console.error(`referenced but missing in en-US: ${key}`);
    failed = true;
  }
}

const unused = [...zh].filter((key) => !referenced.has(key));
if (unused.length > 0) {
  console.warn(
    `defined but never referenced (informational):\n  ${unused.join("\n  ")}`,
  );
}

if (failed) {
  process.exit(1);
}
console.log(`i18n OK: ${zh.size} keys, parity held.`);
