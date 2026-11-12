// Applies pending migrations to the database in DATABASE_URL, as its owner role:
//   DATABASE_URL='postgres://…' npm run migrate
// Set the variable in your shell only for this command; never commit or paste the connection string.
import pg from 'pg';
import { migrate } from './migrations.js';

const url = process.env.DATABASE_URL;
if (!url) {
  console.error('DATABASE_URL is not set');
  process.exit(1);
}
// pg reads anything else as a relative address and fails with "getaddrinfo ENOTFOUND base".
// The message never echoes the value: it holds the password.
if (!/^postgres(ql)?:\/\//.test(url)) {
  console.error("DATABASE_URL must start with postgresql:// (paste the URL only: no quotes, no 'psql', no 'DATABASE_URL=')");
  process.exit(1);
}
// TLS with the certificate checked (verify-full) comes from `ssl` below, not from Neon's sslmode=require, which
// pg 9 will weaken to libpq's meaning (encrypted, certificate unchecked).
const address = new URL(url);
address.searchParams.delete('sslmode');
const client = new pg.Client({ connectionString: address.href, ssl: { rejectUnauthorized: true } });
await client.connect();
try {
  const applied = await migrate({ query: (sql) => client.query(sql), exec: (sql) => client.query(sql) });
  console.log(applied.length ? `applied: ${applied.join(', ')}` : 'up to date');
} catch (error) {
  await client.query('ROLLBACK').catch(() => {});
  console.error(error.message);
  process.exitCode = 1;
} finally {
  await client.end();
}
