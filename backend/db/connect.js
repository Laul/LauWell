// Connection settings for Neon, shared by the migration runner and the API.

/** Whether `url` is a Postgres URL: anything else (a quoted URL, a psql snippet) fails later as "ENOTFOUND base". */
export const isPostgresUrl = (url) => /^postgres(ql)?:\/\//.test(url ?? '');

/**
 * pg client settings for `url`. TLS with the certificate checked (verify-full) comes from `ssl`, not from Neon's
 * sslmode=require, which pg 9 will weaken to libpq's meaning (encrypted, certificate unchecked).
 */
export function connectionConfig(url) {
  const address = new URL(url);
  address.searchParams.delete('sslmode');
  return { connectionString: address.href, ssl: { rejectUnauthorized: true } };
}
