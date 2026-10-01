package com.andrea.openrates.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stable identifiers the instrumented end-to-end tests drive the screen with. */
object Tags {
    const val AMOUNT = "amount_field"
    const val CLEAR_AMOUNT = "clear_amount_button"
    const val RESULT = "result_text"
    const val RATE = "rate_text"
    const val FROM = "from_button"
    const val TO = "to_button"
    const val SWAP = "swap_button"
    const val REFRESH = "refresh_button"
    const val STATUS = "status_text"
    const val WATCHLIST = "watchlist"
    const val ADD_WATCH = "add_watch_button"
    fun currency(code: String) = "currency_$code"
    fun watchRow(code: String) = "watch_$code"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    state: ConverterUiState,
    onAmountChange: (String) -> Unit,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onSwap: () -> Unit,
    onRefresh: () -> Unit,
    onToggleWatch: (String) -> Unit,
) {
    var picker by remember { mutableStateOf<PickerTarget?>(null) }

    // safeDrawing rather than the default system bars: it adds the keyboard, so the
    // screen stays scrollable above it, and the camera cutout when held sideways.
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("OpenRates") },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.isRefreshing,
                        modifier = Modifier
                            .testTag(Tags.REFRESH)
                            .semantics { contentDescription = "Refresh rates" },
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                        }
                    }
                },
            )
        },
    ) { padding ->
        // The whole width scrolls, but the content stops growing at a phone-like
        // width and centres: on a tablet or a phone held sideways the cards keep
        // their phone proportions instead of stretching edge to edge.
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Column(
            modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusLine(state)
            ConversionCard(
                state = state,
                onAmountChange = onAmountChange,
                onSwap = onSwap,
                onPickFrom = { picker = PickerTarget.From },
                onPickTo = { picker = PickerTarget.To },
            )
            WatchlistCard(
                state = state,
                onSelect = onToChange,
                onRemove = onToggleWatch,
                onAdd = { picker = PickerTarget.Watch },
            )
            Box(Modifier.padding(bottom = 24.dp))
        }
        }
    }

    picker?.let { target ->
        CurrencyPickerSheet(
            title = when (target) {
                PickerTarget.From -> "Convert from"
                PickerTarget.To -> "Convert to"
                PickerTarget.Watch -> "Currencies to watch"
            },
            codes = state.availableCurrencies,
            names = state.names,
            selected = if (target == PickerTarget.From) state.from else state.to,
            watchlist = state.watchlist,
            onToggleWatch = onToggleWatch,
            onPick = { code ->
                when (target) {
                    PickerTarget.From -> onFromChange(code)
                    PickerTarget.To -> onToChange(code)
                    // Watching is multi-select: keep the sheet open to add several.
                    PickerTarget.Watch -> if (code !in state.watchlist) onToggleWatch(code)
                }
                if (target != PickerTarget.Watch) picker = null
            },
            onDismiss = { picker = null },
        )
    }
}

private enum class PickerTarget { From, To, Watch }

/** Wide enough for a large phone's layout, narrow enough that a tablet doesn't stretch it. */
private val MaxContentWidth = 560.dp

/**
 * Below this card width (small phones, or "Display size: largest") the currency
 * pills slim down so the amount keeps room for its digits.
 */
private val NarrowCardWidth = 300.dp

/** "Updated 3 min ago · rates for 2026-09-02", or the offline explanation. */
@Composable
private fun StatusLine(state: ConverterUiState) {
    val snapshot = state.snapshot
    val text = when {
        state.statusMessage != null && snapshot == null -> state.statusMessage
        snapshot == null -> "Loading rates…"
        else -> buildString {
            append("Updated ").append(Format.relativeTime(snapshot.fetchedAtEpochMs))
            snapshot.dateFor(state.from, state.to)?.let { append(" · rates for ").append(it) }
            if (state.isOffline) append(" · offline")
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (state.isOffline) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = "Offline",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(Tags.STATUS),
        )
    }
}

