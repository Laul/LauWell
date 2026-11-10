// The migrations in db/migrations, applied in file-name order, each once and in its own transaction.
// Shared by the runner (Neon) and the tests (PGlite); `db` only needs query(sql) and exec(sql).
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const DIR = new URL('./migrations/', import.meta.url).pathname;

export const migrationFiles = () => readdirSync(DIR).filter((f) => f.endsWith('.sql')).sort();

/** Applies the migrations `db` hasn't had yet; returns their names. */
export async function migrate(db) {
  await db.exec('CREATE TABLE IF NOT EXISTS schema_migrations (name text PRIMARY KEY, applied_at timestamptz NOT NULL DEFAULT now())');
  const done = new Set((await db.query('SELECT name FROM schema_migrations')).rows.map((r) => r.name));
  const applied = [];
  for (const name of migrationFiles().filter((f) => !done.has(f))) {
    const sql = readFileSync(join(DIR, name), 'utf8');
    await db.exec(`BEGIN;\n${sql}\nINSERT INTO schema_migrations (name) VALUES ('${name}');\nCOMMIT;`);
    applied.push(name);
  }
  return applied;
}
