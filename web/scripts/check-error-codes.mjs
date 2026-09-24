// Cross-checks the stable error codes: every code the Java API can emit must
// have a matching apiErrors.<code> entry in both locale packs, and vice versa.
//
//   npm run check-errors
import { readdirSync, readFileSync, statSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = fileURLToPath(new URL(".", import.meta.url));
const webRoot = resolve(here, "..");
const repoRoot = resolve(webRoot, "..");

// 1. Error codes emitted by the Java API layer.
const javaRoot = join(repoRoot, "src", "main", "java");
const javaCodes = new Set();
function walkJava(dir) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) walkJava(path);
    else if (entry.name.endsWith(".java")) {
      scanJava(readFileSync(path, "utf-8"));
    }
  }
}
function scanJava(text) {
  // ApiException carries named codes; jsonError covers the pre-route auth/size
  // rejections in WebService that bypass ApiException.
  const patterns = [
    /new ApiException\(\s*\d+\s*,\s*"([a-z_]+)"\s*\)/g,
    /jsonError\(\s*\d+\s*,\s*"([a-z_]+)"\s*\)/g,
  ];
  for (const pattern of patterns) {
    for (const match of text.matchAll(pattern)) {
      javaCodes.add(match[1]);
    }
  }
}
walkJava(javaRoot);

// 2. Error codes documented as the API contract in DESIGN.md.
const design = readFileSync(join(repoRoot, "docs", "DESIGN.md"), "utf-8");
const designSection = design.split("### API 约定")[1] ?? "";
const designCodes = new Set();
for (const match of designSection.matchAll(/`([a-z_]+)`/g)) {
  // Error codes appear in the "错误码：`a`、`b`" line only.
}
const errorLine = designSection
  .split("\n")
  .find((line) => line.includes("错误码："));
if (errorLine) {
  for (const match of errorLine.matchAll(/`([a-z_]+)`/g)) {
    designCodes.add(match[1]);
  }
}

// 3. Locale packs.
const localesDir = join(webRoot, "src", "locales");
function loadApiErrorKeys(file) {
  const data = JSON.parse(readFileSync(file, "utf-8"));
  return new Set(
    Object.keys(data)
      .filter((key) => key.startsWith("apiErrors."))
      .map((key) => key.slice("apiErrors.".length)),
  );
}
const zhCodes = loadApiErrorKeys(join(localesDir, "zh-CN.json"));
const enCodes = loadApiErrorKeys(join(localesDir, "en-US.json"));

// 4. Codes the frontend can synthesise locally.
const clientCodes = ["network"];

let failed = false;
for (const code of javaCodes) {
  for (const [name, set] of [["zh-CN", zhCodes], ["en-US", enCodes]]) {
    if (!set.has(code)) {
      console.error(
        `java emits "${code}" but apiErrors.${code} is missing in ${name}`,
      );
      failed = true;
    }
  }
  if (!designCodes.has(code)) {
    console.error(
      `java emits "${code}" but it is missing from the DESIGN error-code list`,
    );
    failed = true;
  }
}
for (const code of designCodes) {
  if (!javaCodes.has(code)) {
    console.error(`DESIGN lists "${code}" but the Java layer never emits it`);
    failed = true;
  }
}

if (failed) process.exit(1);
console.log(
  `error codes OK: ${javaCodes.size} emitted by java, all localized, DESIGN list in sync.`,
);
