package io.github.rim95dev.island.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.rim95dev.island.R
import io.github.rim95dev.island.apps.LauncherApp
import io.github.rim95dev.island.settings.KeywordRule
import io.github.rim95dev.island.settings.KeywordRuleMode

/** One app's keyword rule in the settings list. [app] is null when the app is no longer installed. */
@Composable
internal fun KeywordRuleRow(app: LauncherApp?, packageName: String, rule: KeywordRule, onClick: () -> Unit) {
    val summaryRes = when (rule.mode) {
        KeywordRuleMode.HIDE_MATCHING -> R.string.keyword_rule_hide_summary
        KeywordRuleMode.SHOW_MATCHING -> R.string.keyword_rule_show_summary
    }
    AppRow(
        app = app,
        label = app?.label ?: packageName,
        summary = stringResource(summaryRes, rule.keywords.joinToString(", ")),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
internal fun AppPickerDialog(apps: List<LauncherApp>, onPick: (LauncherApp) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleApps = apps.filter {
        query.isBlank() ||
            it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.keyword_choose_app)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_apps)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            label = app.label,
                            summary = app.packageName,
                            modifier = Modifier.clickable { onPick(app) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.keyword_cancel)) }
        },
    )
}

/**
 * Edits one app's keyword rule. Every change is saved right away through [onSave]; a rule without keywords
 * is not kept.
 */
@Composable
internal fun KeywordRuleDialog(
    appLabel: String,
    savedRule: KeywordRule?,
    onSave: (KeywordRule) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var rule by remember { mutableStateOf(savedRule ?: KeywordRule()) }

    fun update(new: KeywordRule) {
        rule = new
        onSave(new)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = appLabel) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ModeOption(
                    title = stringResource(R.string.keyword_mode_hide),
                    summary = stringResource(R.string.keyword_mode_hide_summary),
                    selected = rule.mode == KeywordRuleMode.HIDE_MATCHING,
                    onSelect = { update(rule.copy(mode = KeywordRuleMode.HIDE_MATCHING)) },
                )
                ModeOption(
                    title = stringResource(R.string.keyword_mode_show),
                    summary = stringResource(R.string.keyword_mode_show_summary),
                    selected = rule.mode == KeywordRuleMode.SHOW_MATCHING,
                    onSelect = { update(rule.copy(mode = KeywordRuleMode.SHOW_MATCHING)) },
                )
                KeywordInput(
                    label = stringResource(R.string.keyword_input),
                    onAdd = { update(rule.withKeyword(it)) },
                )
                if (rule.keywords.isEmpty()) {
                    Text(text = stringResource(R.string.keyword_empty), style = MaterialTheme.typography.bodySmall)
                }
                rule.keywords.forEach { keyword ->
                    KeywordItem(keyword = keyword, onRemove = { update(rule.withoutKeyword(keyword)) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.keyword_done)) }
        },
        dismissButton = if (rule.keywords.isEmpty()) {
            null
        } else {
            {
                TextButton(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                ) {
                    Text(text = stringResource(R.string.keyword_delete_rule))
                }
            }
        },
    )
}

/** A text field with an Add button. The field clears after each add. */
@Composable
internal fun KeywordInput(label: String, onAdd: (String) -> Unit, modifier: Modifier = Modifier) {
    var input by rememberSaveable { mutableStateOf("") }

    fun add() {
        onAdd(input)
        input = ""
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { add() }, enabled = input.isNotBlank()) {
            Text(text = stringResource(R.string.keyword_add))
        }
    }
}

@Composable
internal fun KeywordItem(keyword: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = keyword,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRemove) {
            Text(text = stringResource(R.string.keyword_remove))
        }
    }
}

@Composable
private fun AppRow(app: LauncherApp?, label: String, summary: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (app != null) {
            val icon = remember(app.packageName) { app.icon.toBitmap(ICON_SIZE_PX, ICON_SIZE_PX).asImageBitmap() }
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp))
        } else {
            Spacer(modifier = Modifier.size(40.dp))
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = summary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
