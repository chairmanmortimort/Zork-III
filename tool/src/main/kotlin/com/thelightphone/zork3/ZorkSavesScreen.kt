package com.thelightphone.zork3

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves browser — three numbered slots. Tapping a slot loads it (returns the
 * slot number to the host). Each occupied slot has a Delete action.
 *
 * Uses a plain Compose [Row] + [Modifier.clickable] for the back button and
 * delete action because the SDK's [LightTopBar]/[LightBarButton] route through
 * [com.thelightphone.sdk.ui.lightClickable], which no-ops when haptics are
 * disabled (the default on the LP3 emulator).
 */
class ZorkSavesScreen(
    sealedActivity: SealedLightActivity,
    private val slots: List<ZorkEngine.SlotInfo>,
    private val onDelete: (Int) -> Unit,
) : SimpleLightScreen<Int>(sealedActivity) {

    @Composable
    override fun Content() {
        val colors by LightThemeController.colors.collectAsState()
        LightTheme(colors = colors) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightThemeTokens.colors.background),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { goBack(0) },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        LightText(
                            text = "←",
                            variant = LightTextVariant.Paragraph,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                    LightText(
                        text = "Saves",
                        variant = LightTextVariant.Paragraph,
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentWidth(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.weight(1f))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 2f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
                ) {
                    Spacer(Modifier.height(1f.gridUnitsAsDp()))
                    slots.forEach { info ->
                        val dateStr = if (info.exists && info.lastModified > 0) {
                            try {
                                SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                    .format(Date(info.lastModified))
                            } catch (_: Exception) { "" }
                        } else ""
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightThemeTokens.colors.background)
                                .clickable { goBack(info.slot) }
                                .padding(vertical = 1f.gridUnitsAsDp()),
                        ) {
                            LightText(
                                text = "Slot ${info.slot}",
                                variant = LightTextVariant.Subheading,
                                align = TextAlign.Start,
                            )
                            LightText(
                                text = if (info.exists) "Saved · $dateStr" else "Empty",
                                variant = LightTextVariant.Detail,
                                align = TextAlign.Start,
                            )
                        }
                        if (info.exists) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LightThemeTokens.colors.background),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onDelete(info.slot); goBack(0) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    LightText(
                                        text = "Delete slot ${info.slot}",
                                        variant = LightTextVariant.Paragraph,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(1f.gridUnitsAsDp()))
                    }
                }
            }
        }
    }
}
