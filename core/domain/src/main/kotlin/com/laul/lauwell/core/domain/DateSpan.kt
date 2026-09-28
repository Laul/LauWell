package com.laul.lauwell.core.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** An inclusive run of calendar days, e.g. "the last 7 days including today". */
data class DateSpan(val first: LocalDate, val last: LocalDate) : Iterable<LocalDate> {
    init {
        require(!last.isBefore(first)) { "last ($last) is before first ($first)" }
    }

    val days: List<LocalDate> get() = generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toList()

    override fun iterator(): Iterator<LocalDate> = days.iterator()

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(first) && !date.isAfter(last)

    /** Start of [first] in [zone]. Uses the zone's rules, so a DST day is 23 or 25 hours long. */
    fun startIn(zone: ZoneId): Instant = first.atStartOfDay(zone).toInstant()

    /** Start of the day after [last] in [zone] (exclusive end). */
    fun endIn(zone: ZoneId): Instant = last.plusDays(1).atStartOfDay(zone).toInstant()

    companion object {
        /** The [count] days ending on [today], today included. */
        fun lastDays(count: Int, today: LocalDate): DateSpan {
            require(count >= 1) { "count must be >= 1, was $count" }
            return DateSpan(today.minusDays(count - 1L), today)
        }
    }
}
