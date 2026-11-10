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
const client = new pg.Client({ connectionString: url, ssl: { rejectUnauthorized: true } });
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
