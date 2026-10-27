package com.laul.lauwell.feature.log

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.common.AppResult
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.ui.components.CategoryLine
import com.laul.lauwell.core.ui.components.FilledButton
import com.laul.lauwell.core.ui.components.LauWellBottomSheet
import com.laul.lauwell.core.ui.components.LauWellIcon
import com.laul.lauwell.core.ui.components.PanelDivider
import com.laul.lauwell.core.ui.components.PipRating
import com.laul.lauwell.core.ui.theme.LauWellTheme
import java.time.Clock
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The quick-log sheet (design: Today › Quick log, drawn for fatigue): category line, title and
 * question, the rating's pips, when it was (now, or Change), an optional note, and Save · n/5.
 * Opens for [metric], closes when it becomes null; each opening starts blank.
 */
@Composable
fun LogEntrySheet(metric: MetricDefinition?, onDismiss: () -> Unit) {
    val viewModel = appViewModel { LogEntryViewModel(recordRepository, Clock.systemDefaultZone()) }
    // The last metric shown, kept while the sheet slides away.
    var shown by remember { mutableStateOf<MetricDefinition?>(null) }
    var opening by remember { mutableIntStateOf(0) }
    LaunchedEffect(metric) {
        if (metric != null) {
            shown = metric
            opening++
        }
    }
    LauWellBottomSheet(visible = metric != null, onDismissRequest = onDismiss) {
        val current = shown ?: return@LauWellBottomSheet
        key(opening) {
            RatingForm(current, onSave = { value, at, note ->
                val result = viewModel.saveRating(current, value, at, note)
                if (result is AppResult.Success) onDismiss()
                result
            })
        }
    }
}

@Composable
private fun RatingForm(metric: MetricDefinition, onSave: (Int, LocalTime?, String) -> AppResult<Unit>) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val prompt = requireNotNull(metric.prompt) { "${metric.id} has no rating prompt" }
    val scale = requireNotNull(metric.quantities.single().scale)
    var value by remember { mutableStateOf<Int?>(null) }
    var at by remember { mutableStateOf<LocalTime?>(null) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xxs)) {
        CategoryLine(metric.category, "${metric.category.label} · ${metric.subCategory.label}")
        BasicText(metric.label, style = type.headline.copy(color = colors.ink))
        BasicText(prompt.question, style = type.body.copy(color = colors.ink2))
    }
    PipRating(value, { value = it }, metric.category, prompt.lowLabel, prompt.highLabel, scale = scale)
    Column {
        PanelDivider()
        TimeRow(at, onChange = { at = it })
        PanelDivider()
        NoteRow(note, onChange = { note = it })
    }
    error?.let { BasicText(it, style = type.caption.copy(color = colors.ink2)) }
    FilledButton(
        text = "Save",
        onClick = {
            val result = onSave(value ?: return@FilledButton, at, note)
            error = (result as? AppResult.Error)?.message
        },
        enabled = value != null,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText("Save ·", style = type.item.copy(color = colors.onAccent))
            BasicText("${value ?: "–"}/${scale.last}", style = type.metricSmall.copy(color = colors.onAccent))
        }
    }
}

/** "Now · 09:14", or the chosen time ("Yesterday · 23:30"); Change opens the system time picker. */
@Composable
private fun TimeRow(at: LocalTime?, onChange: (LocalTime) -> Unit) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val context = LocalContext.current
    val now = LocalTime.now()
    val shown = at ?: now
    val label = when {
        at == null -> "Now"
        at.isAfter(now) -> "Yesterday"
        else -> "Today"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClickLabel = "Change the time") {
                TimePickerDialog(context, { _, h, m -> onChange(LocalTime.of(h, m)) }, shown.hour, shown.minute, DateFormat.is24HourFormat(context)).show()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm),
    ) {
        LauWellIcon(R.drawable.ic_schedule, contentDescription = null, tint = colors.ink2, size = 20.dp)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            BasicText("$label · ", style = type.item.copy(color = colors.ink))
            BasicText(shown.format(TimeFormat), style = type.metricSmall.copy(color = colors.ink))
        }
        BasicText("Change", style = type.control.copy(color = colors.ink))
    }
}

/** "Add a note" until tapped, then a text field (the sheet rises above the keyboard). */
@Composable
private fun NoteRow(note: String, onChange: (String) -> Unit) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    var editing by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (editing) Modifier else Modifier.clickable(role = Role.Button) { editing = true }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm),
    ) {
        LauWellIcon(R.drawable.ic_notes, contentDescription = null, tint = colors.ink2, size = 20.dp)
        if (editing) {
            BasicTextField(
                value = note,
                onValueChange = onChange,
                textStyle = type.item.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.ink),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
                decorationBox = { field ->
                    if (note.isEmpty()) BasicText("Add a note", style = type.item.copy(color = colors.ink2))
                    field()
                },
            )
            LaunchedEffect(Unit) { focus.requestFocus() }
        } else {
            BasicText("Add a note", style = type.item.copy(color = colors.ink2), modifier = Modifier.weight(1f))
        }
    }
}

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
