// The sync API's two operations (decision "Sync: our own"), run inside a request's transaction (session.js), so
// row-level security already limits them to the signed-in person.
//
// Wire format: a record with the Room / Postgres column names, times in epoch milliseconds, its numbers as
// { quantity_key: value } and its links as [{ to_id, relation }]:
//   push { changes: [record…] }   → { accepted: [id…], rejected: [{ id, reason }…] }
//   pull { after, limit? }        → { records: [record + change_seq…], next, more }
//
// Latest edit wins: a change applies when its last moment (recorded, edited or deleted) is at least the stored
// one's, so a resent change applies again and an older one is accepted but dropped. A deletion is final: a later
// edit doesn't bring the record back.

export const MAX_CHANGES = 500;
export const MAX_PULL = 500;

/** A malformed request as a whole: the API answers 400 with the message. */
export class BadRequest extends Error {}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const isId = (v) => typeof v === 'string' && UUID.test(v);
const isText = (v) => typeof v === 'string' && v !== '';
const isInt = Number.isSafeInteger;
const isOptional = (check) => (v) => v == null || check(v);

const FIELDS = {
  id: isId,
  metric_id: isText,
  shape: isText,
  time_utc: isInt,
  end_time_utc: isOptional(isInt),
  zone_offset_s: isOptional(isInt),
  source: isText,
  recorded_at: isInt,
  edited_at: isOptional(isInt),
  deleted_at: isOptional(isInt),
  note: isOptional((v) => typeof v === 'string'),
  schema_version: isInt,
};

/** Whether `change` has the wire format; the database checks the rest (shape and source names, ranges). */
function wellFormed(change) {
  if (change === null || typeof change !== 'object') return false;
  if (!Object.entries(FIELDS).every(([name, check]) => check(change[name]))) return false;
  const values = change.values ?? {};
  const links = change.links ?? [];
  return typeof values === 'object' && !Array.isArray(values)
    && Object.values(values).every(Number.isFinite)
    && Array.isArray(links)
    && links.every((l) => l !== null && typeof l === 'object' && isId(l.to_id) && isText(l.relation));
}

// Postgres error codes → why a change was refused. `missing_link` may pass later, once the linked record is pushed;
// the others never will.
const REASONS = {
  '42501': 'not_yours', // row-level security: the id belongs to someone else
  '23503': 'missing_link', // foreign key: a link to a record the server doesn't have (yet)
};
const reasonOf = (error) => REASONS[error.code] ?? (error.code?.startsWith('2') ? 'invalid' : null);

const toTime = (n) => `to_timestamp($${n}::float8 / 1000)`;

const UPSERT = `
  INSERT INTO record AS r (id, metric_id, shape, time_utc, end_time_utc, zone_offset_s, source, recorded_at,
                           edited_at, deleted_at, note, schema_version)
  VALUES ($1, $2, $3, ${toTime(4)}, ${toTime(5)}, $6, $7, ${toTime(8)},
          ${toTime(9)}, ${toTime(10)}, $11, $12)
  ON CONFLICT (id) DO UPDATE SET
    metric_id = excluded.metric_id, shape = excluded.shape, time_utc = excluded.time_utc,
    end_time_utc = excluded.end_time_utc, zone_offset_s = excluded.zone_offset_s, source = excluded.source,
    recorded_at = excluded.recorded_at, edited_at = excluded.edited_at,
    deleted_at = coalesce(r.deleted_at, excluded.deleted_at), note = excluded.note,
    schema_version = excluded.schema_version
  WHERE greatest(excluded.recorded_at, excluded.edited_at, excluded.deleted_at)
     >= greatest(r.recorded_at, r.edited_at, r.deleted_at)
  RETURNING id`;

