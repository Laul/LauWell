package com.laul.lauwell.core.domain

import com.laul.lauwell.core.model.Measurement
import java.time.Instant

/** One reading on a time axis. Time stays an [Instant]; conversion to chart units happens later. */
data class TimePoint(val time: Instant, val value: Double)

/** All readings in time order, for the raw timeline chart. */
fun Iterable<Measurement>.timeline(): List<TimePoint> =
    map { TimePoint(it.start, it.value) }.sortedBy { it.time }
