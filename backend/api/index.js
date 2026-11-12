// The LauWell API as a Firebase Cloud Function (2nd gen) in the lauwell-app project, next to Firebase Auth.
// DATABASE_URL is lauwell_api's pooled Neon connection string, kept in Secret Manager
// (`firebase functions:secrets:set DATABASE_URL`), never in the repo.
import { initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { defineSecret } from 'firebase-functions/params';
import { onRequest } from 'firebase-functions/v2/https';
import pg from 'pg';
import { connectionConfig } from '../db/connect.js';
import { makeHandler } from './handler.js';
import { inRequest } from './session.js';

const DATABASE_URL = defineSecret('DATABASE_URL');

initializeApp();

// One small pool per instance, made on the first request (the secret is only readable at run time).
let pool;

async function runAs(uid, fn) {
  pool ??= new pg.Pool({ ...connectionConfig(DATABASE_URL.value()), max: 2 });
  const client = await pool.connect();
  try {
    return await inRequest(client, uid, fn);
  } finally {
    client.release();
  }
}

// Near Neon's US East region. Public at the network level: every call is checked against its Firebase ID token.
export const api = onRequest(
  { region: 'us-east4', secrets: [DATABASE_URL], invoker: 'public', maxInstances: 2 },
  makeHandler({ verifyToken: (token) => getAuth().verifyIdToken(token), runAs }),
);