@Composable
private fun ConversionCard(
    state: ConverterUiState,
    onAmountChange: (String) -> Unit,
    onSwap: () -> Unit,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
) {
    Card(colors = CardDefaults.elevatedCardColors()) {
        BoxWithConstraints(Modifier.padding(20.dp)) {
        val narrow = maxWidth < NarrowCardWidth
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = onAmountChange,
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = if (narrow) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    // Clearing also clears what is remembered for the next launch:
                    // onAmountChange persists every edit, blank included.
                    trailingIcon = {
                        if (state.amountInput.isNotEmpty()) {
                            IconButton(
                                onClick = { onAmountChange("") },
                                modifier = Modifier
                                    .testTag(Tags.CLEAR_AMOUNT)
                                    .semantics { contentDescription = "Clear amount" },
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag(Tags.AMOUNT),
                )
                CurrencyChip(
                    code = state.from,
                    name = state.names[state.from],
                    onClick = onPickFrom,
                    tag = Tags.FROM,
                    narrow = narrow,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(Modifier.weight(1f))
                IconButton(
                    onClick = onSwap,
                    modifier = Modifier
                        .testTag(Tags.SWAP)
                        .semantics { contentDescription = "Swap currencies" },
                ) {
                    Icon(Icons.Filled.SwapVert, contentDescription = null)
                }
                HorizontalDivider(Modifier.weight(1f))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = state.converted?.let { Format.amount(it) }
                        ?: if (state.hasData) "—" else "…",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Start,
                    // A big amount into a currency like IDR shrinks to fit one line
                    // instead of breaking the number in two.
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 14.sp,
                        maxFontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    ),
                    modifier = Modifier.weight(1f).testTag(Tags.RESULT),
                )
                CurrencyChip(
                    code = state.to,
                    name = state.names[state.to],
                    onClick = onPickTo,
                    tag = Tags.TO,
                    narrow = narrow,
                )
            }

            state.rate?.let { rate ->
                Text(
                    text = "1 ${state.from} = ${Format.rate(rate)} ${state.to}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(Tags.RATE),
                )
            }
        }
        }
    }
}

@Composable
private fun CurrencyChip(
    code: String,
    name: String?,
    onClick: () -> Unit,
    tag: String,
    narrow: Boolean,
) {
    // Narrow pills trim the padding by the same 24dp as the width, so the code
    // itself keeps the same room and still fits at large font sizes.
    FilledTonalButton(
        onClick = onClick,
        contentPadding = if (narrow) PaddingValues(horizontal = 12.dp) else ButtonDefaults.ContentPadding,
        modifier = Modifier
            .width(if (narrow) 88.dp else 112.dp)
            .testTag(tag)
            .semantics { contentDescription = name?.let { "$code, $it" } ?: code },
    ) {
        Text(code, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * The multi-quote view: one `/v2/rates?base=&quotes=` call keeps several
 * currencies visible at once, which is the common "how much is that in..." case.
 */
@Composable
private fun WatchlistCard(
    state: ConverterUiState,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    // Every watched currency stays listed, the amount's own included, so swapping
    // never makes a row appear or disappear.
    val entries = state.watchlist
    Card(modifier = Modifier.testTag(Tags.WATCHLIST)) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${state.amountInput.ifBlank { "0" }} ${state.from} in",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                // A tonal container rather than bare text: with no accent colour in the
                // palette, shape is the only thing left to say "this is tappable".
                FilledTonalButton(
                    onClick = onAdd,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.testTag(Tags.ADD_WATCH),
                ) {
                    Text("Add", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (entries.isEmpty()) {
                Text(
                    text = "Add the currencies you check often.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            entries.forEach { code ->
                val value = state.rateFor(code)?.let { r -> state.amount?.times(r) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(Tags.watchRow(code))
                        .clickable { onSelect(code) }
                        .padding(start = 20.dp, end = 8.dp)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(code, style = MaterialTheme.typography.titleMedium)
                        state.names[code]?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = value?.let { Format.amount(it) } ?: "\u2014",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    IconButton(
                        onClick = { onRemove(code) },
                        modifier = Modifier.semantics { contentDescription = "Remove $code" },
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
