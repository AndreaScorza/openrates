package com.andrea.openrates

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag

/** `onAllNodesWithTag(...)` plus the fetch, so it can be polled from `waitUntil`. */
fun ComposeTestRule.onAllNodesWithTagSafe(tag: String) =
    onAllNodesWithTag(tag).fetchSemanticsNodesSafe()

private fun SemanticsNodeInteractionCollection.fetchSemanticsNodesSafe() =
    runCatching { fetchSemanticsNodes() }.getOrDefault(emptyList())

/** The rendered text of a node, joined across spans. */
fun SemanticsNodeInteraction.textValue(): String =
    fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)
        ?.joinToString("") { it.text }
        .orEmpty()
