package com.andrea.openrates.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.andrea.openrates.BuildConfig
import com.andrea.openrates.R

const val ABOUT_SHEET_TAG = "about_sheet"

private const val SOURCE_URL = "https://github.com/AndreaScorza/openrates"
private const val FRANKFURTER_URL = "https://frankfurter.dev"
private const val APACHE = "Apache License 2.0"

/** A library the app ships, with the license it is distributed under. */
private class Notice(val name: String, val owner: String, val license: String, val source: String? = null)

/**
 * Everything in the release runtime classpath. All of it is Apache 2.0, whose
 * text is bundled below, except the Public Suffix List that OkHttp embeds.
 */
private val notices = listOf(
    Notice("AndroidX and Jetpack Compose", "Google", APACHE),
    Notice("Guava ListenableFuture", "Google", APACHE),
    Notice("Kotlin, kotlinx.coroutines, kotlinx.serialization", "JetBrains", APACHE),
    Notice("OkHttp and Okio", "Square", APACHE),
    Notice("JSpecify", "The JSpecify Authors", APACHE),
    Notice(
        "Public Suffix List, bundled in OkHttp",
        "Mozilla",
        "Mozilla Public License 2.0",
        source = "publicsuffix.org/list/public_suffix_list.dat",
    ),
)

/** Version, where the rates come from and what they are not, source and licenses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current
    var showLicense by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .testTag(ABOUT_SHEET_TAG)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text("OpenRates", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Version ${BuildConfig.VERSION_NAME} · no ads, no trackers, no accounts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                "OpenRates is free software under the GNU GPL v3: you can use, study, change " +
                    "and share it, as long as your version stays open too. The name and icon are " +
                    "not licensed.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                "Rates come from Frankfurter, which collects the reference rates published " +
                    "by the European Central Bank and other central banks. They are updated " +
                    "about once a day and are for information: banks and card providers " +
                    "usually charge a margin on top.",
                style = MaterialTheme.typography.bodyMedium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { uriHandler.openUri(SOURCE_URL) }) {
                    Text("Source code")
                }
                OutlinedButton(onClick = { uriHandler.openUri(FRANKFURTER_URL) }) {
                    Text("frankfurter.dev")
                }
            }

            HorizontalDivider()
            Text("Open-source licenses", style = MaterialTheme.typography.titleMedium)
            notices.forEach { notice ->
                Column {
                    Text(notice.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${notice.owner} · ${notice.license}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // MPL 2.0 asks that recipients can find the source form.
                    notice.source?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showLicense = !showLicense }) {
                    Text(if (showLicense) "Hide $APACHE" else APACHE)
                }
                OutlinedButton(onClick = { uriHandler.openUri("https://mozilla.org/MPL/2.0/") }) {
                    Text("Mozilla Public License 2.0")
                }
            }
            if (showLicense) {
                val context = LocalContext.current
                // The official text is hard-wrapped at 80 columns, which wraps raggedly
                // on a phone: rejoin each paragraph and let the screen wrap it.
                val text = remember {
                    context.resources.openRawResource(R.raw.apache_license_2_0)
                        .bufferedReader().use { it.readText() }
                        .split(Regex("""\n\s*\n"""))
                        .joinToString("\n\n") { para -> para.lines().joinToString(" ") { it.trim() }.trim() }
                }
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
