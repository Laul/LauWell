// Emulator tests for firestore.rules. Run from firebase/: `npm test`
// (starts the Firestore emulator, runs these with node's built-in test runner, stops it).
//
// Uses a "demo-" project id, so nothing here can ever touch the real lauwell-app project.

import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, setDoc } from 'firebase/firestore';

const ALICE = 'alice';
const BOB = 'bob';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-lauwell',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  // Seed Alice's data with rules disabled, so read/delete tests have something to target.
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, `users/${ALICE}`), { createdAt: 1 });
    await setDoc(doc(db, `users/${ALICE}/weight/w1`), { value: 60 });
  });
});

const asAlice = () => env.authenticatedContext(ALICE).firestore();
const asBob = () => env.authenticatedContext(BOB).firestore();
const asAnonymous = () => env.unauthenticatedContext().firestore();

describe('owner', () => {
  test('can read and write their own user document', async () => {
    await assertSucceeds(getDoc(doc(asAlice(), `users/${ALICE}`)));
    await assertSucceeds(setDoc(doc(asAlice(), `users/${ALICE}`), { createdAt: 2 }));
  });

  test('can create, read and delete documents in their own collections', async () => {
    const db = asAlice();
    await assertSucceeds(setDoc(doc(db, `users/${ALICE}/weight/w2`), { value: 61 }));
    await assertSucceeds(getDoc(doc(db, `users/${ALICE}/weight/w1`)));
    await assertSucceeds(deleteDoc(doc(db, `users/${ALICE}/weight/w1`)));
  });

  test('can use deeply nested paths', async () => {
    const path = `users/${ALICE}/ostomy/appliance1/changes/c1`;
    await assertSucceeds(setDoc(doc(asAlice(), path), { at: 1 }));
  });
});

describe('another signed-in user', () => {
  test('cannot read the owner\'s data', async () => {
    await assertFails(getDoc(doc(asBob(), `users/${ALICE}`)));
    await assertFails(getDoc(doc(asBob(), `users/${ALICE}/weight/w1`)));
  });

  test('cannot write or delete the owner\'s data', async () => {
    await assertFails(setDoc(doc(asBob(), `users/${ALICE}/weight/w9`), { value: 1 }));
    await assertFails(deleteDoc(doc(asBob(), `users/${ALICE}/weight/w1`)));
  });
});

describe('signed-out access', () => {
  test('is denied everywhere', async () => {
    await assertFails(getDoc(doc(asAnonymous(), `users/${ALICE}`)));
    await assertFails(getDoc(doc(asAnonymous(), `users/${ALICE}/weight/w1`)));
    await assertFails(setDoc(doc(asAnonymous(), `users/${ALICE}/weight/w9`), { value: 1 }));
  });
});

describe('outside users/{uid}', () => {
  test('top-level collections are denied, even to a signed-in user', async () => {
    await assertFails(getDoc(doc(asAlice(), 'config/app')));
    await assertFails(setDoc(doc(asAlice(), 'config/app'), { x: 1 }));
  });
});
