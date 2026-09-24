// API + static smoke test against a running console (mock or the real in-game
// server) to catch contract drift without a browser.
//
//   MOCK_URL=http://127.0.0.1:5640 MOCK_TOKEN=feedface... npm run smoke
//
// Defaults target `npm run mock` on port 5640.
import { request as httpRequest } from "node:http";

const BASE = process.env.MOCK_URL ?? "http://127.0.0.1:5640";
const TOKEN = process.env.MOCK_TOKEN ?? "feedfacefeedfacefeedfacefeedface";

let passed = 0;
let failed = 0;

function check(name, ok, detail = "") {
  if (ok) {
    passed += 1;
  } else {
    failed += 1;
    console.error(`FAIL ${name}${detail ? ` — ${detail}` : ""}`);
  }
}

function request(method, path, { headers = {}, body } = {}) {
  return new Promise((resolvePromise) => {
    const req = httpRequest(new URL(path, BASE), { method, headers }, (res) => {
      const chunks = [];
      res.on("data", (chunk) => chunks.push(chunk));
      res.on("end", () =>
        resolvePromise({
          status: res.statusCode,
          text: Buffer.concat(chunks).toString("utf-8"),
          contentType: res.headers["content-type"] ?? "",
        }));
    });
    req.on("error", () => resolvePromise({ status: 0, text: "", contentType: "" }));
    if (body !== undefined) req.write(JSON.stringify(body));
    req.end();
  });
}

async function json(method, path, options = {}) {
  const res = await request(method, path, {
    ...options,
    headers: {
      Authorization: `Bearer ${TOKEN}`,
      ...(options.body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    },
  });
  let payload = null;
  try {
    payload = JSON.parse(res.text);
  } catch {
    // Non-JSON body.
  }
  return { res, payload };
}

// region auth
{
  const res = await request("GET", "/api/status");
  check("unauthenticated request is rejected with 401", res.status === 401);
  check("401 body carries the stable code", res.text === '{"error":"unauthorized"}');
}
{
  const res = await request("GET", "/api/status", {
    headers: { Authorization: "Bearer wrong" },
  });
  check("wrong bearer token is rejected", res.status === 401);
}
{
  const res = await request("GET", `/api/status?token=${TOKEN}`);
  check("query token is accepted", res.status === 200);
}
// endregion

// region status shape
{
  const { res, payload } = await json("GET", "/api/status");
  check("status is 200", res.status === 200);
  check(
    "status shape",
    Boolean(
      payload &&
        payload.minecraft?.name &&
        typeof payload.minecraft?.dataVersion === "number" &&
        payload.gitparcel?.version &&
        Array.isArray(payload.parcels) &&
        payload.operations &&
        typeof payload.serverTime === "string",
    ),
  );
}
// endregion

// region parcels CRUD contract
let probeUuid = null;
{
  const { res, payload } = await json("POST", "/api/parcels", {
    body: {
      dimension: "minecraft:the_end",
      from: [0, 0, 0],
      to: [7, 7, 7],
      name: "Smoke Probe",
    },
  });
  check("create returns 201", res.status === 201);
  check("create echoes the name", payload?.name === "Smoke Probe");
  probeUuid = payload?.uuid ?? null;
}
{
  const { payload: rejected } = await json("POST", "/api/parcels", {
    body: {
      dimension: "minecraft:overworld",
      from: [0, 0, 0],
      to: [7, 7, 7],
      name: "",
    },
  });
  check(
    "create with a blank name is rejected with invalid_name",
    rejected?.error === "invalid_name",
  );
}
if (probeUuid) {
  const { res: detail } = await json("GET", `/api/parcels/${probeUuid}`);
  check("created parcel is retrievable", detail.status === 200);

  const { payload: deleted } = await json(
    "DELETE",
    `/api/parcels/${probeUuid}`,
  );
  check("delete returns 204/empty", deleted === null || deleted === undefined);
  const gone = await json("GET", `/api/parcels/${probeUuid}`);
  check("deleted parcel is gone", gone.res.status === 404);
}
// endregion

// region operations
{
  const { payload } = await json("GET", "/api/operations?limit=50");
  check(
    "operations list shape",
    Array.isArray(payload?.operations) &&
      payload.operations.every(
        (op) =>
          typeof op.operationId === "string" &&
          typeof op.kind === "string" &&
          ["queued", "running", "succeeded", "failed", "canceled"].includes(op.state),
      ),
  );
}
// endregion

// region static + SPA fallback
{
  const res = await request("GET", "/");
  check("root serves the SPA entry", res.status === 200 && res.text.includes('id="app"'));
  const asset = res.text.match(/assets\/[^"]+\.js/)?.[0];
  check("entry references a bundled asset", Boolean(asset));
  if (asset) {
    const assetRes = await request("GET", `/${asset}`);
    check("bundled asset is served", assetRes.status === 200);
  }
  const spa = await request("GET", "/some/deep/route");
  check(
    "SPA fallback serves index for unknown paths",
    spa.status === 200 && spa.text.includes('id="app"'),
  );
}
// endregion

console.log(`smoke: ${passed} passed, ${failed} failed`);
if (failed > 0) process.exit(1);
