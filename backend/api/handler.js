// The HTTP side of the API: POST /push and POST /pull, each with "Authorization: Bearer <Firebase ID token>".
// Dependencies come in, so the tests run it on PGlite with a fake token check:
//   verifyToken(token) → { uid }, throws when the token is missing, expired or forged
//   runAs(uid, fn)     → fn(tx) inside that person's request transaction (session.js)
// Logs carry only the operation and an error code, never a body: no health data in logs.
import { BadRequest, pull, push } from './sync.js';

const ROUTES = { '/push': push, '/pull': pull };

export function makeHandler({ verifyToken, runAs }) {
  return async (req, res) => {
    const op = ROUTES[req.path];
    if (!op) return res.status(404).json({ error: 'not found' });
    if (req.method !== 'POST') return res.status(405).json({ error: 'use POST' });

    const token = /^Bearer (.+)$/.exec(req.headers.authorization ?? '')?.[1];
    let uid;
    try {
      ({ uid } = await verifyToken(token));
    } catch {
      return res.status(401).json({ error: 'sign in again' });
    }

    try {
      return res.status(200).json(await runAs(uid, (tx) => op(tx, req.body)));
    } catch (error) {
      if (error instanceof BadRequest) return res.status(400).json({ error: error.message });
      console.error(`${req.path} failed: ${error.code ?? error.name}`);
      return res.status(500).json({ error: 'server error' });
    }
  };
}
