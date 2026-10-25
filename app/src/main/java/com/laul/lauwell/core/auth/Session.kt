package com.laul.lauwell.core.auth

/**
 * The signed-in user, as the rest of the app sees it.
 *
 * [uid] is the Firebase user id: stable for the life of the account (unlike an email) and the root
 * of every Firestore path (`users/{uid}/…`).
 *
 * Deliberately minimal. No tokens: Firebase stores and refreshes those itself, so they can't leak
 * into UI state, logs or crash reports. No name yet (Notion: *Decide Today greeting name*).
 *
 * @param email the Google account's address, shown in Settings › Account; null if Firebase has none.
 */
data class Session(
    val uid: String,
    val email: String? = null,
)
