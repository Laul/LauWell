package com.laul.lauwell.core.record

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.laul.lauwell.core.auth.Session
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.common.AppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * The records you enter yourself (mood now; fatigue, blood pressure, medication… later), kept in
 * your account. Health Connect data never goes through here (Notion → Decisions Tracker: *Data
 * placement*). Implementation: [FirestoreRecordRepository].
 */
interface RecordRepository {

    /** A fresh id for a record about to be [save]d. */
    fun newId(): RecordId

    /**
     * Every [metric] record, oldest first, updated live (including your own writes made offline).
     * Empty while signed out.
     */
    fun measurements(metric: MetricId): Flow<AppResult<List<Measurement>>>

    /** Creates or replaces [record]. Succeeds once written locally; Firestore syncs it when online. */
    fun save(record: Measurement): AppResult<Unit>

    fun delete(id: RecordId): AppResult<Unit>
}

/**
 * [RecordRepository] on Cloud Firestore: `users/{uid}/records/{id}`, laid out by `RecordDocument.kt`.
 *
 * Writes don't wait for the server: Firestore's offline cache applies them at once (the live
 * [measurements] see them) and sends them when it can. Waiting would hang the UI while offline.
 * The rules allow only the owner's uid (`firebase/firestore.rules`). Never logs record contents.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreRecordRepository(
    private val firestore: FirebaseFirestore,
    private val session: StateFlow<Session?>,
) : RecordRepository {

    override fun newId(): RecordId = RecordId(firestore.collection(RecordsCollection).document().id)

    override fun measurements(metric: MetricId): Flow<AppResult<List<Measurement>>> =
        session.flatMapLatest { current ->
            if (current == null) flowOf(AppResult.Success(emptyList()))
            else callbackFlow {
                // One equality filter needs no composite index; sorting by time is done here.
                val registration = records(current.uid)
                    .whereEqualTo(RecordFields.Metric, metric.value)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(AppResult.Error(error, "Couldn't read your ${metric.value} entries"))
                        } else if (snapshot != null) {
                            val list = snapshot.documents
                                .mapNotNull { measurementFrom(it.id, it.data.orEmpty()) }
                                .sortedBy { it.time }
                            trySend(AppResult.Success(list))
                        }
                    }
                awaitClose { registration.remove() }
            }
        }

    override fun save(record: Measurement): AppResult<Unit> = withUser { uid ->
        records(uid).document(record.id.value).set(record.toDocument())
    }

    override fun delete(id: RecordId): AppResult<Unit> = withUser { uid ->
        records(uid).document(id.value).delete()
    }

    private fun records(uid: String): CollectionReference =
        firestore.collection(UsersCollection).document(uid).collection(RecordsCollection)

    private inline fun withUser(write: (uid: String) -> Unit): AppResult<Unit> {
        val uid = session.value?.uid ?: return AppResult.Error(IllegalStateException("Signed out"), "You're signed out")
        return runCatching { write(uid) }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(it, "Couldn't save the change") },
        )
    }

    private companion object {
        const val UsersCollection = "users"
        const val RecordsCollection = "records"
    }
}
