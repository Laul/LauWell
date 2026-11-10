// The schema on PGlite (real Postgres, in process): migrations, identity, row-level security and sync numbering.
// Every request runs as the API role, as the cloud function will: lauwell_api + app.begin_request(uid).
import { test, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { PGlite } from '@electric-sql/pglite';
import { migrate, migrationFiles } from '../db/migrations.js';

let db;

beforeEach(async () => {
  db = new PGlite();
  await migrate(db);
});

/** Runs fn(tx) in one transaction as the API, signed in as Firebase uid `uid` (none when null). */
async function as(uid, fn) {
  return db.transaction(async (tx) => {
    await tx.exec('SET LOCAL ROLE lauwell_api');
    if (uid !== null) await tx.query('SELECT app.begin_request($1)', [uid]);
    return fn(tx);
  });
}

const rejects = (uid, sql, params, pattern) => assert.rejects(as(uid, (tx) => tx.query(sql, params)), pattern);

const insertRecord = (tx, id, fields = {}) =>
  tx.query(
    `INSERT INTO record (id, metric_id, shape, time_utc, source, recorded_at, schema_version, note)
     VALUES ($1, $2, 'Measurement', '2026-11-09T13:30:00Z', 'manual', '2026-11-09T13:31:00Z', 1, $3)`,
    [id, fields.metric ?? 'mood', fields.note ?? null],
  );

const seqOf = async (tx, id) => (await tx.query('SELECT change_seq FROM record WHERE id = $1', [id])).rows[0].change_seq;

test('migrations apply once', async () => {
  assert.deepEqual(await migrate(db), []);
  const { rows } = await db.query('SELECT name FROM schema_migrations ORDER BY name');
  assert.deepEqual(rows.map((r) => r.name), migrationFiles());
});

test('a first sign-in creates the person, later ones find the same one', async () => {
  const first = await as(null, (tx) => tx.query("SELECT app.begin_request('alice') AS id"));
  const again = await as(null, (tx) => tx.query("SELECT app.begin_request('alice') AS id"));
  const other = await as(null, (tx) => tx.query("SELECT app.begin_request('bob') AS id"));
  assert.equal(first.rows[0].id, again.rows[0].id);
  assert.notEqual(first.rows[0].id, other.rows[0].id);
  await rejects(null, "SELECT app.begin_request('')", [], /no signed-in user/);
});

test('each person sees and changes only their own records', async () => {
  const id = randomUUID();
  await as('alice', (tx) => insertRecord(tx, id));

  assert.equal((await as('alice', (tx) => tx.query('SELECT * FROM record'))).rows.length, 1);
  assert.equal((await as('bob', (tx) => tx.query('SELECT * FROM record'))).rows.length, 0);

  const updated = await as('bob', (tx) => tx.query("UPDATE record SET note = 'mine' WHERE id = $1", [id]));
  assert.equal(updated.affectedRows, 0);

  // Writing over Alice's id from Bob's account fails instead of overwriting it.
  await assert.rejects(
    as('bob', (tx) => tx.query(
      `INSERT INTO record (id, metric_id, shape, time_utc, source, recorded_at, schema_version)
       VALUES ($1, 'mood', 'Measurement', now(), 'manual', now(), 1)
       ON CONFLICT (id) DO UPDATE SET note = 'mine'`, [id])),
    /row-level security/,
  );
  const note = await as('alice', (tx) => tx.query('SELECT note FROM record WHERE id = $1', [id]));
  assert.equal(note.rows[0].note, null);
});

test("no one can write a row in someone else's name", async () => {
  const bob = (await as(null, (tx) => tx.query("SELECT app.begin_request('bob') AS id"))).rows[0].id;
  await rejects('alice',
    `INSERT INTO record (id, user_id, metric_id, shape, time_utc, source, recorded_at, schema_version)
     VALUES ($1, $2, 'mood', 'Measurement', now(), 'manual', now(), 1)`,
    [randomUUID(), bob], /row-level security/);
});

test("values and links cannot point at someone else's record", async () => {
  const alices = randomUUID();
  const bobs = randomUUID();
  await as('alice', (tx) => insertRecord(tx, alices));
  await as('bob', (tx) => insertRecord(tx, bobs));

  await rejects('bob', "INSERT INTO record_value (record_id, quantity_key, value) VALUES ($1, 'value', 7)", [alices], /foreign key/);
  await rejects('bob', "INSERT INTO record_link (from_id, to_id, relation) VALUES ($1, $2, 'about')", [bobs, alices], /foreign key/);
});

test('without a signed-in person nothing is read or written', async () => {
  await as('alice', (tx) => insertRecord(tx, randomUUID()));
  assert.equal((await as(null, (tx) => tx.query('SELECT * FROM record'))).rows.length, 0);
  await rejects(null,
    `INSERT INTO record (id, metric_id, shape, time_utc, source, recorded_at, schema_version)
     VALUES ($1, 'mood', 'Measurement', now(), 'manual', now(), 1)`, [randomUUID()], /null value|row-level security/);
});

test('the API cannot read identities or delete acts', async () => {
  const id = randomUUID();
  await as('alice', (tx) => insertRecord(tx, id));
  await rejects('alice', 'SELECT * FROM users', [], /permission denied/);
  await rejects('alice', 'SELECT * FROM auth_identity', [], /permission denied/);
  await rejects('alice', 'DELETE FROM record WHERE id = $1', [id], /permission denied/);
});

test('every change takes a new number, including a change to its values or links', async () => {
  const mood = randomUUID();
  const encounter = randomUUID();
  await as('alice', async (tx) => {
    await insertRecord(tx, encounter, { metric: 'encounter_note' });
    await insertRecord(tx, mood);
  });
  const created = await as('alice', (tx) => seqOf(tx, mood));

  await as('alice', (tx) => tx.query("INSERT INTO record_value (record_id, quantity_key, value) VALUES ($1, 'value', 7)", [mood]));
  const valued = await as('alice', (tx) => seqOf(tx, mood));
  assert.ok(valued > created);

  await as('alice', (tx) => tx.query("INSERT INTO record_link (from_id, to_id, relation) VALUES ($1, $2, 'recorded_during')", [mood, encounter]));
  const linked = await as('alice', (tx) => seqOf(tx, mood));
  assert.ok(linked > valued);

  await as('alice', (tx) => tx.query('UPDATE record SET deleted_at = now() WHERE id = $1', [mood]));
  const deleted = await as('alice', (tx) => seqOf(tx, mood));
  assert.ok(deleted > linked);

  // What the phone pulls after having seen `created`: the mood, once, as deleted; not the untouched encounter.
  const pulled = await as('alice', (tx) =>
    tx.query('SELECT id, deleted_at FROM record WHERE change_seq > $1 ORDER BY change_seq', [created]));
  assert.deepEqual(pulled.rows.map((r) => r.id), [mood]);
  assert.ok(pulled.rows[0].deleted_at);
});

test('rows that cannot be right are refused', async () => {
  await rejects('alice',
    `INSERT INTO record (id, metric_id, shape, time_utc, source, recorded_at, schema_version)
     VALUES ($1, 'mood', 'Blob', now(), 'manual', now(), 1)`, [randomUUID()], /check constraint/);
  await rejects('alice',
    `INSERT INTO record (id, metric_id, shape, time_utc, end_time_utc, source, recorded_at, schema_version)
     VALUES ($1, 'course', 'Interval', '2026-11-09', '2026-11-01', 'manual', now(), 1)`, [randomUUID()], /check constraint/);
  const id = randomUUID();
  await as('alice', (tx) => insertRecord(tx, id));
  await rejects('alice', "INSERT INTO record_value (record_id, quantity_key, value) VALUES ($1, 'value', 'NaN')", [id], /check constraint/);
});
