package com.laul.lauwell.ui

import com.laul.lauwell.core.model.Metric
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A value with the metric's own precision: glucose 5.4, heart rate 72. */
fun Metric.format(value: Double): String = "%.${decimals}f".format(value)

fun formatTime(time: Instant, zone: ZoneId): String = time.atZone(zone).format(TIME)

private val TIME = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm")
