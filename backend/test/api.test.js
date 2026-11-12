// The sync API on PGlite, through the same request transaction as on Neon (session.js), logged in as lauwell_api.
import { test, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { PGlite } from '@electric-sql/pglite';
import { migrate } from '../db/migrations.js';
import { inRequest } from '../api/session.js';
import { pull, push } from '../api/sync.js';
import { makeHandler } from '../api/handler.js';

let db;

beforeEach(async () => {
  db = new PGlite();
  await migrate(db);
  await db.exec('SET ROLE lauwell_api');
});

const runAs = (uid, fn) => inRequest(db, uid, fn);
const pushAs = (uid, ...changes) => runAs(uid, (tx) => push(tx, { changes }));
const pullAs = (uid, body = {}) => runAs(uid, (tx) => pull(tx, body));

const T = Date.parse('2026-11-09T13:30:00.123Z');

const mood = (fields = {}) => ({
  id: randomUUID(),
  metric_id: 'mood',
  shape: 'Measurement',
  time_utc: T,
  end_time_utc: null,
  zone_offset_s: -18000,
  source: 'manual',
  recorded_at: T + 60_000,
  edited_at: null,
  deleted_at: null,
  note: null,
  schema_version: 1,
  values: { value: 7 },
  links: [],
  ...fields,
});

test('what one phone pushes, another pulls back unchanged', async () => {
  const encounter = mood({ metric_id: 'encounter_note', values: {} });
  const entry = mood({ note: 'after the walk', links: [{ to_id: encounter.id, relation: 'recorded_during' }] });
  assert.deepEqual(await pushAs('alice', encounter, entry), { accepted: [encounter.id, entry.id], rejected: [] });

  const { records, next, more } = await pullAs('alice');
  assert.deepEqual(records.map(({ change_seq, ...r }) => r), [encounter, entry]);
  assert.equal(next, records[1].change_seq);
  assert.equal(more, false);
  assert.deepEqual(await pullAs('alice', { after: next }), { records: [], next, more: false });
});

test('pull pages through the changes in order', async () => {
  const entries = [mood(), mood(), mood()];
  await pushAs('alice', ...entries);
  const first = await pullAs('alice', { limit: 2 });
  assert.equal(first.more, true);
  const second = await pullAs('alice', { after: first.next, limit: 2 });
  assert.equal(second.more, false);
  assert.deepEqual([...first.records, ...second.records].map((r) => r.id), entries.map((e) => e.id));
});

test('the latest edit wins; an older one is accepted and dropped', async () => {
  const entry = mood();
  await pushAs('alice', entry);
  const later = { ...entry, edited_at: T + 120_000, values: { value: 3 } };
  const earlier = { ...entry, edited_at: T + 90_000, values: { value: 5 } };
  await pushAs('alice', later);
  assert.deepEqual(await pushAs('alice', earlier), { accepted: [entry.id], rejected: [] });

  const [pulled] = (await pullAs('alice')).records;
  assert.deepEqual(pulled.values, { value: 3 });
  assert.equal(pulled.edited_at, later.edited_at);
});

test('the same change sent twice applies once more, with the same result', async () => {
  const entry = mood();
  await pushAs('alice', entry);
  const before = (await pullAs('alice')).records[0];
  await pushAs('alice', entry);
  const after = (await pullAs('alice')).records[0];
  assert.deepEqual({ ...after, change_seq: 0 }, { ...before, change_seq: 0 });
});

test('a deletion is final', async () => {
  const entry = mood();
  await pushAs('alice', entry);
  await pushAs('alice', { ...entry, deleted_at: T + 120_000 });
  await pushAs('alice', { ...entry, edited_at: T + 180_000, note: 'edited elsewhere' });
  const [pulled] = (await pullAs('alice')).records;
  assert.equal(pulled.deleted_at, T + 120_000);
});

test('a refused change says why, and the others in the batch still apply', async () => {
  const alices = mood();
  await pushAs('alice', alices);

  const missing = randomUUID();
  const ok = mood();
  const result = await pushAs('bob',
    mood({ shape: 'Blob' }),
    { ...alices, edited_at: T + 120_000 },
    mood({ links: [{ to_id: missing, relation: 'about' }] }),
    { id: 'not-an-id' },
    ok);
  assert.deepEqual(result.accepted, [ok.id]);
  assert.deepEqual(result.rejected.map((r) => r.reason), ['invalid', 'not_yours', 'missing_link', 'invalid']);
  assert.equal(result.rejected[3].id, null);

  assert.deepEqual((await pullAs('bob')).records.map((r) => r.id), [ok.id]);
  assert.equal((await pullAs('alice')).records[0].edited_at, null);
});

test('a link refused for a missing record passes once that record is pushed', async () => {
  const encounter = mood({ metric_id: 'encounter_note' });
  const entry = mood({ links: [{ to_id: encounter.id, relation: 'recorded_during' }] });
  assert.equal((await pushAs('alice', entry)).rejected[0].reason, 'missing_link');
  assert.deepEqual((await pushAs('alice', encounter, entry)).rejected, []);
});

test('each person pulls only their own records', async () => {
  await pushAs('alice', mood());
  assert.deepEqual((await pullAs('bob')).records, []);
});

test('malformed requests are refused as a whole', async () => {
  await assert.rejects(pushAs('alice', ...Array(501).fill(mood())), /at most 500/);
  await assert.rejects(runAs('alice', (tx) => push(tx, {})), /changes must be a list/);
  await assert.rejects(pullAs('alice', { after: -1 }), /change number/);
  await assert.rejects(pullAs('alice', { limit: 0 }), /limit/);
});

// HTTP ------------------------------------------------------------------------------------------------------

const handler = makeHandler({
  verifyToken: async (token) => {
    if (!token?.startsWith('valid:')) throw new Error('bad token');
    return { uid: token.slice('valid:'.length) };
  },
  runAs,
});

async function call(path, { method = 'POST', token = 'valid:alice', body = {} } = {}) {
  const res = {
    status(code) { this.code = code; return this; },
    json(payload) { this.body = payload; return this; },
  };
  const headers = token ? { authorization: `Bearer ${token}` } : {};
  await handler({ path, method, headers, body }, res);
  return res;
}

test('the API answers only signed-in POSTs on its two routes', async () => {
  assert.equal((await call('/pull', { token: null })).code, 401);
  assert.equal((await call('/pull', { token: 'forged' })).code, 401);
  assert.equal((await call('/pull', { method: 'GET' })).code, 405);
  assert.equal((await call('/other')).code, 404);
  assert.equal((await call('/push', { body: { changes: 'x' } })).code, 400);

  const entry = mood();
  assert.deepEqual((await call('/push', { body: { changes: [entry] } })).body, { accepted: [entry.id], rejected: [] });
  const pulled = await call('/pull', { body: { after: 0 } });
  assert.equal(pulled.code, 200);
  assert.deepEqual(pulled.body.records.map((r) => r.id), [entry.id]);
  assert.deepEqual((await call('/pull', { token: 'valid:bob' })).body.records, []);
});