/** Applies one change: the record, then its values and links replaced as a whole. False when an older edit. */
async function apply(tx, c) {
  const { rows } = await tx.query(UPSERT, [
    c.id, c.metric_id, c.shape, c.time_utc, c.end_time_utc ?? null, c.zone_offset_s ?? null, c.source,
    c.recorded_at, c.edited_at ?? null, c.deleted_at ?? null, c.note ?? null, c.schema_version,
  ]);
  if (rows.length === 0) return false;
  await tx.query('DELETE FROM record_value WHERE record_id = $1', [c.id]);
  for (const [key, value] of Object.entries(c.values ?? {})) {
    await tx.query('INSERT INTO record_value (record_id, quantity_key, value) VALUES ($1, $2, $3)', [c.id, key, value]);
  }
  await tx.query('DELETE FROM record_link WHERE from_id = $1', [c.id]);
  for (const link of c.links ?? []) {
    await tx.query('INSERT INTO record_link (from_id, to_id, relation) VALUES ($1, $2, $3)', [c.id, link.to_id, link.relation]);
  }
  return true;
}

/** Applies each change on its own (a savepoint each), so one refused change doesn't hold back the others. */
export async function push(tx, body) {
  const changes = body?.changes;
  if (!Array.isArray(changes)) throw new BadRequest('changes must be a list');
  if (changes.length > MAX_CHANGES) throw new BadRequest(`at most ${MAX_CHANGES} changes per push`);

  const accepted = [];
  const rejected = [];
  for (const change of changes) {
    if (!wellFormed(change)) {
      rejected.push({ id: isId(change?.id) ? change.id : null, reason: 'invalid' });
      continue;
    }
    await tx.query('SAVEPOINT change');
    try {
      await apply(tx, change);
      await tx.query('RELEASE SAVEPOINT change');
      accepted.push(change.id);
    } catch (error) {
      const reason = reasonOf(error);
      if (!reason) throw error;
      await tx.query('ROLLBACK TO SAVEPOINT change');
      rejected.push({ id: change.id, reason });
    }
  }
  return { accepted, rejected };
}

const ms = (column) => `(extract(epoch FROM ${column}) * 1000)::bigint AS ${column}`;

const PULL = `
  SELECT id, metric_id, shape, ${ms('time_utc')}, ${ms('end_time_utc')}, zone_offset_s, source, ${ms('recorded_at')},
         ${ms('edited_at')}, ${ms('deleted_at')}, note, schema_version, change_seq
  FROM record WHERE change_seq > $1 ORDER BY change_seq LIMIT $2`;

const num = (v) => (v == null ? null : Number(v));

/** The records changed after change number `after`, oldest change first, with their values and links. */
export async function pull(tx, body) {
  const after = body?.after ?? 0;
  const limit = body?.limit ?? MAX_PULL;
  if (!isInt(after) || after < 0) throw new BadRequest('after must be a change number');
  if (!isInt(limit) || limit < 1 || limit > MAX_PULL) throw new BadRequest(`limit must be 1 to ${MAX_PULL}`);

  const { rows } = await tx.query(PULL, [after, limit]);
  const records = rows.map((r) => ({
    ...r,
    time_utc: num(r.time_utc),
    end_time_utc: num(r.end_time_utc),
    recorded_at: num(r.recorded_at),
    edited_at: num(r.edited_at),
    deleted_at: num(r.deleted_at),
    change_seq: num(r.change_seq),
    values: {},
    links: [],
  }));
  if (records.length > 0) {
    const byId = new Map(records.map((r) => [r.id, r]));
    const ids = [...byId.keys()];
    const values = await tx.query('SELECT record_id, quantity_key, value FROM record_value WHERE record_id = ANY($1::uuid[])', [ids]);
    for (const v of values.rows) byId.get(v.record_id).values[v.quantity_key] = v.value;
    const links = await tx.query(
      'SELECT from_id, to_id, relation FROM record_link WHERE from_id = ANY($1::uuid[]) ORDER BY to_id, relation', [ids]);
    for (const l of links.rows) byId.get(l.from_id).links.push({ to_id: l.to_id, relation: l.relation });
  }
  return { records, next: records.at(-1)?.change_seq ?? after, more: records.length === limit };
}
