package com.laul.lauwell.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.laul.lauwell.R
import com.laul.lauwell.core.ui.theme.LauWellTheme

/** One row of a [ThingPicker]: a thing's id, its name, and an optional second line ("Pharmacy · Laurier"). */
data class PickerOption(val id: String, val label: String, val detail: String? = null)

/**
 * Picks a thing (a pharmacy, a medication, a pouch) inside a logging sheet, or adds a missing one on the
 * spot (docs/architecture/backend/shared-foundation.md → 5): a search field, the [options] in a [Panel]
 * with the selected one ticked, and a last row "+ New [kindLabel]…". That row opens a short form in place,
 * name only, prefilled with the search; Add calls [onCreate], whose caller saves the thing and selects it.
 * The rest of the sheet, and what was typed there, stays as it was.
 *
 * [options] come already filtered and ordered, with things not used anymore left out (`pickerOptions` in
 * `core.thing`), so the same rule holds in every picker.
 */
@Composable
fun ThingPicker(
    options: List<PickerOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    kindLabel: String,
    onCreate: (name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs)) {
        if (adding) {
            NewThingForm(
                kindLabel = kindLabel,
                initialName = query.trim(),
                onAdd = { name -> onCreate(name); adding = false },
                onCancel = { adding = false },
            )
        } else {
            PickerTextField(query, onQueryChange, placeholder = "Search", icon = R.drawable.ic_search)
            Panel(Modifier.selectableGroup()) {
                options.forEach { option ->
                    OptionRow(option, selected = option.id == selectedId, onClick = { onSelect(option.id) })
                    PanelDivider()
                }
                if (options.isEmpty() && query.isNotBlank()) {
                    BasicText(
                        "No $kindLabel matches “${query.trim()}”",
                        style = LauWellTheme.typography.caption.copy(color = LauWellTheme.colors.ink2),
                        modifier = Modifier.padding(vertical = LauWellTheme.spacing.sm),
                    )
                    PanelDivider()
                }
                NewRow(kindLabel, onClick = { adding = true })
            }
        }
    }
}

@Composable
private fun OptionRow(option: PickerOption, selected: Boolean, onClick: () -> Unit) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm),
    ) {
        LauWellIcon(
            icon = if (selected) R.drawable.ic_check_circle else R.drawable.ic_radio_button_unchecked,
            contentDescription = null,
            tint = if (selected) colors.ink else colors.ink2,
            size = 20.dp,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            BasicText(option.label, style = type.item.copy(color = colors.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            option.detail?.let {
                BasicText(it, style = type.caption.copy(color = colors.ink2), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NewRow(kindLabel: String, onClick: () -> Unit) {
    val colors = LauWellTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm),
    ) {
        LauWellIcon(R.drawable.ic_add, contentDescription = null, tint = colors.ink, size = 20.dp)
        BasicText("New $kindLabel…", style = LauWellTheme.typography.item.copy(color = colors.ink))
    }
}

/** The name, the only required field; the rest of the thing is filled in later from Care or Records. */
@Composable
internal fun NewThingForm(kindLabel: String, initialName: String, onAdd: (String) -> Unit, onCancel: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val focus = remember { FocusRequester() }
    Panel(contentPadding = PaddingValues(LauWellTheme.spacing.panel)) {
        Column(verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm)) {
            BasicText(
                "New $kindLabel",
                style = LauWellTheme.typography.title.copy(color = LauWellTheme.colors.ink),
            )
            PickerTextField(name, { name = it }, placeholder = "Name", modifier = Modifier.focusRequester(focus))
            Row(horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                StrokeButton("Cancel", onClick = onCancel)
                StrokeButton("Add", onClick = { onAdd(name.trim()) }, enabled = name.isNotBlank())
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** A one-line field on the panel color, with a 1.5dp `lineSoft` outline and an optional leading icon. */
@Composable
private fun PickerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val shape = LauWellTheme.shapes.control
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = type.item.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.ink),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { field ->
            Row(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .border(LauWellTheme.strokes.control, colors.lineSoft, shape)
                    .padding(horizontal = LauWellTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs),
            ) {
                icon?.let { LauWellIcon(it, contentDescription = null, tint = colors.ink2, size = 18.dp) }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) BasicText(placeholder, style = type.item.copy(color = colors.ink2))
                    field()
                }
            }
        },
    )
}
