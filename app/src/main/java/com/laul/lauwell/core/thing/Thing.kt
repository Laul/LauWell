package com.laul.lauwell.core.thing

import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Things: what acts point at, entered once and reused (docs/architecture/backend/shared-foundation.md → 1, 4).
// Two families that never mix, one table per kind: Products (what I take or use: medication, ostomy
// product, device) and Resources (who and where care comes from: care provider, care facility, supplier).
// Each kind arrives with its feature as a class implementing [Product] or [Resource].

/** Identifier of a thing: a UUID made on the phone, so a thing can be created offline. */
@JvmInline
value class ThingId(val value: String) {
    init {
        require(value.isNotBlank()) { "ThingId must not be blank" }
    }

    override fun toString(): String = value
}

/** The fields every thing carries, in both families. */
sealed interface Thing {
    val id: ThingId

    /** What lists and pickers show: "Ramipril 5 mg", "Pharmaprix Mont-Royal". */
    val name: String
    val note: String?
    val createdAt: Instant

    /** Last change; null when never edited. */
    val editedAt: Instant?

    /**
     * When it got the [NotUsedAnymore] tag; null while in use. A thing an entry points to is never
     * deleted: it keeps its place in its list, with the tag, and is no longer offered when logging.
     */
    val notUsedSince: Instant?
}

/** What I take or use: a medication, an ostomy product, a device. */
interface Product : Thing {
    /** Brand or maker. */
    val brand: String?

    /** DIN, product code. */
    val referenceCode: String?

    /** Tablet, drops, two-piece pouch… */
    val form: String?
}

/** Who and where care comes from: a care provider, a care facility (labs included), a supplier. */
interface Resource : Thing {
    /** GP, nurse… / clinic, hospital, lab… / pharmacy, ostomy store… */
    val type: String?
    val address: String?
    val phone: String?
    val website: String?
    val contactPerson: String?
}

val Thing.inUse: Boolean get() = notUsedSince == null

/** The tag's wording, the same for every kind of thing, medications included. */
const val NotUsedAnymore = "Not used anymore"

/** "Not used anymore · since Tue 3 Nov", or null while in use. */
fun notUsedLabel(thing: Thing, zone: ZoneId): String? =
    thing.notUsedSince?.let { "$NotUsedAnymore · since ${it.atZone(zone).format(SinceFormat)}" }

private val SinceFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

/**
 * What a picker offers for [query]: things whose name contains it (ignoring case and accents), most
 * recently used first ([lastUsed], null for never), then by name. Things not used anymore are left out,
 * unless [includeNotUsed] (starting a medication course again keeps one history instead of a duplicate);
 * then they come after the ones in use.
 */
fun <T : Thing> pickerOptions(
    things: List<T>,
    query: String,
    lastUsed: (ThingId) -> Instant?,
    includeNotUsed: Boolean = false,
): List<T> {
    val needle = searchKey(query.trim())
    return things
        .filter { (includeNotUsed || it.inUse) && searchKey(it.name).contains(needle) }
        .sortedWith(
            compareBy<T> { !it.inUse }
                .thenByDescending { lastUsed(it.id) ?: Instant.MIN }
                .thenBy { searchKey(it.name) },
        )
}

/** Lower case without accents, so "pharmaprix" finds "Pharmaprix" and "creme" finds "Crème". */
private fun searchKey(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Accents, "").lowercase(Locale.ROOT)

private val Accents = Regex("\\p{Mn}+")
