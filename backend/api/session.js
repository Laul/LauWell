// One API request = one transaction acting for one person: BEGIN, app.begin_request(uid), the work, COMMIT.
// `client` is a connection logged in as lauwell_api (a pg pool client on Neon, PGlite in the tests).

/** Runs fn(client) for the signed-in Firebase `uid`, inside one transaction; rolls back if it throws. */
export async function inRequest(client, uid, fn) {
  await client.query('BEGIN');
  try {
    await client.query('SELECT app.begin_request($1)', [uid]);
    const result = await fn(client);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  }
}
